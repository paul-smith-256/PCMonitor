package src.pcmonitor.client;

import java.util.ArrayList;

import src.pcmonitor.client.net.NetworkService;
import src.pcmonitor.client.net.ServiceAccessWrapper;
import src.pcmonitor.net.BaseRequestPacket;
import src.pcmonitor.net.BaseResponsePacket;
import src.pcmonitor.net.Server;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;

public abstract class PlotManagerFragment extends FragmentWithServerConnection {

	protected abstract void updateOtherUIComponents(BaseResponsePacket p);
	protected abstract float[] getPlotDataFromPacket(BaseResponsePacket p);
	
	@Override
	@SuppressWarnings("unchecked")
	public void onCreate(Bundle state) {
		super.onCreate(state);
		if (state != null) {
			mModels = (ArrayList<PlotModel>) state.getSerializable(EXTRA_MODELS);
			if (mModels != null) {
				mFirstPoint = mModels.get(0).getPointCount() == 0;
			}
			else {
				mFirstPoint = true;
			}
			mLastPointTimestamp = state.getLong(EXTRA_LAST_POINT_TIMESTAMP);
		}
		else {
			mFirstPoint = true;
		}
		mUpdater = new Handler();
		mLastPacketProcessed = true;
		mUpdatingUI = false;
	}
	
	@Override
	public void onDestroyView() {
		if (mPlots != null) {
			for (PlotView pv: mPlots) {
				pv.setModel(null);
			}
		}
		pauseUpdatingUI();
		super.onDestroyView();
	}
	
	@Override
	public void onDestroy() {
		pauseUpdatingUI();
		super.onDestroy();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mModels != null) {
			outState.putSerializable(EXTRA_MODELS, mModels);
			outState.putLong(EXTRA_LAST_POINT_TIMESTAMP, mLastPointTimestamp);
		}
	}
	
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (isVisibleToUser) {
			resumeUpdatingUI();
		} 
		else {
			pauseUpdatingUI();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		pauseUpdatingUI();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (getUserVisibleHint()) {
			resumeUpdatingUI();
		}
	}
	
	protected void bindPlotsToModels() {
		checkConfigured();
		if (mPlots.size() != mModels.size()) {
			throw new IllegalStateException("Model count isn't equal to plot count");
		}
		for (int i = 0; i < mPlots.size(); i++) {
			mPlots.get(i).setModel(mModels.get(i));
		}
	}
	
	protected void startManagingModels(ArrayList<PlotModel> models) {
		if (mUpdatingUI) {
			throw new IllegalStateException("Do not change model while UI is automatically updated");
		}
		mModels = models;
		mFirstPoint = mModels.get(0).getPointCount() == 0;
	}
	
	protected void startManagingPlots(ArrayList<PlotView> plots) {
		if (mUpdatingUI) {
			throw new IllegalStateException("Do not change plots while UI is automatically updated");
		}
		mPlots = plots;
	}
	
	protected void startUpdatingUI(BaseRequestPacket requestPacket) {
		mUpdatingUI = true;
		mUpdaterRunnable = new UpdaterRunnable(requestPacket);
		if (!getUserVisibleHint()) {
			return;
		}
		mUpdater.post(mUpdaterRunnable);
	}
	
	protected boolean isUpdatingUI() {
		return mUpdatingUI;
	}
	
	private void pauseUpdatingUI() {
		if (mUpdaterRunnable != null) {
			mUpdater.removeCallbacks(mUpdaterRunnable);
			mUpdatingUI = false;
		}
	}
	
	private void resumeUpdatingUI() {
		if (mUpdaterRunnable != null) {
			mUpdater.post(mUpdaterRunnable);
			mUpdatingUI = true;
		}
	}
	
	protected ArrayList<PlotModel> getModels() {
		return mModels;
	}
	
	private void checkConfigured() {
		if (mPlots == null || mModels == null) {
			throw new IllegalStateException("Set managed plots and models first");
		}
	}
	 
	private class UpdaterRunnable implements Runnable {
		
		public UpdaterRunnable(BaseRequestPacket p) {
			mRequestPacket = p;
		}
		
		@Override
		public void run() {
			if (isServiceReady() && mLastPacketProcessed) {
				NetworkService s = getService();
				s.send(getServer().getIdentity(), mRequestPacket, mReceiver);
				mLastPacketProcessed = false;
			}
			mUpdater.postDelayed(this, UPDATING_PERIOD);
		}
		
		private BaseRequestPacket mRequestPacket;
	}
	
	private Handler mReceiver = new Handler() {
		
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case NetworkService.RESULT_RESPONSE:
				BaseResponsePacket response = (BaseResponsePacket) m.getData()
						.getSerializable(NetworkService.EXTRA_RESPONSE_PACKET);
				
				if (mModels != null || mPlots != null) {
					float data[] = getPlotDataFromPacket(response);
					if (data == null) {
						return;
					}
					if (data.length != mModels.size()) {
						throw new IllegalArgumentException("Plot data size must be equal to model count");
					}
					
					if (!mFirstPoint && (response.getTimestamp() - mLastPointTimestamp) > 5 * UPDATING_PERIOD) {
						for (PlotModel pm: mModels) {
							pm.clear();
						}
						mFirstPoint = true;
					}
					
					if (mFirstPoint) {
						putStartingPoints(data);
						mFirstPoint = false;
						mLastPointTimestamp = response.getTimestamp();
					}
					else {
						if (putNextPoints(response.getTimestamp(), data)) {
							mLastPointTimestamp = response.getTimestamp();
						}
					}
					
				}
				
				updateOtherUIComponents(response);
				
				break;
			}
			mLastPacketProcessed = true;
		}
			
		private void putStartingPoints(float[] data) {
			for (int i = 0; i < mModels.size(); i++) {
				PlotModel m = mModels.get(i);
				float y = PlotModel.fitOrdinate(data[i], !m.isAutoscaled());
				m.putStartingPoint(y);
			}
		}
		
		private boolean putNextPoints(long timestamp, float[] data) {
			float dx = ((float) (timestamp - mLastPointTimestamp)) / TIME_SCALING_FACTOR;
			if (dx <= 0.0f) {
				Log.d(LOG_TAG, "Timestamp is before last point timestamp");
				return false;
			}
			for (int i = 0; i < mModels.size(); i++) {
				PlotModel m = mModels.get(i);
				float y = PlotModel.fitOrdinate(data[i], !m.isAutoscaled());
				m.putNextPoint(y, dx);
			}
			return true;
		}
	};
	
	private ArrayList<PlotModel> mModels;
	private ArrayList<PlotView> mPlots;
	private Handler mUpdater;
	private UpdaterRunnable mUpdaterRunnable;
	private boolean mUpdatingUI;
	private boolean mLastPacketProcessed;
	private boolean mFirstPoint;
	private long mLastPointTimestamp;
	
	private static final String EXTRA_MODELS = "models";
	private static final String EXTRA_LAST_POINT_TIMESTAMP = "lastPointTimestamp";
	private static final int UPDATING_PERIOD = 1000;
	private static final int TIME_SCALING_FACTOR = 100000;
	
	private static final String LOG_TAG = PlotManagerFragment.class.getSimpleName();
}
