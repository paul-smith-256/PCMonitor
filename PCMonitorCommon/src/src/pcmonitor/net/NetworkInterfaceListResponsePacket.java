package src.pcmonitor.net;

import java.util.ArrayList;

public class NetworkInterfaceListResponsePacket extends BaseResponsePacket {
	
	public NetworkInterfaceListResponsePacket(ArrayList<NetInterface> interfaces) {
		super();
		mInterfaces = interfaces;
	}

	public ArrayList<NetInterface> getInterfaces() {
		return mInterfaces;
	}

	public void setInterfaces(ArrayList<NetInterface> interfaces) {
		mInterfaces = interfaces;
	}

	private ArrayList<NetInterface> mInterfaces;
	
	private static final long serialVersionUID = 1;
}
