package p2p_server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.util.Properties;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.NotBoundException;
import java.io.BufferedOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import javax.swing.DefaultListModel;

/* A class to act as a server as well as a client*/
public class Client {
	private String shareDir = null;
	private int clientID, portNum;
	private ArrayList<ClientDetails> searchResults;
	private I_ClientRMI clientInterface;

	// ==Method to handle initializing client.==//
	public void clientInit(int portNum, int clientID, String sharedDir) throws RemoteException {
		ArrayList<String> localClientFiles = new ArrayList<String>();
		List<Thread> threadInstancesList = new ArrayList<Thread>();

		this.portNum = portNum;
		this.clientID = clientID;
		this.shareDir = sharedDir;

		getLocalClientFiles(sharedDir, localClientFiles);

		startClientSubServer(clientID, portNum, sharedDir, localClientFiles);
		System.out.println("Client: " + clientID + " is online.");
		return;
	}

	// ==Method to handle searching files on connected clients.==//
	public String searchFile(String searchFileName, DefaultListModel peerFileModel)
			throws InterruptedException, IOException {
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

		getClientsNeighbours(neighborClients, clientID);
		++searchCounter;
		String messageID = "Peer1.Search" + searchCounter;

		for (int i = 0; i < neighborClients.size(); i++) {
			System.out.println(
					"Sending request to " + neighborClients.get(i).clientID + " " + neighborClients.get(i).portNum);
			NeighborConnection connectionThread = new NeighborConnection(neighborClients.get(i).ipAddress,
					neighborClients.get(i).portNum, searchFileName, messageID, clientID,
					neighborClients.get(i).clientID);
			Thread threadInstance = new Thread(connectionThread);
			threadInstance.start();
			threadInstancesList.add(threadInstance);
			neighborThreads.add(connectionThread);
		}
		for (int i = 0; i < threadInstancesList.size(); i++)
			((Thread) threadInstancesList.get(i)).join();

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
			peerFileModel.addElement("Client: " + searchResults.get(i).clientID + " , running on 127.0.0.1:"
					+ searchResults.get(i).portNum);
		}
		this.searchResults = searchResults;

		if (searchResults.size() == 0) {
			System.out.println(searchFileName + " No file(s) found in the netwrok.");
			return searchFileName + " No file(s) found in the netwrok.";
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
		File file = new File(sharedDir);
		File newFind;
		String fileName;
		String[] filesList = file.list();
		for (int i = 0; i < filesList.length; i++) {
			newFind = new File(filesList[i]);
			fileName = newFind.getName();
			clientLocalFiles.add(fileName);
		}

	}

	// ==Method to handle starting client rmi sub server.==//
	public void startClientSubServer(int clientID, int portNum, String sharedDir, ArrayList<String> clientLocalFiles) {
		try {
			LocateRegistry.createRegistry(portNum);
			clientInterface = new ClientRMI(sharedDir, clientID, portNum, clientLocalFiles);
			Naming.rebind("rmi://localhost:" + portNum + "/subServer", clientInterface);
			System.out.println("Client " + clientID + "sub-server running on localhost:" + portNum);
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
	}

	// ==Method to get client's connected neighbours.==//
	public void getClientsNeighbours(ArrayList<NeighbourClient> neighbourClients, int clientID) throws IOException {
		Properties properties = new Properties();
		String property;
		InputStream inputStream = null;

		inputStream = new FileInputStream("C:\\Users\\Aiden\\Documents\\NetBeansProjects\\Peer to Peer File Sharing System (RMI)\\src\\p2p_server\\config.properties");
		properties.load(inputStream);
		property = "peerid." + clientID + ".neighbors";

		String[] str = properties.getProperty(property).split(",");
		for (int i = 0; i < str.length; i++) {
			NeighbourClient neighbourClient = new NeighbourClient();
			neighbourClient.clientID = str[i];
			neighbourClient.ipAddress = properties.getProperty(str[i] + ".ip");
			neighbourClient.portNum = Integer.parseInt(properties.getProperty(str[i] + ".port"));
			neighbourClients.add(neighbourClient);
		}

		inputStream.close();
	}

	// ==Method to handle downloading files from available clients.==//
	public void download(ArrayList<ClientDetails> searchResults, int clientID, String fileName, String downloadDir)
			throws IOException, NotBoundException {
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

		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
				new FileOutputStream(downloadDir + "//" + fileName));
		bufferedOutputStream.write(bytes, 0, bytes.length);

		bufferedOutputStream.flush();
		bufferedOutputStream.close();
		System.out.println("\"" + fileName + "\" downloaded to path: " + downloadDir);

	}

}
