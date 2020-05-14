package p2p_server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

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
	
	private HashMap<Integer, Integer> timestampMap;
	ArrayList<ClientDetails> dirFilesFoundAt = new ArrayList<ClientDetails>();
	HitQuery hitQueryResult = new HitQuery();
	private NeighbourClient nc;
	
	public NeighborConnection(String ipAddress, int portNum, String fileName, String messageID, int fromClientID, String toClientID, HashMap<Integer, Integer> timestampMap) { // Used for searching
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		this.fromClientID = fromClientID;
		this.fileName = fileName;
		this.messageID = messageID;
		this.toClientID = toClientID;
		this.messageType = "search";
		this.timestampMap = timestampMap;
	}
	
	public NeighborConnection(NeighbourClient nc, String ipAddress, int portNum, String messageType, String searchID, boolean isLeader, HashMap<Integer, Integer> timestampMap) { // Used for updating ring
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		this.messageType = messageType;
		this.searchID = searchID;
		this.nc = nc;
		this.isLeader = isLeader;
		this.timestampMap = timestampMap;
	}
	
	public NeighborConnection(NeighbourClient nc, String ipAddress, int portNum, String messageType, int searchIDint, boolean isLeader, HashMap<Integer, Integer> timestampMap) { // Used for updating ring
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		this.messageType = messageType;
		this.searchIDint = searchIDint;
		this.nc = nc;
		this.isLeader = isLeader;
		this.timestampMap = timestampMap;
	}

	@Override
	public void run() {
		I_ClientRMI client = null;
		
		if(messageType.equals("connect")) { // connection a new client
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				client.changeConnectionConnect(nc, searchID, timestampMap);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
			
		} else if(messageType.equals("disconnect")) { // disconnecting a client
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				client.changeConnectionDisconnect(nc, searchID, isLeader, timestampMap);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
			
		} else if (messageType.equals("election") || messageType.equals("leader")) { // electing a new leader
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				client.changRobertsRecieveMessage(messageType, searchIDint, timestampMap);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
			
		} else if (messageType.equals("search")) { // searching for a file
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				hitQueryResult = client.query(fromClientID, messageID, fileName, timestampMap);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
		} else if (messageType.equals("snapshot")) { // searching for a file
			try {
				client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
				client.printNeighbour(searchIDint,timestampMap);
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
			}
		}
	}

	public HitQuery getValue() {
		return hitQueryResult;
	}
}
