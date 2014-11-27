package src.pcmonitor.net;

public class CpuLoadRequestPacket extends BaseRequestPacket {
	
	public CpuLoadRequestPacket() {
		super(true, false);
	}
	
	public static final long serialVersionUID = 1;
}
