package p2p_server;

import java.io.Serializable;

/* A object to be passed around through rmi.*/
public class ClientDetails implements Serializable {
	private static final long serialVersionUID = 1L;
	public int portNum, clientID;
	public String ipAddress;
}
