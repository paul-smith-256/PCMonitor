package src.pcmonitor.net;


public abstract class BaseRequestPacket extends BasePacket {
	
	public BaseRequestPacket(boolean waitForResponse, 
			boolean waitForMultipleResponses) {
		mWaitForResponse = waitForResponse;
		mWaitForMultipleResponses = waitForMultipleResponses;
	}
	
	public final boolean waitForResponse() {
		return mWaitForResponse;
	}
	
	public final boolean waitForMultipleResponses() {
		return mWaitForMultipleResponses;
	}
	
	private boolean mWaitForResponse;
	private boolean mWaitForMultipleResponses;
	
	private static final long serialVersionUID = 1;
}