package indexServer;

import java.io.Serializable;

public class ConnectedClient implements Serializable {
	private static final long serialVersionUID = 1L;
	public int clientID;
    public int portNumber;
    
	public String toString() {
		return "{Client ID: " + clientID + ", Port: " + portNumber + "}";
	}
}