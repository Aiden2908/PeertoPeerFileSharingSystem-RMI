package p2p_server;
import java.rmi.*;
import java.util.ArrayList;

public interface I_ClientRMI extends Remote{
	public HitQuery query(int fromPeerId,String msgId,String fileName)throws RemoteException;
	public byte[] obtain(String filename)throws RemoteException;
	public void removeReadMessageIDs()throws RemoteException;
        public void updateLocalFiles(ArrayList<String> localFiles)throws RemoteException;
}
