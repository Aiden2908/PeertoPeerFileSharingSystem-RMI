package p2p_server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class NeighborConnection extends Thread {
	String fileName;
	int portNum;
	String ipAddress;
	String messageID;
	int fromClientPort;
	int fromClientID;
	ArrayList<ClientDetails> dirFilesFoundAt = new ArrayList<ClientDetails>();
	HitQuery hitQueryResult = new HitQuery();
	String toClientID;

	NeighborConnection(String ipAddress, int portNum, String fileName, String messageID, int fromClientID, String toClientID) {
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		this.fromClientID = fromClientID;
		this.fileName = fileName;
		;
		this.messageID = messageID;
		this.toClientID = toClientID;
	}

	@Override
	public void run() {
		I_ClientRMI client = null;
		try {
			client = (I_ClientRMI) Naming.lookup("rmi://" + ipAddress + ":" + portNum + "/subServer");
			hitQueryResult = client.query(fromClientID, messageID, fileName);
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			System.out.println("Unable to connect to " + toClientID + " : " + e.getMessage());
		}

	}

	public HitQuery getValue() {
		return hitQueryResult;
	}

}
