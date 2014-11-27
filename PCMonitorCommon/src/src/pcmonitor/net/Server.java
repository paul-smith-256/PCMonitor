package src.pcmonitor.net;

import java.io.Serializable;

public class Server implements Serializable {

	public Server(ServerIdentity identity, ServerConfig config) {
		mIdentity = identity;
		mConfig = config;
	}
	
	public ServerIdentity getIdentity() {
		return mIdentity;
	}
	
	public ServerConfig getConfig() {
		return mConfig;
	}
	
	private ServerIdentity mIdentity;
	private ServerConfig mConfig;
	
	private static final long serialVersionUID = 1;
}
