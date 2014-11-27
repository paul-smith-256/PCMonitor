package src.pcmonitor.net;

public class MemUsageResponsePacket extends BaseResponsePacket {
	
	public MemUsageResponsePacket(long total, long used, long free, 
			long swapUsed, long swapFree) {
		mTotal = total;
		mUsed = used;
		mFree = free;
		mSwapUsed = swapUsed;
		mSwapFree = swapFree;
	}
	
	public long getTotal() {
		return mTotal;
	}

	public long getUsed() {
		return mUsed;
	}

	public long getFree() {
		return mFree;
	}
	
	public long getSwapUsed() {
		return mSwapUsed;
	}

	public long getSwapFree() {
		return mSwapFree;
	}
	
	private long mTotal, mUsed, mFree;
	private float mUsedPercent;
	private long mSwapUsed, mSwapFree;
}
