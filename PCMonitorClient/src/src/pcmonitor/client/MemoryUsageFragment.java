package src.pcmonitor.client;

import static java.lang.String.format;

import java.util.ArrayList;

import src.pcmonitor.net.BaseResponsePacket;
import src.pcmonitor.net.MemUsageRequestPacket;
import src.pcmonitor.net.MemUsageResponsePacket;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.InputDevice.MotionRange;
import android.widget.TextView;

public class MemoryUsageFragment extends PlotManagerFragment {
	
	public static MemoryUsageFragment newInstance(Bundle b) {
		MemoryUsageFragment result = new MemoryUsageFragment();
		result.setArguments(b);
		return result;
	}
	
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		if (getModels() == null) {
			ArrayList<PlotModel> models = new ArrayList<PlotModel>();
			Resources r = getResources();
			
			PlotModel ramUsageModel = new PlotModel();
			ramUsageModel.setAutoscaled(true);
			ramUsageModel.setTitle(r.getString(R.string.memoryUsage_ramPlotTitle));
			models.add(ramUsageModel);
			
			PlotModel swapUsageModel = new PlotModel();
			swapUsageModel.setTitle(r.getString(R.string.memoryUsage_swapPlotTitle));
			swapUsageModel.setAutoscaled(true);
			swapUsageModel.setOrdinateUnitName("B");
			models.add(swapUsageModel);
			
			startManagingModels(models);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup root,
			Bundle savedInstanceState) {
		View result = inflater.inflate(R.layout.fragment_memory_usage, root, false);
		
		ArrayList<PlotView> plots = new ArrayList<PlotView>();
		plots.add((PlotView) result.findViewById(R.id.memoryUsage_ramUsagePlot));
		plots.add((PlotView) result.findViewById(R.id.memoryUsage_swapUsagePlot));
		startManagingPlots(plots);
		bindPlotsToModels();
		
		TextView physicalRamText = (TextView) result.findViewById(R.id.memoryUsage_physicalRamText);
		physicalRamText.setText(
				format(getResources().getString(R.string.memoryUsage_physicalRam), 
						getServer().getConfig().getRamSize()));
		
		mTotalRamText = (TextView) result.findViewById(R.id.memoryUsage_totalText);
		mFreeRamText = (TextView) result.findViewById(R.id.memoryUsage_freeText);
		mUsedRamText = (TextView) result.findViewById(R.id.memoryUsage_usedText);
		mFreeSwapText = (TextView) result.findViewById(R.id.memoryUsage_swapFreeText);
		mUsedSwapText = (TextView) result.findViewById(R.id.memoryUsage_swapUsedText);
		
		startUpdatingUI(new MemUsageRequestPacket());
		return result;
	}
	
	@Override
	protected float[] getPlotDataFromPacket(BaseResponsePacket p) {
		if (!(p instanceof MemUsageResponsePacket)) {
			return null;
		}
		MemUsageResponsePacket response = (MemUsageResponsePacket) p;
		return new float[] {((float) response.getUsed()) / (response.getTotal()), response.getSwapUsed()};
	}
	
	@Override
	protected void updateOtherUIComponents(BaseResponsePacket p) {
		if (!(p instanceof MemUsageResponsePacket)) {
			return;
		}
		MemUsageResponsePacket response = (MemUsageResponsePacket) p;
		Resources r = getResources();
		String free = r.getString(R.string.memoryUsage_free);
		String used = r.getString(R.string.memoryUsage_used);
		String total = r.getString(R.string.memoryUsage_total);
		
		mTotalRamText.setText(format(total, toMB(response.getTotal())));
		mFreeRamText.setText(format(free, toMB(response.getFree())));
		mUsedRamText.setText(format(used, toMB(response.getUsed())));
		
		mFreeSwapText.setText(format(free, toMB(response.getSwapFree())));
		mUsedSwapText.setText(format(used, toMB(response.getSwapUsed())));
	}
	
	private long toMB(long v) {
		return v / (1024 * 1024);
	}
	
	private TextView mTotalRamText, mUsedRamText, mFreeRamText,
			mUsedSwapText, mFreeSwapText;
}
