package src.pcmonitor.net;

import java.util.ArrayList;

public class ProcessListResponsePacket extends BaseResponsePacket {
	
	public ProcessListResponsePacket(ArrayList<ServerProcess> processes) {
		mProcesses = processes;
	}
	
	public ArrayList<ServerProcess> getProcesses() {
		return mProcesses;
	}

	public void setProcesses(ArrayList<ServerProcess> processes) {
		mProcesses = processes;
	}

	private ArrayList<ServerProcess> mProcesses;
	
	private static final long serialVersionUID = 1;
}
