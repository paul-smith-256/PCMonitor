package src.pcmonitor.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import src.pcmonitor.client.R.menu;
import src.pcmonitor.net.BaseResponsePacket;
import src.pcmonitor.net.ProcessListRequestPacket;
import src.pcmonitor.net.ProcessListResponsePacket;
import src.pcmonitor.net.ServerProcess;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ProcessListFragment extends PlotManagerFragment {
	
	public static ProcessListFragment newInstance(Bundle args) {
		ProcessListFragment result = new ProcessListFragment();
		result.setArguments(args);
		return result;
	}
	
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		mAdapter = new Adapter();
		if (state != null) {
			@SuppressWarnings("unchecked")
			ArrayList<ServerProcess> processes = (ArrayList<ServerProcess>) 
					state.getSerializable(STATE_PROCESSES);
			for (ServerProcess p: processes) {
				mAdapter.add(p);
			}
			mComparator = state.getInt(SETTINGS_SORT_BY);
			mSortDescending = state.getBoolean(SETTING_SORT_DESCENDING);
		}
		else {
			SharedPreferences pref = getActivity().getSharedPreferences(SHARED_PREFERNCES, Context.MODE_PRIVATE);
			int comparator = pref.getInt(SETTINGS_SORT_BY, DEFAULT_SORTING_ORDER);
			if (comparator < 0 || comparator > mComparators.length - 1) {
				comparator = DEFAULT_SORTING_ORDER;
			}
			mComparator = comparator;
			mSortDescending = pref.getBoolean(SETTING_SORT_DESCENDING, false);
			getCurrentComparator().setSortDescending(mSortDescending);
		}
		setHasOptionsMenu(true);
 	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle state) {
		View result = inflater.inflate(R.layout.fragment_process_list, container, false);
		mList = (ListView) result.findViewById(R.id.processList_list);
		mList.setAdapter(mAdapter);
		mList.setEmptyView(result.findViewById(R.id.processList_noData));	
		return result;
	}
	
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		if (!isUpdatingUI() && isVisibleToUser) {
			startUpdatingUI(new ProcessListRequestPacket());
		}
		super.setUserVisibleHint(isVisibleToUser);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		ArrayList<ServerProcess> processes = new ArrayList<ServerProcess>();
		int size = mAdapter.getCount();
		for (int i = 0; i < size; i++) {
			processes.add(mAdapter.getItem(i));
		}
		Log.d(LOG_TAG, "Saving list state");
		outState.putSerializable(STATE_PROCESSES, processes);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.options_process_list, menu);
		super.onCreateOptionsMenu(menu, inflater);
		menu.findItem(R.id.processList_sortDescendingItem).setChecked(mSortDescending);
		menu.findItem(comparatorToMenuItemMapping.get(mComparator)).setChecked(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (menuItemToComparatorMapping.containsKey(id)) {
			updateSortingType(menuItemToComparatorMapping.get(id), mSortDescending);
			item.setChecked(true);
			return true;
		}
		
		if (id == R.id.processList_sortDescendingItem) {
			updateSortingType(mComparator, !mSortDescending);
			item.setChecked(mSortDescending);
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private SharedPreferences obtainSharedPreferences() {
		return getActivity().getSharedPreferences(SHARED_PREFERNCES, Context.MODE_PRIVATE);
	}
	
	private void updateSortingType(int comparator, boolean sortDescending) {
		mComparator = comparator;
		mSortDescending = sortDescending;
		obtainSharedPreferences().edit()
				.putInt(SETTINGS_SORT_BY, mComparator)
				.putBoolean(SETTING_SORT_DESCENDING, mSortDescending)
				.commit();
		getCurrentComparator().setSortDescending(mSortDescending);
		mAdapter.sort(mComparators[mComparator]);
	}
	
	private ReversableComparator getCurrentComparator() {
		return mComparators[mComparator];
	}
	
	@Override
	protected void updateOtherUIComponents(BaseResponsePacket p) {
		if (!(p instanceof ProcessListResponsePacket)) {
			Log.d(LOG_TAG, "Unexpected packet " + p);
			return;
		}
		ProcessListResponsePacket response = (ProcessListResponsePacket) p;
		Log.d(LOG_TAG, "Updating list model");
		Parcelable state = mList.onSaveInstanceState();
		mAdapter.clear();
		for (ServerProcess proc: response.getProcesses()) {
			mAdapter.add(proc);
		}
		mAdapter.sort(mComparators[mComparator]);
		mList.onRestoreInstanceState(state);
	}
	
	@Override
	protected float[] getPlotDataFromPacket(BaseResponsePacket p) {
		return null;
	}
	
	private class Adapter extends ArrayAdapter<ServerProcess> {
		
		public Adapter() {
			super(getActivity(), android.R.layout.simple_list_item_2);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View result;
			if (convertView == null) {
				result = getActivity().getLayoutInflater().inflate(R.layout.row_process_info, parent, false);
			}
			else {
				result = convertView;
			}
			
			TextView processName = (TextView) result.findViewById(R.id.processList_processExecutable);
			TextView processDescr = (TextView) result.findViewById(R.id.processView_processDetails);
			
			ServerProcess p = getItem(position);
			processName.setText(p.getExecutable());
			processDescr.setText(String.format(
					getResources().getString(R.string.processList_processDetailsFormat),
					p.getOwner(), p.getMemory() / 1024, (int) (p.getCpu() * 100)));
			
			return result;
		}
	};
	
	private static abstract class ReversableComparator implements Comparator<ServerProcess> {
		
		@Override
		public int compare(ServerProcess lhs, ServerProcess rhs) {
			if (mSortDescending) {
				return internalCompare(rhs, lhs);
			}
			else {
				return internalCompare(lhs, rhs);
			}
		}
		
		protected abstract int internalCompare(ServerProcess a, ServerProcess b);
		
		public void setSortDescending(boolean v) {
			mSortDescending = v;
		}
		
		public boolean isSortDescending() {
			return mSortDescending;
		}
		
		private boolean mSortDescending;
	}
	
	private static class ProcessExecutableComparator extends ReversableComparator {
		
		@Override
		protected int internalCompare(ServerProcess a, ServerProcess b) {
			return a.getExecutable().compareToIgnoreCase(b.getExecutable());
		}
	}
	
	private static class ProcessMemoryComparator extends ReversableComparator {
		
		@Override
		protected int internalCompare(ServerProcess lhs, ServerProcess rhs) {
			return (int) (lhs.getMemory() - rhs.getMemory());
		}
	}
	
	private static class ProcessCpuComparator extends ReversableComparator {
		
		@Override
		protected int internalCompare(ServerProcess lhs, ServerProcess rhs) {
			return (int) ((lhs.getCpu() - rhs.getCpu()) * 100);
		}
	}
	
	private static class ProcessOwnerComparator extends ReversableComparator {
		
		@Override
		protected int internalCompare(ServerProcess lhs, ServerProcess rhs) {
			return lhs.getOwner().compareToIgnoreCase(rhs.getOwner());
		}
	}
	
	private Adapter mAdapter;
	private ListView mList;
	private boolean mSortDescending;
	private int mComparator;
	private ReversableComparator[] mComparators = {
			new ProcessExecutableComparator(),
			new ProcessOwnerComparator(),
			new ProcessCpuComparator(),
			new ProcessMemoryComparator()
	}; 
	
	private static final String LOG_TAG = ProcessListFragment.class.getSimpleName();
	private static final String STATE_PROCESSES = "processes";
	
	private static final int 
			SORT_BY_EXECUTABLE 	= 0,
			SORT_BY_OWNER 		= 1,
			SORT_BY_CPU 		= 2,
			SORT_BY_MEMORY		= 3;
	private static final int DEFAULT_SORTING_ORDER = SORT_BY_EXECUTABLE;
	
	private static final String 
			SETTINGS_SORT_BY = "sortBy",
			SETTING_SORT_DESCENDING = "sortDescending";
	
	private static final String SHARED_PREFERNCES = "processListFragment";
	
	private static final HashMap<Integer, Integer> comparatorToMenuItemMapping = new HashMap<Integer, Integer>();
	private static final HashMap<Integer, Integer> menuItemToComparatorMapping = new HashMap<Integer, Integer>();
	static {
		comparatorToMenuItemMapping.put(SORT_BY_CPU, R.id.processList_sortByCpuItem);
		comparatorToMenuItemMapping.put(SORT_BY_EXECUTABLE, R.id.processList_sortByExecutableItem);
		comparatorToMenuItemMapping.put(SORT_BY_MEMORY, R.id.processList_sortByMemoryItem);
		comparatorToMenuItemMapping.put(SORT_BY_OWNER, R.id.processList_sortByOwnerItem);
		for (Map.Entry<Integer, Integer> e: comparatorToMenuItemMapping.entrySet()) {
			menuItemToComparatorMapping.put(e.getValue(), e.getKey());
		}
	}

}
