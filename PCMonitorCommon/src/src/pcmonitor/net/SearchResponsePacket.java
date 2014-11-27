package src.pcmonitor.net;

public class SearchResponsePacket extends BaseResponsePacket {
	
	public SearchResponsePacket(String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}
	
	private String mName;
	
	private static final long serialVersionUID = 1;
}
