package p2p_server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/* A class that implements RMI*/
public class ClientRMI extends UnicastRemoteObject implements I_ClientRMI {
	private static final long serialVersionUID = 1L;
	public String sharedDirectory;
	public ArrayList<String> localFiles = new ArrayList<String>();
	public ArrayList<String> readMessageIDs;
	int clientID;
	int currentClientPort;

	ClientRMI(String sharedDir, int peerId, int currentPeerPort, ArrayList<String> localFiles) throws RemoteException {
		super();
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
	public HitQuery query(int fromPeerId, String msgId, String filename) throws RemoteException {
		ArrayList<NeighbourClient> neighborPeers = new ArrayList<NeighbourClient>();
		ArrayList<ClientDetails> findAt = new ArrayList<ClientDetails>();
		ArrayList<String> pathTrace = new ArrayList<String>();
		HitQuery hitqueryResult = new HitQuery();
		Boolean bDuplicate = false;
		ArrayList<NeighborConnection> peerThreadsList = new ArrayList<NeighborConnection>();
		synchronized (this) {
			if (this.readMessageIDs.contains(msgId)) {
				System.out.println("Incoming Request to peer " + clientID + ": From - " + fromPeerId
						+ " Duplicate Request - Already searched in this peer- with message id - "
						+ String.valueOf(msgId));
				bDuplicate = true;
			} else {
				this.readMessageIDs.add(msgId);
			}
		}
		if (bDuplicate == false) {
			System.out.println("Incoming Request to peer " + clientID + ": From - " + fromPeerId
					+ " Search locally and send request to neighbours for msg id- " + String.valueOf(msgId));

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
			getNeighborPeers(neighborPeers, clientID);
			if (neighborPeers.size() == 0) {
				pathTrace.add(Integer.toString(clientID));
			}

			for (int i = 0; i < neighborPeers.size(); i++) {
				String currentPeer = "peerid." + fromPeerId;
				if (neighborPeers.get(i).clientID.equals(currentPeer)) {
					// avoid sending request back to the sender
					continue;
				}
				System.out.println("Outgoing Request from peer " + clientID + ": Sending request to "
						+ neighborPeers.get(i).clientID + " " + neighborPeers.get(i).portNum);
				NeighborConnection ths = new NeighborConnection(neighborPeers.get(i).ipAddress,
						neighborPeers.get(i).portNum, filename, msgId, clientID, neighborPeers.get(i).clientID);
				Thread ts = new Thread(ths);
				// start the thread for new request
				ts.start();
				//
				// store the instances to get the return values after all the threads finish the
				// exiecution
				threads.add(ts);
				peerThreadsList.add(ths);

			}
			for (int i = 0; i < threads.size(); i++)
				try {
					// wair for all the request threads finish the search
					((Thread) threads.get(i)).join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			for (int i = 0; i < threads.size(); i++)
				try {
					((Thread) threads.get(i)).join();
				} catch (InterruptedException e) {
					e.printStackTrace();
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
				System.out
						.println("--Found at Peer" + findAt.get(i).clientID + " on localhost:" + findAt.get(i).portNum);

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
		if (index == -1)
			return false;
		else
			return true;
	}

	private void getNeighborPeers(ArrayList<NeighbourClient> neighborPeers, int peerId) {
		// Get the Neighbor peers for the provided peer id
		String property = null;
		// NeighborPeers tempPeer=new NeighborPeers();
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("C:\\Users\\Aiden\\Documents\\NetBeansProjects\\Peer to Peer File Sharing System (RMI)\\src\\p2p_server\\config.properties");

			// load a properties file
			prop.load(input);
			property = "peerid." + peerId + ".neighbors";
			// get the property value and print it out
			String value = prop.getProperty(property);
			if (value != null) {
				String[] strNeighbors = value.split(",");

				for (int i = 0; i < strNeighbors.length; i++) {
					NeighbourClient tempPeer = new NeighbourClient();
					//
					// get th peer detials
					tempPeer.clientID = strNeighbors[i];
					tempPeer.ipAddress = prop.getProperty(strNeighbors[i] + ".ip");
					tempPeer.portNum = Integer.parseInt(prop.getProperty(strNeighbors[i] + ".port"));
					neighborPeers.add(tempPeer);
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void removeReadMessageIDs() throws RemoteException {
		this.readMessageIDs.clear();
	}

	@Override
	public void updateLocalFiles(ArrayList<String> localFiles) throws RemoteException {
		this.localFiles = localFiles;
	}

}

class NeighbourClient {
	public String clientID;
	public int portNum;
	public String ipAddress;
}