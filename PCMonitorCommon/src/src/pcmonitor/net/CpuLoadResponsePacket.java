package src.pcmonitor.net;

public class CpuLoadResponsePacket extends BaseResponsePacket {
	
	public CpuLoadResponsePacket(float[] load) {
		mLoad = load;
	}
	
	public float[] getLoad() {
		return mLoad;
	}
	
	private float[] mLoad;
	
	private static final long serialVersionUID = 1;
}
