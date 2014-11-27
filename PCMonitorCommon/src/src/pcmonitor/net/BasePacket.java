package src.pcmonitor.net;

import java.io.Serializable;

public abstract class BasePacket implements Serializable {

	public void setId(long id) {
		mId = id;
	}
	
	public long getId() {
		return mId;
	}
	
	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[id = "  +  mId + "]";
	}
	
	private long mId = INVALID_PACKET_ID;
	
	private static final long serialVersionUID = 1;
	
	public static final int MAX_PACKET_SIZE = 8192;
	public static final long INVALID_PACKET_ID = -1;
}
