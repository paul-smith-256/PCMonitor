package src.pcmonitor.client;

import java.util.ArrayList;

import src.pcmonitor.client.net.NetworkService;
import src.pcmonitor.net.BaseResponsePacket;
import src.pcmonitor.net.NetInterface;
import src.pcmonitor.net.NetworkInterfaceListRequestPacket;
import src.pcmonitor.net.NetworkInterfaceListResponsePacket;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class NetworkInterfaceListFragment extends FragmentWithServerConnection {
	
	public static NetworkInterfaceListFragment newInstance(Bundle args) {
		NetworkInterfaceListFragment result = new NetworkInterfaceListFragment();
		result.setArguments(args);
		result.setHasOptionsMenu(true);
		return result;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle state) {
		View result = inflater.inflate(R.layout.fragment_network_interface_list, container, false);
		mAdapter = new InterfacesListAdapter(getActivity());
		if (state != null) {
			@SuppressWarnings("unchecked")
			ArrayList<NetInterface> ifaces = (ArrayList<NetInterface>) state.getSerializable(EXTRA_INTERFACE_LIST);
			mAdapter.setItems(ifaces);
		}
		ListView list = (ListView) result.findViewById(R.id.networkInterfaceList_list);
		list.setAdapter(mAdapter);
		list.setEmptyView(result.findViewById(R.id.networkInterfaceList_noInterfacesView));
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				InterfacesListAdapter adapter = (InterfacesListAdapter) parent.getAdapter();
				NetInterface iface = adapter.getItem(position);
				NetworkInterfaceDetails frag = NetworkInterfaceDetails.newInstance(iface);
				frag.show(getChildFragmentManager(), null);
			}
		});
		return result;
	}
	
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (isVisibleToUser) {
			updateList();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(EXTRA_INTERFACE_LIST, mAdapter.getItems());
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.options_network_interface_list, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.networkInterfaceList_refreshItem:
			updateList();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void updateList() {
		if (isServiceReady()) {
			getService().send(getServer().getIdentity(), new NetworkInterfaceListRequestPacket(), mUpdateHandler);
		}
		else {
			showUpdateFailedToast();
		}
	}
	
	private void showUpdateFailedToast() {
		if (getUserVisibleHint()) {
			Toast.makeText(getActivity(), R.string.networkInterfaceList_updateFailed, Toast.LENGTH_LONG).show();
		}
	}
	
	private static class InterfacesListAdapter extends BaseAdapter {
		
		public InterfacesListAdapter(Activity activity) {
			mActivity = activity;
			mInterfaces = new ArrayList<NetInterface>();
		}
		
		public void setItems(ArrayList<NetInterface> interfaces) {
			mInterfaces = interfaces;
			notifyDataSetChanged();
		}
		
		public ArrayList<NetInterface> getItems() {
			return mInterfaces;
		}
		
		@Override
		public NetInterface getItem(int position) {
			return mInterfaces.get(position);
		}
		
		@Override
		public int getCount() {
			return mInterfaces.size();
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View result;
			if (convertView != null) {
				result = convertView;
			}
			else {
				result =  mActivity.getLayoutInflater().inflate(R.layout.row_interface_info, parent, false);
			}
			
			TextView descrView = (TextView) result.findViewById(R.id.networkInterfaces_descriptionText);
			TextView addressView = (TextView) result.findViewById(R.id.networkInterfaces_addressText);
			TextView netmaskView = (TextView) result.findViewById(R.id.networkInterfaces_netmaskText);
			
			Resources r = mActivity.getResources();
			String addressFormat = r.getString(R.string.networkInterfaceList_addressFormat);
			String netmaskFormat = r.getString(R.string.networkIntrefaceList_netmaskFromat);
			
			NetInterface iface = mInterfaces.get(position);
			descrView.setText(iface.getDescritpion());
			addressView.setText(String.format(addressFormat, iface.getAddress()));
			netmaskView.setText(String.format(netmaskFormat, iface.getNetmask()));
			
			return result;
		}
		
		private ArrayList<NetInterface> mInterfaces;
		private Activity mActivity;
	}
	
	private class UpdateButtonClickListener implements View.OnClickListener {
		
		@Override
		public void onClick(View v) {
			updateList();
		}
	}
	
	private Handler mUpdateHandler = new Handler() {
		
		public void handleMessage(Message m) {
			switch (m.what) {
			case NetworkService.RESULT_RESPONSE:
				BaseResponsePacket p = (BaseResponsePacket) m.getData()
						.getSerializable(NetworkService.EXTRA_RESPONSE_PACKET);
				if (!(p instanceof NetworkInterfaceListResponsePacket)) {
					Log.d(LOG_TAG, "Unexpected packet " + p);
					return;
				}
				NetworkInterfaceListResponsePacket response = (NetworkInterfaceListResponsePacket) p;
				sanitizeResponse(response);
				mAdapter.setItems(response.getInterfaces());
				break;
			case NetworkService.RESULT_NO_REPLY:
			case NetworkService.RESULT_NOT_SENT:
				showUpdateFailedToast();
				break;
			}
		}
		
		private void sanitizeResponse(NetworkInterfaceListResponsePacket p) {
			String defaultIfaceName = getResources().getString(R.string.networkInterfaceList_defaultInterfaceName);
			for (NetInterface i: p.getInterfaces()) {
				if (i.getName() == null) {
					i.setName(defaultIfaceName);
				}
				if (i.getDescritpion() == null) {
					i.setDescritpion(defaultIfaceName);
				}
				if (i.getAddress() == null) {
					i.setAddress("");
				}
				if (i.getNetmask() == null) {
					i.setNetmask("");
				}
				if (i.getType() == null) {
					i.setType("");
				}
			}
		}
	};
	
	private InterfacesListAdapter mAdapter;
	
	private static final String LOG_TAG = NetworkInterfaceListFragment.class.getSimpleName();
	private static final String EXTRA_INTERFACE_LIST = "interfaceList";
}
