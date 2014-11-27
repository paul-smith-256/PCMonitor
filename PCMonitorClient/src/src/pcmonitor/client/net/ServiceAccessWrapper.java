package src.pcmonitor.client.net;

import src.pcmonitor.client.net.NetworkService.NetworkServiceBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class ServiceAccessWrapper {
	
	public ServiceAccessWrapper(Context context) {
		mContext = context;
	}
	
	public void bind() {
		Intent intent = new Intent(mContext, NetworkService.class);
		mConnection = new NetworkServiceConnection();
		mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	public void unbind() {
		mContext.unbindService(mConnection);
		mService = null;
	}
	
	private class NetworkServiceConnection implements ServiceConnection {
		
		@Override 
		public void onServiceConnected(ComponentName name, IBinder binder) {
			mService = ((NetworkServiceBinder) binder).getService();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	}
	
	public boolean isServiceReady() {
		return mService != null && mService.isReady();
	}
	
	public NetworkService getService() {
		return mService;
	}
	
	private Context mContext;
	private NetworkService mService;
	private NetworkServiceConnection mConnection;
}
