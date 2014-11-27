package src.pcmonitor.client;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import src.pcmonitor.client.net.NetworkService;
import src.pcmonitor.client.net.ServiceAccessWrapper;
import src.pcmonitor.net.BaseResponsePacket;
import src.pcmonitor.net.SearchRequestPacket;
import src.pcmonitor.net.SearchResponsePacket;
import src.pcmonitor.net.Server;
import src.pcmonitor.net.ServerConfigRequestPacket;
import src.pcmonitor.net.ServerConfigResponsePacket;
import src.pcmonitor.net.ServerIdentity;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ServerListFragment extends FragmentWithNetworkServiceConnection {
	
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setRetainInstance(true);
		mServersAdapter = new ServerListAdapter(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle state) {
		View result = inflater.inflate(R.layout.fragment_server_list, root, false);
		mServerListView = (ListView) result.findViewById(R.id.serverList_serverListView);
		mServerListView.setEmptyView((TextView) result.findViewById(R.id.serverList_noServersView));
		mServerListView.setAdapter(mServersAdapter);
		mServerListView.setOnItemClickListener(mServerClickListener);
		mSearchButton = (Button) result.findViewById(R.id.serverList_searchButton);
		mSearchButton.setOnClickListener(mSearchButtonClickListener);
		mSearchProgressBar = (ProgressBar) result.findViewById(R.id.serverList_searchProgressBar);
		mSearchProgressBar.setVisibility(mSearchInProgress ? View.VISIBLE : View.GONE);
		mSearchButton.setEnabled(!mSearchInProgress);
		registerForContextMenu(mServerListView);
		return result;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo cmi) {
		super.onCreateContextMenu(menu, v, cmi);
		if (v.getId() == R.id.serverList_serverListView) {
			getActivity().getMenuInflater().inflate(
					R.menu.context_server_list_server_actions, menu);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		SystemInfoGroup infoGroup;
		switch (item.getItemId()) {
		case R.id.serverList_showCpuLoadItem:
			infoGroup = SystemInfoGroup.CPU_LOAD;
			break;
		case R.id.serverList_showProcessListItem:
			infoGroup = SystemInfoGroup.PROCESS_LIST;
			break;
		case R.id.serverList_showNetworkInterfacesItem:
			infoGroup = SystemInfoGroup.NETWORK_INTERFACES;
			break;
		case R.id.serverList_showMemoryUsage:
			infoGroup = SystemInfoGroup.MEMORY_USAGE;
			break;
		default:
			return super.onContextItemSelected(item);
		}
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		showSystemInfo(mServersAdapter.getItem(menuInfo.position), infoGroup);
		return true;
	}
	
	private void startSearch() {
		if (isServiceReady()) {
			if (mServerIdentities == null) {
				mServerIdentities = new LinkedList<ServerIdentity>();
			}
			mServerIdentities.clear();
			mServersAdapter.clear();
			mSearchProgressBar.setVisibility(View.VISIBLE);
			mSearchButton.setEnabled(false);
			getService().send(ServerIdentity.broadcast, 
					new SearchRequestPacket(), mSearchHandler);
			mSearchInProgress = true;
		}
	}
	
	private void searchFinished() {
		mSearchProgressBar.setVisibility(View.GONE);
		mSearchButton.setEnabled(true);
		mSearchInProgress = false;
	}
	
	private void showSystemInfo(Server server, SystemInfoGroup infoGroup) {
		if (getActivity() instanceof SystemInfoLink) {
			SystemInfoLink link = (SystemInfoLink) getActivity();
			link.showSystemInfo(server, infoGroup);
		}
		else {
			Log.w(LOG_TAG, "Hosting activity doesn't implement " + SystemInfoLink.class.getName());
		}
	}
	
	private ServerListAdapter mServersAdapter;
	private ListView mServerListView;
	private Button mSearchButton;
	private ProgressBar mSearchProgressBar;
	private boolean mSearchInProgress;
	
	private List<ServerIdentity> mServerIdentities;
	
	private View.OnClickListener mSearchButtonClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			startSearch();
		}
	};
	
	
	private static class ServerListAdapter extends ArrayAdapter<Server> {
		
		public ServerListAdapter(Activity context) {
			super(context, R.layout.row_server_info);
		}
		
		private static class ViewHolder {
			
			ViewHolder(TextView name, TextView ip) {
				this.name = name;
				this.ip = ip;
			}
			
			final TextView name, ip;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup group) {
			View result = convertView;
			if (result == null) {
				LayoutInflater inf = ((Activity) getContext()).getLayoutInflater();
				result = inf.inflate(R.layout.row_server_info, group, false);
				ViewHolder vh = new ViewHolder(
						(TextView) result.findViewById(R.id.serverList_serverName),
						(TextView) result.findViewById(R.id.serverList_serverIp));
				result.setTag(vh);
			}
			
			ViewHolder vh = (ViewHolder) result.getTag();
			TextView nameText = vh.name;
			TextView ipText = vh.ip;
			ServerIdentity identity  = getItem(position).getIdentity();
			nameText.setText(identity.getName());
			ipText.setText(identity.getAddress().getAddress().getHostAddress());
			
			return result;
		}
	}
	
	private Handler mSearchHandler = new Handler() {
		
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case NetworkService.RESULT_NOT_SENT: {
				Toast.makeText(getActivity(), R.string.failedToSendMessage, 
						Toast.LENGTH_LONG).show();
				searchFinished();
				break;
			}
			case NetworkService.RESULT_NO_REPLY:
			case NetworkService.RESULT_SUCCSESS:
				searchFinished();
				break;
			case NetworkService.RESULT_RESPONSE: {
				Bundle data = m.getData();
				InetSocketAddress sender = (InetSocketAddress) 
						data.getSerializable(NetworkService.EXTRA_ADDRESS);
				BaseResponsePacket packet = (BaseResponsePacket)
						data.getSerializable(NetworkService.EXTRA_RESPONSE_PACKET);
				if (packet instanceof SearchResponsePacket) {
					SearchResponsePacket response = (SearchResponsePacket) packet;
					ServerIdentity newId = new ServerIdentity(response.getName(), sender);
					boolean exists = false;
					for (ServerIdentity id: mServerIdentities) {
						if (id.getAddress().getAddress().equals(newId.getAddress().getAddress())) {
							exists = true;
							break;
						}
					}
					if (!exists) {
						if (isServiceReady()) {
							getService().send(newId, new ServerConfigRequestPacket(), 
									mServerConfigHandler);
							mServerIdentities.add(newId);
						}
					}
					else {
						Log.w(LOG_TAG, "Duplicate search responses from " + newId);
					}
				}
				else {
					Log.w(LOG_TAG, "Expected " + SearchResponsePacket.class + 
							", got " + packet.getClass().getName());
				}
				break;
			}
			default:
				Log.w(LOG_TAG, "Unknown message type: " + m.what);
			}
		}
	};
	
	private Handler mServerConfigHandler = new Handler() {
		
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case NetworkService.RESULT_RESPONSE:
				Bundle data = m.getData();
				InetSocketAddress sender = (InetSocketAddress) 
						data.getSerializable(NetworkService.EXTRA_ADDRESS);
				BaseResponsePacket packet = (BaseResponsePacket) 
						data.getSerializable(NetworkService.EXTRA_RESPONSE_PACKET);
				Log.d(LOG_TAG, "Config response from " + sender);
				if (packet instanceof ServerConfigResponsePacket) {
					ServerConfigResponsePacket response = (ServerConfigResponsePacket) packet;
					Log.d(LOG_TAG, "Searching for id");
					for (ServerIdentity id: mServerIdentities) {
						Log.d(LOG_TAG, "Id = " + id);
						if (id.getAddress().getAddress().equals(sender.getAddress())) {
							Log.d(LOG_TAG, "Appending new server");
							mServersAdapter.add(new Server(id, response.getServerConfig()));
						}
					}
				}
			}
		}
	};
	
	private AdapterView.OnItemClickListener mServerClickListener = 
			new AdapterView.OnItemClickListener() {
		
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			
			showSystemInfo(mServersAdapter.getItem(position), SystemInfoGroup.CPU_LOAD);
		}
	};
	
	public static interface SystemInfoLink {
		void showSystemInfo(Server server, SystemInfoGroup infoGroup);
	}
	
	private static final String LOG_TAG = ServerListFragment.class.getName();
}
