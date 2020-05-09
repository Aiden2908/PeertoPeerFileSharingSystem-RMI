package indexServer;

import java.io.Serializable;

public class ConnectedClient implements Serializable
{
        public int clientID;
        public int portNumber;
        
		public String toString() {
			return "{Client ID: "+clientID+", Port: "+portNumber+"}";
		}
}