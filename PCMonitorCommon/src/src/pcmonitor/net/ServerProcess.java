package src.pcmonitor.net;

import java.io.Serializable;

public class ServerProcess implements Serializable {
	
	public ServerProcess(String executable, String owner, long memory, float cpu) {
		super();
		mExecutable = executable;
		mOwner = owner;
		mMemory = memory;
		mCpu = cpu;
	}

	public String getExecutable() {
		return mExecutable;
	}
	
	public void setExecutable(String executable) {
		mExecutable = executable;
	}
	
	public String getOwner() {
		return mOwner;
	}
	
	public void setOwner(String owner) {
		mOwner = owner;
	}
	
	public long getMemory() {
		return mMemory;
	}
	
	public void setMemory(long memory) {
		mMemory = memory;
	}
	
	public float getCpu() {
		return mCpu;
	}
	
	public void setCpu(float cpu) {
		mCpu = cpu;
	}
	
	private String mExecutable;
	private String mOwner;
	private long mMemory;
	private float mCpu;
	
	private static final long serialVersionUID = 1;
}
