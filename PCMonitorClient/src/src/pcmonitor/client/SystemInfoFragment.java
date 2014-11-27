package src.pcmonitor.client;

import static src.pcmonitor.client.SystemInfoGroup.*;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SystemInfoFragment extends Fragment {
	
	public static SystemInfoFragment newInstance(Bundle args) {
		SystemInfoFragment result = new SystemInfoFragment();
		result.setArguments(args);
		return result;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle state) {
		View result = inflater.inflate(R.layout.fragment_system_info, root, false);
		ViewPager pager = (ViewPager) result.findViewById(R.id.systemInfoFragment_viewPager);
		pager.setAdapter(new Adapter());
		if (state == null) {
			pager.setCurrentItem(getArguments().getInt(EXTRA_TAB_ID));
		}
		return result;
		
	}
	
	private class Adapter extends FragmentPagerAdapter {
		
		public Adapter() {
			super(getChildFragmentManager());
		}
		
		@Override
		public int getCount() {
			return SystemInfoGroup.values().length;
		}
		
		@Override
		public Fragment getItem(int pos) {
			Bundle args = getArguments();
			if (pos == CPU_LOAD.ordinal()) {
				return CpuLoadFragment.newInstance(args);
			}
			else if (pos == PROCESS_LIST.ordinal()) {
				return ProcessListFragment.newInstance(args);
			}
			else if (pos == MEMORY_USAGE.ordinal()) {
				return MemoryUsageFragment.newInstance(args);
			}
			else if (pos == NETWORK_INTERFACES.ordinal()) {
				return NetworkInterfaceListFragment.newInstance(args);
			}
			else {
				return null;
			}
		}
		
		@Override
		public CharSequence getPageTitle(int pos) {
			Resources r = getResources();
			Integer id = null;
			if (pos == CPU_LOAD.ordinal()) {
				id = R.string.cpuLoad_title;
			}
			else if (pos == PROCESS_LIST.ordinal()) {
				id = R.string.processList_title;
			}
			else if (pos == MEMORY_USAGE.ordinal()) {
				id = R.string.memoryUsage_title;
			}
			else if (pos == NETWORK_INTERFACES.ordinal()) {
				id = R.string.networkInterfaceList_title;
			}

			if (id == null) {
				return null;
			}
			else {
				return r.getString(id);
			}
		}
	}
	
	public static final String EXTRA_SERVER = "server";
	public static final String EXTRA_TAB_ID = "tabId";
	private static final String LOG_TAG = SystemInfoFragment.class.getSimpleName();
}
