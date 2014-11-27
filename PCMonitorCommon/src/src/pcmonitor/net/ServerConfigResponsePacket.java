package src.pcmonitor.net;

public class ServerConfigResponsePacket extends BaseResponsePacket {

	public ServerConfigResponsePacket(ServerConfig cfg) {
		mServerConfig = cfg;
	}
	
	public ServerConfig getServerConfig() {
		return mServerConfig;
	}
	
	private ServerConfig mServerConfig;
	
	private static final long serialVersionUID = 1;
}
