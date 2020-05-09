package p2p_server;

import java.io.Serializable;
import java.util.ArrayList;

public class HitQuery implements Serializable {
	private static final long serialVersionUID = 1L;
	public ArrayList<String> paths = new ArrayList<String>();
	public ArrayList<ClientDetails> foundClients = new ArrayList<ClientDetails>();
}
