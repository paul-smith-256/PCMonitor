package src.pcmonitor.net;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class ServerIdentity implements Serializable {
	
	public ServerIdentity(String name, InetSocketAddress address) {
		mName = name;
		mAddress = address;
	}
	
	public String getName() {
		return mName;
	}

	public InetSocketAddress getAddress() {
		return mAddress;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (this.getClass() != o.getClass()) {
			return false;
		}
		ServerIdentity other = (ServerIdentity) o;
		return mName.equals(other.mName) && mAddress.equals(other.mAddress);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[name = " + mName + ", address = " + mAddress + "]";
	}
	
	private final String mName;
	private final InetSocketAddress mAddress;

	private static final long serialVersionUID = 1;
	public static final ServerIdentity broadcast = 
			new ServerIdentity("broadcast", new InetSocketAddress("255.255.255.255", Ports.SERVER_PORT));
}
