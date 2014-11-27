package src.pcmonitor.client;

import java.util.ArrayList;

import src.pcmonitor.client.net.NetworkService;
import src.pcmonitor.client.net.ServiceAccessWrapper;
import src.pcmonitor.net.BaseResponsePacket;
import src.pcmonitor.net.CpuLoadRequestPacket;
import src.pcmonitor.net.CpuLoadResponsePacket;
import src.pcmonitor.net.Server;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class CpuLoadFragment extends PlotManagerFragment {
	
	public static CpuLoadFragment newInstance(Bundle args) {
		CpuLoadFragment result = new CpuLoadFragment();
		result.setArguments(args);
		return result;
	}
	
	@Override
	protected void updateOtherUIComponents(BaseResponsePacket p) {
		
	}
	
	@Override
	protected float[] getPlotDataFromPacket(BaseResponsePacket p) {
		if (!(p instanceof CpuLoadResponsePacket)) {
			return null;
		}
		return prepareCpuLoad((CpuLoadResponsePacket) p);
	}
	
	private float[] prepareCpuLoad(CpuLoadResponsePacket p) {
		float[] load = p.getLoad();
		float[] result;
		if (load.length >= getModels().size()) {
			result = load;
		}
		else {
			Log.d(LOG_TAG, "CPU load response packet contains fewer entries than expected");
			result = new float[getModels().size()];
			System.arraycopy(load, 0, result, 0, load.length);
		}
		return result;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle state) {
		int plotCount = getServer().getConfig().getCpuCount();
		if (plotCount > 0) {
			View result = inflater.inflate(R.layout.fragment_cpu_load, root, false);
			LinearLayout plotViewHolder = (LinearLayout) result.findViewById(R.id.cpuLoadFragment_plotViewHolder);
			
			if (getModels() == null) {
				String titleFormat = getResources().getString(R.string.cpuLoad_plotTitle);
				ArrayList<PlotModel> models = new ArrayList<PlotModel>();
				for (int i = 0; i < plotCount; i++) {
					PlotModel m = new PlotModel();
					m.setTitle(String.format(titleFormat, i));
					models.add(m);
				}
				startManagingModels(models);
			}
			
			ArrayList<PlotView> plots = new ArrayList<PlotView>();
			for (int i = 0; i < plotCount; i++) {
				PlotView v = (PlotView) inflater.inflate(R.layout.fragment_cpu_load_plot, plotViewHolder, false)
						.findViewById(R.id.cpuUsage_plot);
				plotViewHolder.addView(v);
				plots.add(v);
			}
			startManagingPlots(plots);
			
			bindPlotsToModels();			
			startUpdatingUI(new CpuLoadRequestPacket());
			
			return result;
		}
		else {
			return inflater.inflate(R.layout.fragment_cpu_load_wrong_cpu_count, root, false);
		}
	}
	
	private static final String LOG_TAG = CpuLoadFragment.class.getSimpleName();
}
