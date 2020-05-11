package p2p_server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class NeighborConnection extends Thread {	
	private String toClientID;
	private String fileName;
	private String messageID;
	private String ipAddress;
	private String messageType;
	private String searchID;
	private int searchIDint;
	private int portNum;
	private int fromClientID;
	private boolean isLeader;
	
	ArrayList<ClientDetails> dirFilesFoundAt = new ArrayList<ClientDetails>();
	HitQuery hitQueryResult = new HitQuery();
	private NeighbourClient nc;
	
	public NeighborConnection(String ipAddress, int portNum, String fileName, String messageID, int fromClientID, String toClientID) { // Used for searching
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		this.fromClientID = fromClientID;
		this.fileName = fileName;
		this.messageID = messageID;
		this.toClientID = toClientID;
		this.messageType = "search";
	}
	
	public NeighborConnection(NeighbourClient nc, String ipAddress, int portNum, String messageType, String searchID, boolean isLeader) { // Used for updating ring
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		this.messageType = messageType;
		this.searchID = searchID;
		this.nc = nc;
		this.isLeader = isLeader;
	}
	
	public NeighborConnection(NeighbourClient nc, String ipAddress, int portNum, String messageType, int searchIDint, boolean isLeader) { // Used for updating ring
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		this.messageType = messageType;
		this.searchIDint = searchIDint;
		this.nc = nc;
		this.isLeader = isLeader;
	}

	@Override
	public void run() {
		I_ClientRMI client = null;
		
		if(messageType.equals("connect")) { // connection a new client
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				client.changeConnectionConnect(nc, searchID);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
			
		} else if(messageType.equals("disconnect")) { // disconnecting a client
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				client.changeConnectionDisconnect(nc, searchID, isLeader);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
			
		} else if (messageType.equals("election") || messageType.equals("leader")) { // electing a new leader
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				client.changRobertsRecieveMessage(messageType, searchIDint);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
			
		} else if (messageType.equals("search")) { // searching for a file
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				hitQueryResult = client.query(fromClientID, messageID, fileName);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
		} else if (messageType.equals("snapshot")) { // searching for a file
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				client.printNeighbour(searchIDint);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
		}
	}

	public HitQuery getValue() {
		return hitQueryResult;
	}
}
