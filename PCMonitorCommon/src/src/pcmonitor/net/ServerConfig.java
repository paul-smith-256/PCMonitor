package src.pcmonitor.net;

import java.io.Serializable;

public class ServerConfig implements Serializable {
	
	public ServerConfig(int cpuCount, long ramSize) {
		mCpuCount = cpuCount;
		mRamSize = ramSize;
	}
	
	public int getCpuCount() {
		return mCpuCount;
	}
	
	public long getRamSize() {
		return mRamSize;
	}
	
	private int mCpuCount;
	private long mRamSize;
	
	private static final long serialVersionUID = 1;
}
