package p2p_server;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.io.InputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.NotBoundException;
import java.io.BufferedOutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.File;
import javax.swing.DefaultListModel;

/* A class to act as a server as well as a client*/
public class Client implements Runnable {
	private int clientID;
	private boolean isLeader;
	private boolean electionParticipant;
	private boolean isRunning;
	private String shareDir = null;
	private I_ClientRMI clientInterface;
	private ArrayList<ClientDetails> searchResults;
	private ArrayList<NeighbourClient> clientNeighbours = new ArrayList<NeighbourClient>();
	private HashMap<Integer, Integer> vectorTimestampMap = new HashMap<>();
	public Registry rmiRegistry; 
	
	// ==Method to handle initializing client.==//
	public void clientInit(int portNum, int clientID, String sharedDir, boolean afterStartup, int connectClient) throws RemoteException {
		ArrayList<String> localClientFiles = new ArrayList<String>();

		vectorTimestampMap.put(clientID, 0);
		
		isRunning = true;
		
		this.clientID = clientID;
		this.shareDir = sharedDir;

		getLocalClientFiles(sharedDir, localClientFiles);

		startClientSubServer(clientID, portNum, sharedDir, localClientFiles);
		
		System.out.println("Client: " + clientID + " is online.");
		
		try {
			getClientsNeighbours(clientNeighbours, clientID, afterStartup, connectClient);
		} catch (IOException e) {
			e.printStackTrace();
		}
		changRobertsStartElection();
	}

	// ==Method to handle searching files on connected clients.==//
	public String searchFile(String searchFileName, DefaultListModel peerFileModel) throws InterruptedException, IOException {
		List<Thread> threadInstancesList = new ArrayList<Thread>();
		ArrayList<NeighbourClient> neighborClients = new ArrayList<NeighbourClient>();
		ArrayList<ClientDetails> searchResults = new ArrayList<ClientDetails>();
		ArrayList<NeighborConnection> neighborThreads = new ArrayList<NeighborConnection>();
		ArrayList<String> localClientFiles = new ArrayList<String>();

		getLocalClientFiles(this.shareDir, localClientFiles);
		clientInterface.updateLocalFiles(localClientFiles);
		int searchCounter = 0;

		peerFileModel.removeAllElements();
		neighborClients.clear();
		threadInstancesList.clear();
		neighborThreads.clear();
		searchResults.clear();

		System.out.println("Enter file name to search: " + searchFileName);

		searchCounter++;
		
		String messageID = "Peer1.Search" + searchCounter;

		for(int i = 0; i < clientNeighbours.size(); i++) {
			System.out.println("Sending request to " + clientNeighbours.get(i).clientID + " (" + clientNeighbours.get(i).ipAddress + ":" + clientNeighbours.get(i).portNum + ")");
			NeighborConnection connectionThread = new NeighborConnection(clientNeighbours.get(i).ipAddress, clientNeighbours.get(i).portNum, searchFileName, messageID, clientID, clientNeighbours.get(i).clientID, vectorTimestampMap);
			Thread threadInstance = new Thread(connectionThread);
			threadInstance.start();
			threadInstancesList.add(threadInstance);
			neighborThreads.add(connectionThread);
		}

		for (int i = 0; i < threadInstancesList.size(); i++) {
			((Thread) threadInstancesList.get(i)).join();
		}
		
		for (int i = 0; i < neighborThreads.size(); i++) {
			HitQuery hitQueryResult = (HitQuery) neighborThreads.get(i).getValue();
			if (hitQueryResult.foundClients.size() > 0) {
				searchResults.addAll(hitQueryResult.foundClients);
			}
			
			for (int count = 0; count < hitQueryResult.paths.size(); count++) {
				String path = clientID + hitQueryResult.paths.get(count);
				System.out.println("Number of Paths searched: " + path);
			}
		}
		
		for (int i = 0; i < searchResults.size(); i++) {
			peerFileModel.addElement("Client: " + searchResults.get(i).clientID + " , running on 127.0.0.1:" + searchResults.get(i).portNum);
		}
		
		this.searchResults = searchResults;

		if (searchResults.size() == 0) {
			System.out.println(searchFileName + " No file(s) found in the network.");
			return searchFileName + " No file(s) found in the network.";
		} else {
			System.out.println(searchFileName + " Found file in client(s): ");
			return "";
		}
	}

	// ==Wrapper method to download file.==//
	public void downloadFile(int clientIDToDownloadFrom, String fileName) throws IOException, NotBoundException {
		if (this.searchResults.size() > 0) {
			download(this.searchResults, clientIDToDownloadFrom, fileName, this.shareDir);
		}
	}

	// ==Method to grab files in client's shared directory.==//
	public void getLocalClientFiles(String sharedDir, ArrayList<String> clientLocalFiles) {
		vectorTimestampMap.replace(clientID, vectorTimestampMap.get(clientID) + 1);
		
		File file = new File(sharedDir);
		File newFind;
		String fileName;
		
		if(!file.exists()) file.mkdir();
		
		String[] filesList = file.list();	//list of files within that directory
		
		for (int i = 0; i < filesList.length; i++) {
			newFind = new File(filesList[i]);
			fileName = newFind.getName();
			clientLocalFiles.add(fileName);
		}

	}

	// ==Method to handle starting client rmi sub server.==//
	public void startClientSubServer(int clientID, int portNum, String sharedDir, ArrayList<String> clientLocalFiles) {
		vectorTimestampMap.replace(clientID, vectorTimestampMap.get(clientID) + 1);
		try {
			rmiRegistry = LocateRegistry.createRegistry(portNum);
			clientInterface = new ClientRMI(this, sharedDir, clientID, portNum, clientLocalFiles);
			Naming.rebind("rmi://localhost:" + portNum + "/subServer", clientInterface);
			System.out.println("Client " + clientID + "sub-server running on localhost:" + portNum);
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
	}

	// ==Method to get client's connected neighbours.==//
	public void getClientsNeighbours(ArrayList<NeighbourClient> neighbourClients, int clientID, boolean afterStartup, int connectClient) throws IOException {
		vectorTimestampMap.replace(clientID, vectorTimestampMap.get(clientID) + 1);
		
		Properties properties = new Properties();
		String property = null;
		InputStream inputStream = null;

		if(!afterStartup) { // checking to see if it should load config.properties or not
			inputStream = new FileInputStream("C:\\Users\\rober\\Documents\\GitHub\\Java-File-Sharing-rmi--v1\\P2P-FS-v01\\src\\p2p_server\\config.properties");
			properties.load(inputStream);
			property = "peerid." + clientID + ".neighbors";
			
			String[] str = properties.getProperty(property).split(",");
		
			for (int i = 0; i < str.length; i++) {
				NeighbourClient neighbourClient = new NeighbourClient();
				neighbourClient.clientID = str[i];
				neighbourClient.ipAddress = properties.getProperty(str[i] + ".ip");
				neighbourClient.portNum = Integer.parseInt(properties.getProperty(str[i] + ".port"));
				
				System.out.println("String: " + neighbourClient.clientID + " | " + neighbourClient.ipAddress + " | " + neighbourClient.portNum);
				
				neighbourClients.add(neighbourClient);
			}

			inputStream.close();
		} else {
			property = "peerid." + clientID + ".neighbors";
			
			NeighbourClient nc = new NeighbourClient();
			nc.clientID = "peerid." + connectClient;
			nc.intID = connectClient;
			nc.ipAddress = "127.0.0.1";
			nc.portNum = 8000 + connectClient;
			
			neighbourClients.add(nc);
			
			NeighbourClient newClient = new NeighbourClient();
			newClient.clientID = "peerid." + clientID;
			newClient.intID = clientID;
			newClient.ipAddress = "127.0.0.1";
			newClient.portNum = 8000 + clientID;

			// Send message around the ring to 1s connection saying to now point to new client
			
			System.out.println("Sending message to " + clientNeighbours.get(0).clientID + " (" + clientNeighbours.get(0).ipAddress + ":" + clientNeighbours.get(0).portNum + ")");
			
			NeighborConnection connectionThread = new NeighborConnection(newClient, clientNeighbours.get(0).ipAddress, clientNeighbours.get(0).portNum, "connect", "peerid." + connectClient, true, vectorTimestampMap);
			Thread threadInstance = new Thread(connectionThread);
			threadInstance.start();
		}
	}

	// ==Method to handle downloading files from available clients.==//
	public void download(ArrayList<ClientDetails> searchResults, int clientID, String fileName, String downloadDir) throws IOException, NotBoundException {
		vectorTimestampMap.replace(clientID, vectorTimestampMap.get(clientID) + 1);
		
		int numClients;
		int count = 0;
		int portNum = 0;
		String downloadIP = null;
		numClients = searchResults.size();

		while (count < numClients) {
			if (clientID == searchResults.get(count).clientID) {
				portNum = searchResults.get(count).portNum;
				downloadIP = searchResults.get(count).ipAddress;
				break;
			}
			count++;
		}

		System.out.println("Downloading from " + downloadIP + ":" + portNum);

		I_ClientRMI subServer = null;
		subServer = (I_ClientRMI) Naming.lookup("rmi://localhost:" + portNum + "/subServer");

		byte[] bytes = null;
		bytes = subServer.obtain(fileName);

		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(downloadDir + "//" + fileName));
		bufferedOutputStream.write(bytes, 0, bytes.length);

		bufferedOutputStream.flush();
		bufferedOutputStream.close();
		System.out.println("\"" + fileName + "\" downloaded to path: " + downloadDir);

	}

	public ArrayList<NeighbourClient> getNeighbourList() {
		return clientNeighbours;
	}
	
	// Method which sends a message to neighbour in the ring to handing disconnecting of a client
	public void disconnectClient() {
		NeighborConnection connectionThread = new NeighborConnection(clientNeighbours.get(0), clientNeighbours.get(0).ipAddress ,clientNeighbours.get(0).portNum, "disconnect", "peerid." + Integer.toString(clientID), isLeader, vectorTimestampMap);
		
		Thread threadInstance = new Thread(connectionThread);
		threadInstance.start();
	}
	
	// Method which updates the neighbour of a client when a new client joins the system
	public void setNeighbourList(NeighbourClient newNeighbour) {
		vectorTimestampMap.replace(clientID, vectorTimestampMap.get(clientID) + 1);
		
		System.out.println("Changing client: " + clientID + "s neighbour.");
		System.out.println("Old client ID: " + clientNeighbours.get(0).clientID);
		
		clientNeighbours.clear();
		clientNeighbours.add(newNeighbour);
		
		System.out.println("New client ID: " + clientNeighbours.get(0).clientID);
	}
	
	// Method which sends a message to neighbour when a leader election is occuring
	// Gets called when a new client joins the ring and when a client leaves the ring
	public void changRobertsStartElection() {
		vectorTimestampMap.replace(clientID, vectorTimestampMap.get(clientID) + 1);
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		electionParticipant = true;
		
		NeighborConnection connectionThread = new NeighborConnection(clientNeighbours.get(0), clientNeighbours.get(0).ipAddress, clientNeighbours.get(0).portNum, "election", clientID, true, vectorTimestampMap);
		Thread threadInstance = new Thread(connectionThread);
		threadInstance.start();
	}
	
	public boolean getLeader() {
		return isLeader;
	}	
	
	public void setLeader(boolean leader) {
		isLeader = leader;
	}

	public boolean getParticipated() {
		return electionParticipant;
	}
	
	public void setParticipated(boolean participated) {
		electionParticipant = participated;
	}

	public int getClientID() {
		return clientID;
	}
	
	public void setIsRunning() {
		isRunning = false;
	}
	
	public HashMap<Integer, Integer> getVectorTimestampMap() {
		return vectorTimestampMap;
	}
	
	// Handles updating the timestamp when an event occurs for the client
	// also handles removing from the map when a client leaves the system
	public void setVectorTimestampMap(int clientID, int newValue, boolean toDelete) {
		if(toDelete) {
			vectorTimestampMap.remove(clientID);
			return;
		}
		
		if(vectorTimestampMap.containsKey(clientID)) {
			vectorTimestampMap.replace(clientID, newValue);
		} else {
			vectorTimestampMap.put(clientID, newValue);
		}
	}
	
	// alg which updates the map checking if the value currently stored for a particular is less than the actual value
	public void handleNewTimestampValues(HashMap<Integer, Integer> incomingMap) {
		Iterator<?> it = incomingMap.entrySet().iterator();
		
		while(it.hasNext()) {
			Map.Entry incomingPair = (Map.Entry)it.next();

			int incomingKey = (Integer)incomingPair.getKey();
			int incomingValue = incomingMap.get(incomingPair.getKey());
			
			if(vectorTimestampMap.containsKey(incomingKey)) {
				int myValue = vectorTimestampMap.get((int)incomingPair.getKey());
				if(incomingValue > myValue) {
					vectorTimestampMap.replace(incomingKey, incomingValue);
				}
			} else {
				vectorTimestampMap.put(incomingKey, incomingValue);
			}
		}
	}
	
	// Prints out the timestamp map - used for the snapshot feature
	public String mapToString() {
		String returnString = "Client ID Map: ";
		
		Iterator<?> it = vectorTimestampMap.entrySet().iterator();
		
		while(it.hasNext()) {
			Map.Entry incomingPair = (Map.Entry)it.next();
			
			returnString += "(" + incomingPair.getKey() + ", " + incomingPair.getValue() + ").";
		}
		return returnString;
	}
	
	@Override
	public void run() {
		while(isRunning) {
			try {
				Thread.sleep(15000);

				if(isLeader) {
					System.out.println("\nSnapshot feature| My id: " + clientID + ", My neighbours id: " + clientNeighbours.get(0).clientID);
					System.out.println(mapToString());
					
					NeighborConnection connectionThread = new NeighborConnection(clientNeighbours.get(0), clientNeighbours.get(0).ipAddress, clientNeighbours.get(0).portNum, "snapshot", clientID, true, vectorTimestampMap);
					Thread threadInstance = new Thread(connectionThread);
					threadInstance.start();
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
