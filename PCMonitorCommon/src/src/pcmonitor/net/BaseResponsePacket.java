package src.pcmonitor.net;


public abstract class BaseResponsePacket extends BasePacket {
	
	public BaseResponsePacket() {
		mTimestamp = System.currentTimeMillis();
	}
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	private long mTimestamp;
	
	private static final long serialVersionUID = 1;
}
