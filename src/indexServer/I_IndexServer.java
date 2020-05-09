package indexServer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface I_IndexServer extends Remote {
	public void newClient(int clientID,int clientPortNum) throws RemoteException;

	public ArrayList<ConnectedClient> getConnectClients() throws RemoteException;
}
