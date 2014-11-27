package src.pcmonitor.net;

import src.pcmonitor.net.BaseRequestPacket;

public class SearchRequestPacket extends BaseRequestPacket {

	public SearchRequestPacket() {
		super(true, true);
	}
	
	private static final long serialVersionUID = 1;
}
