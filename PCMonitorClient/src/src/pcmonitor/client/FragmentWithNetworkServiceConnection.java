package src.pcmonitor.client;

import src.pcmonitor.client.net.NetworkService;
import src.pcmonitor.client.net.ServiceAccessWrapper;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

public class FragmentWithNetworkServiceConnection extends Fragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mWrapper = new ServiceAccessWrapper(getActivity().getApplicationContext());
		Log.d("LOG", "Binding " + getClass().getSimpleName());
		mWrapper.bind();
	}
	
	@Override
	public void onDestroy() {
		Log.d("LOG", "Unbinding " + getClass().getSimpleName());
		mWrapper.unbind();
		super.onDestroy();
	}
	
	protected NetworkService getService() {
		return mWrapper.getService();
	}
	
	protected boolean isServiceReady() {
		return mWrapper.isServiceReady();
	}
	
	private ServiceAccessWrapper mWrapper;
}
