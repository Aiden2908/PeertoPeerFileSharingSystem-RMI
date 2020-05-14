package p2p_server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/* A class that implements RMI*/
public class ClientRMI extends UnicastRemoteObject implements I_ClientRMI {
	private Client theClient;
	
	public String sharedDirectory;
	public ArrayList<String> localFiles = new ArrayList<String>();
	public ArrayList<String> readMessageIDs;
	int clientID;
	int currentClientPort;

	public ClientRMI(Client theClient, String sharedDir, int peerId, int currentPeerPort, ArrayList<String> localFiles) throws RemoteException {
		super();
		this.theClient = theClient;
		this.sharedDirectory = sharedDir;
		this.clientID = peerId;
		this.localFiles = localFiles;
		this.currentClientPort = currentPeerPort;
		readMessageIDs = new ArrayList<String>();
	}

	public synchronized byte[] obtain(String filename) throws RemoteException {
		byte[] fileBytes = null;
		String fullFileName = sharedDirectory + "/" + filename;
		try {
			File myFile = new File(fullFileName);
			fileBytes = new byte[(int) myFile.length()];
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(fullFileName));
			input.read(fileBytes, 0, fileBytes.length);
			input.close();
			return fileBytes;
		} catch (Exception e) {

		}
		return fileBytes;

	}

	// Remote method for handling the search request
	public HitQuery query(int fromPeerId, String msgId, String filename, HashMap<Integer, Integer> timestampMap) throws RemoteException {
		ArrayList<NeighbourClient> neighborPeers = theClient.getNeighbourList(); // 
		ArrayList<ClientDetails> findAt = new ArrayList<ClientDetails>();
		ArrayList<String> pathTrace = new ArrayList<String>();
		ArrayList<NeighborConnection> peerThreadsList = new ArrayList<NeighborConnection>();
		HitQuery hitqueryResult = new HitQuery();
		Boolean bDuplicate = false;
		
		theClient.setVectorTimestampMap(theClient.getClientID(), theClient.getVectorTimestampMap().get(theClient.getClientID()) + 1, false);
		
		theClient.handleNewTimestampValues(timestampMap);
		
		synchronized (this) {	//prevent searching with the same client again
			if (this.readMessageIDs.contains(msgId)) {
				//System.out.println("Incoming Request to peer " + clientID + ": From - " + fromPeerId + " Duplicate Request - Already searched in this peer- with message id - " + String.valueOf(msgId));
				
				System.out.println("FROM: Client " + fromPeerId + " TO: " + clientID + " Duplicate Request - Already searched in this peer- with message id - " + String.valueOf(msgId) + "\n");
				bDuplicate = true;
			} else {
				this.readMessageIDs.add(msgId);
			}
		}
		
		if (bDuplicate == false) {
			System.out.println();
			
			//System.out.println("Incoming Request to peer " + clientID + " from " + fromPeerId + ". Searching locally and sending request to neighbours for msg id: " + String.valueOf(msgId));
			
			System.out.println("FROM: Client " + fromPeerId + ". TO: " + clientID);
			
			List<Thread> threads = new ArrayList<Thread>();

			if (searchInCurrentPeer(localFiles, filename) == true) {
				System.out.println("Local Search: File Found in the current peer");
				ClientDetails temp = new ClientDetails();
				temp.ipAddress = "localhost";
				temp.clientID = clientID;
				temp.portNum = currentClientPort;
				findAt.add(temp);
			} else {
				System.out.println("Local Search: File not found in the current peer");
			}

			if (neighborPeers.size() == 0) {
				pathTrace.add(Integer.toString(clientID));
			}

			for (int i = 0; i < neighborPeers.size(); i++) {
				String currentPeer = "peerid." + fromPeerId;
				System.out.println(neighborPeers.get(i));
				if (neighborPeers.get(i).clientID.equals(currentPeer)) {
					// avoid sending request back to the sender
					continue;
				}
				
				//System.out.println("Outgoing Request from peer " + clientID + ": Sending request to " + neighborPeers.get(i).clientID + " " + neighborPeers.get(i).portNum);
				
				System.out.println("FROM: Client " + clientID + ". TO: " + neighborPeers.get(i).clientID);
				
				NeighborConnection ths = new NeighborConnection(neighborPeers.get(i).ipAddress, neighborPeers.get(i).portNum, filename, msgId, clientID, neighborPeers.get(i).clientID, theClient.getVectorTimestampMap());
				
				// Starts thread for the request
				Thread ts = new Thread(ths);
				ts.start();
				// store the instances to get the return values after all the threads finish the execution
				threads.add(ts);
				peerThreadsList.add(ths);

			}
			
			for (int i = 0; i < threads.size(); i++) {
				try {
					// wait for all the request threads finish the search
					((Thread) threads.get(i)).join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			for (int i = 0; i < threads.size(); i++) {
				try {
					((Thread) threads.get(i)).join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			for (int i = 0; i < peerThreadsList.size(); i++) {
				// ArrayList<PeerDetails> threadResult = new ArrayList<PeerDetails>();
				HitQuery temp = new HitQuery();
				//
				// Get the result of the thread request
				temp = (HitQuery) peerThreadsList.get(i).getValue();
				if (temp.foundClients.size() > 0) {
					// System.out.println("return value of thread "+i+" is
					// "+threadResult.toArray());
					findAt.addAll(temp.foundClients);
				}
				for (int count = 0; count < temp.paths.size(); count++) {
					String path = clientID + temp.paths.get(count);
					pathTrace.add(path);
				}
			}
			
			if (pathTrace.size() == 0) {
				pathTrace.add(Integer.toString(clientID));
			}
			
			// send the result back to the sender
			System.out.println("HitQuery: Send following result back to " + fromPeerId);
			for (int i = 0; i < findAt.size(); i++) {
				System.out.println("--Found at Peer" + findAt.get(i).clientID + " on localhost:" + findAt.get(i).portNum);

			}
			
			hitqueryResult.foundClients.addAll(findAt);
			hitqueryResult.paths.addAll(pathTrace);

		}

		this.readMessageIDs.clear();
		return hitqueryResult;
	}

	private Boolean searchInCurrentPeer(ArrayList<String> localFiles2, String filename) {
		// function to search the files in local peer
		int index = localFiles.indexOf(filename);
		
		if (index == -1) {
			return false;
		} else {
			return true;
		}
	}

	public void changeConnectionConnect(NeighbourClient nc, String searchID, HashMap<Integer, Integer> timestampMap) { // Method to edit the ring after connection	
		System.out.println("Search id: " + searchID);
		
		theClient.setVectorTimestampMap(theClient.getClientID(), theClient.getVectorTimestampMap().get(theClient.getClientID()) + 1, false);
		
		theClient.handleNewTimestampValues(timestampMap);
		
		ArrayList<NeighbourClient> neighborPeers = theClient.getNeighbourList();
		
		if(neighborPeers.get(0).clientID.equals(searchID)) { // Change neigbhour
			theClient.setNeighbourList(nc);
			
		} else { // Pass message on to neighbour
			NeighborConnection connectionThread = new NeighborConnection(nc, neighborPeers.get(0).ipAddress, neighborPeers.get(0).portNum, "connect", searchID, true, theClient.getVectorTimestampMap());
			Thread threadInstance = new Thread(connectionThread);
			threadInstance.start();
		}
	}
	
	public void changeConnectionDisconnect(NeighbourClient nc, String searchID, boolean isLeader, HashMap<Integer, Integer> timestampMap) {
		ArrayList<NeighbourClient> neighborPeers = theClient.getNeighbourList();

		theClient.setVectorTimestampMap(theClient.getClientID(), theClient.getVectorTimestampMap().get(theClient.getClientID()) + 1, false);

		theClient.setVectorTimestampMap(Integer.parseInt(Character.toString(searchID.charAt(searchID.length()-1))), 0, true);
		
		timestampMap.remove(Integer.parseInt(Character.toString(searchID.charAt(searchID.length()-1))));
		
		theClient.handleNewTimestampValues(timestampMap);
		
		System.out.println("My neighbours ID: " + neighborPeers.get(0).clientID + ", Search ID: " + searchID);
		
		if(neighborPeers.get(0).clientID.equals(searchID)) { // Change neigbhour
			theClient.setNeighbourList(nc);
			if(isLeader) {
				theClient.changRobertsStartElection();
			}
			
		} else { // Pass message on to neighbour
			NeighborConnection connectionThread = new NeighborConnection(nc, neighborPeers.get(0).ipAddress, neighborPeers.get(0).portNum, "disconnect", searchID, isLeader, theClient.getVectorTimestampMap());
			Thread threadInstance = new Thread(connectionThread);
			threadInstance.start();
		}
	}

	@Override
	public void removeReadMessageIDs() throws RemoteException {
		this.readMessageIDs.clear();
	}

    @Override
    public void updateLocalFiles(ArrayList<String> localFiles) throws RemoteException {
        this.localFiles=localFiles;
    }
    
	
	public void changRobertsRecieveMessage(String message, int sendingClientID, HashMap<Integer, Integer> timestampMap) {
		ArrayList<NeighbourClient> neighborPeers = theClient.getNeighbourList();
		theClient.setLeader(false);

		theClient.setVectorTimestampMap(theClient.getClientID(), theClient.getVectorTimestampMap().get(theClient.getClientID()) + 1, false);
		
		theClient.handleNewTimestampValues(timestampMap);
		
		if(message.equals("election")) {
			if(sendingClientID > clientID) {
				theClient.setParticipated(true);
				NeighborConnection connectionThread = new NeighborConnection(neighborPeers.get(0), neighborPeers.get(0).ipAddress, neighborPeers.get(0).portNum, "election", sendingClientID, true, theClient.getVectorTimestampMap());
				Thread threadInstance = new Thread(connectionThread);
				threadInstance.start();
				
			} else if(sendingClientID == clientID) {
				NeighborConnection connectionThread = new NeighborConnection(neighborPeers.get(0), neighborPeers.get(0).ipAddress, neighborPeers.get(0).portNum, "leader", clientID, true, theClient.getVectorTimestampMap());
				Thread threadInstance = new Thread(connectionThread);
				threadInstance.start();
				
			} else if(sendingClientID < clientID) {
				if(!theClient.getParticipated()) {
					theClient.changRobertsStartElection();
				}
			}
		} else if(message.equals("leader")) {
			theClient.setParticipated(false);
			if(sendingClientID != clientID) {
				NeighborConnection connectionThread = new NeighborConnection(neighborPeers.get(0), neighborPeers.get(0).ipAddress, neighborPeers.get(0).portNum, "leader", sendingClientID, true, theClient.getVectorTimestampMap());
				Thread threadInstance = new Thread(connectionThread);
				threadInstance.start();
				
			} else {
				System.out.println("New leader is: " + clientID);
				theClient.setLeader(true);
			}
		} else {
			System.out.println("Something went wrong.");
		}
	}
	
	public void printNeighbour(int sendingClientID, HashMap<Integer, Integer> timestampMap) {
		ArrayList<NeighbourClient> neighborPeers = theClient.getNeighbourList();
		
		theClient.setVectorTimestampMap(theClient.getClientID(), theClient.getVectorTimestampMap().get(theClient.getClientID()) + 1, false);
		
		theClient.handleNewTimestampValues(timestampMap);

		if(clientID != sendingClientID) {
			System.out.println("Snapshot feature| My id: " + clientID + ", My neighbours id: " + neighborPeers.get(0).clientID);
			System.out.println(theClient.mapToString());
			
			NeighborConnection connectionThread = new NeighborConnection(neighborPeers.get(0), neighborPeers.get(0).ipAddress, neighborPeers.get(0).portNum, "snapshot", sendingClientID, true, theClient.getVectorTimestampMap());
			Thread threadInstance = new Thread(connectionThread);
			threadInstance.start();
		}
	}
}

class NeighbourClient implements Serializable {
	private static final long serialVersionUID = 1L;
	public String clientID;
	public String ipAddress;
	public int portNum;
	public int intID;
}