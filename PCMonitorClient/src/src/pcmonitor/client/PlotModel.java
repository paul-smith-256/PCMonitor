package src.pcmonitor.client;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PlotModel implements Serializable {
	
	public PlotModel() {
		mPoints = new LinkedList<Point>();
		mEventListeners = new LinkedList<PlotModelEventListener>();
	}
	
	public boolean isAutoscaled() {
		return mAutoscale;
	}

	public void setAutoscaled(boolean autoscale) {
		mAutoscale = autoscale;
		if (mAutoscale) {
			findMaxOrdinate();
			fireModelChangedEvent();
		}
	}
	
	public int getPointCount() {
		return mPoints.size();
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String name) {
		mTitle = name;
		fireModelChangedEvent();
	}

	public String getOrdinateUnitName() {
		return mOrdinateUnitName;
	}

	public void setOrdinateUnitName(String ordinateUnitName) {
		mOrdinateUnitName = ordinateUnitName;
		if (mAutoscale) {
			updateOrdinateUnits();
		}
		fireModelChangedEvent();
	}
	
	public String getOrdinateUnits() {
		return mAutoscale ? mOrdinateUnits : mOrdinateUnitName;
	}
	
	public static interface PlotModelEventListener {
		public void onModelChanged();
	}

	public void addEventListener(PlotModelEventListener listener) {
		getListeners().add(listener);
	}
	
	public void removeEventListener(PlotModelEventListener listener) {
		getListeners().remove(listener);
	}
	
	private void fireModelChangedEvent() {
		for (PlotModelEventListener el: getListeners()) {
			el.onModelChanged();
		}
	}
	
	private LinkedList<PlotModelEventListener> getListeners() {
		if (mEventListeners == null) {
			mEventListeners = new LinkedList<PlotModelEventListener>();
		}
		return mEventListeners;
	}
	
	public Iterator<Point> getPoints() {
		if (mAutoscale) {
			return new ScaledPointsIterator(mPoints, mMaxOrdinate);
		}
		else {
			return mPoints.iterator();
		}
	}
	
	private static class ScaledPointsIterator implements Iterator<Point> {
		
		public ScaledPointsIterator(List<Point> source, float maxOrdinate) {
			mSource = source.iterator();
			mScalingFactor = (float) Math.pow(10.0f, findPositiveExponent(maxOrdinate));
		}
		
		@Override
		public boolean hasNext() {
			return mSource.hasNext();
		}
		
		@Override
		public Point next() {
			Point p = mSource.next();
			return new Point(p.x, p.y / mScalingFactor);
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		private Iterator<Point> mSource;
		private float mScalingFactor;
	}
	
	public static float fitOrdinate(float y, boolean toUnity) {
		if (y > 1.0f && toUnity) {
			return 1.0f;
		}
		if (y < 0.0f) {
			return 0.0f;
		}
		return y;
	}

	public void putStartingPoint(float y) {
		if (!mPoints.isEmpty()) {
			throw new IllegalStateException("Starting point already set");
		}
		if (y < MIN_Y) {
			throw new IllegalArgumentException("Negative ordinate");
		}
		if (mAutoscale) {
			updateMaxOrdinate(y);
		}
		mPoints.add(new Point(MIN_X, y));
		fireModelChangedEvent();
	}
	
	public void putNextPoint(float y, float dx) {
		if (mPoints.isEmpty()) {
			throw new IllegalStateException("Starting point is not set");
		}
		if (y < MIN_Y || dx <= 0.0f) {
			throw new IllegalArgumentException("Invalid coordinate: y = " + y + ", dx = " + dx);
		}
		Point lastPoint = mPoints.get(mPoints.size() - 1);
		float nextX = lastPoint.x + dx;
		if (mAutoscale && y > mMaxOrdinate) {
			updateMaxOrdinate(y);
		}
		if (nextX > MAX_X) {
			shiftPlotLeft(nextX - MAX_X);
			removeInvisiblePoints();
			if (mAutoscale) {
				findMaxOrdinate();
			}
			nextX = MAX_X;
		}
		mPoints.add(new Point(nextX, y));
		fireModelChangedEvent();
	}
	
	public void clear() {
		mPoints.clear();
		updateMaxOrdinate(DEFAULT_MAX_ORDINATE);
		fireModelChangedEvent();
	}
	
	private void findMaxOrdinate() {
		float result = DEFAULT_MAX_ORDINATE;
		for (Point p: mPoints) {
			if (result < p.y) {
				result = p.y;
			}
		}
		updateMaxOrdinate(result);
	}
	
	private void shiftPlotLeft(float offset) {
		for (Point p: mPoints) {
			p.x -= offset;
		}
	}
	
	private void removeInvisiblePoints() {
		while (true) {
			if (mPoints.size() < 2) {
				break;
			}
			Point firstPoint = mPoints.get(0);
			if (firstPoint.x > 0) {
				break;
			}
			Point secondPoint = mPoints.get(1);
			if (firstPoint.x < MIN_X && secondPoint.x > 0) {
				break;
			}
			else {
				mPoints.remove(0);
			}
		}
	}
	
	private void updateOrdinateUnits() {
		if (mOrdinateUnitName == null) {
			return;
		}
		String result = mOrdinateUnitName;
		int exponent = findPositiveExponent(mMaxOrdinate);
		if (exponent > 0) {
			int d = exponent / 3 - 1;
			if (d < 0) {
				result = pow10(exponent) + result;
			} else if (d >= 0 && d < sUnitPrefixes.length) {
				result = sUnitPrefixes[d] + result;
				int r = exponent % 3;
				if (r != 0) {
					result = pow10(r) + result;
				}
			}
			else {
				result = "10^" + exponent;
			}
		}
		mOrdinateUnits = result;
	}
	
	private void updateMaxOrdinate(float o) {
		mMaxOrdinate = Math.max(o, DEFAULT_MAX_ORDINATE);
		updateOrdinateUnits();
	}
	
	private int pow10(int p) {
		int result = 1;
		for (int i = 0; i < p; i++) {
			result *= 10;
		}
		return result;
	}
	
	private static int findPositiveExponent(float value) {
		int result = 0;
		while (value > 1.0f) {
			value /= 10;
			result += 1;
		}
		return result;
	}
	
	private LinkedList<Point> mPoints;	
	private boolean mAutoscale;	
	private String mTitle;
	private float mMaxOrdinate;
	private String mOrdinateUnitName;
	private String mOrdinateUnits;
	private transient LinkedList<PlotModelEventListener> mEventListeners;
	
	private static final String[] sUnitPrefixes = new String[] {"k", "M", "G", "T"};
	private static final float DEFAULT_MAX_ORDINATE = 1.0f;
	private static final float MAX_X = 1.0f;
	private static final float MIN_X = 0.0f;
	private static final float MIN_Y = 0.0f;
	
	private static final long serialVersionUID = 1;
}
