package indexServer;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class IndexServer  extends UnicastRemoteObject implements I_IndexServer{
	private int numClients=0;
	private ArrayList<ConnectedClient> connectedClients;
	public IndexServer() throws RemoteException {
		super();
		connectedClients=new ArrayList<>();
	}
	
	@Override
	public void newClient(int clientID, int portNum) throws RemoteException {
		ConnectedClient connectedClient = new ConnectedClient();
		connectedClient.clientID=clientID;
		connectedClient.portNumber=portNum;
		
		connectedClients.add(connectedClient);
		System.out.println("Added: "+connectedClients.get(connectedClients.size()-1).toString());
		numClients++;
	}

	@Override
	public ArrayList<ConnectedClient> getConnectClients() throws RemoteException {
		return connectedClients;
	}	
	
	public static void main(String[] args) {
		try {
			LocateRegistry.createRegistry(1099);
			IndexServer indexServer = new IndexServer();
			Naming.rebind("index", indexServer);
			System.out.println("Index server running.");
			
		} catch (RemoteException e1) {
			System.out.println("Error: "+e1);
		} catch (MalformedURLException e1) {
			System.out.println("Error: "+e1);
		}
	}

}
