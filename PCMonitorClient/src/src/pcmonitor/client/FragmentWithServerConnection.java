package src.pcmonitor.client;

import src.pcmonitor.net.Server;
import android.os.Bundle;

public class FragmentWithServerConnection extends FragmentWithNetworkServiceConnection {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mServer = (Server) getArguments().getSerializable(SystemInfoFragment.EXTRA_SERVER);
	}
	
	protected Server getServer() {
		return mServer;
	}
	
	private Server mServer;
}
