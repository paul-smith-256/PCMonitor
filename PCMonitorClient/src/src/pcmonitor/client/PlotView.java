package src.pcmonitor.client;

import java.util.Iterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PlotView 
		extends View 
		implements PlotModel.PlotModelEventListener {
	
	public PlotView(Context c, AttributeSet attrs) {
		super(c, attrs);
		initialize(new PlotModel());
	}
	
	public PlotView(Context c, PlotModel model) {
		super(c);
		initialize(model);
	}
	
	public PlotView(Context c) {
		 this(c, new PlotModel());
	}
	
	private void initialize(PlotModel model) {
		mModel = model;
		if (model != null) {
			mModel.addEventListener(this);
		}
		
		setBackgroundColor(getResources().getColor(R.color.transparentBackground));
		
		mRect = new Rect();
		
		mGridPaint = new Paint();
		mGridPaint.setColor(getResources().getColor(R.color.plotView_gridColor));
		mGridPaint.setAntiAlias(true);
		
		mPlotPaint = new Paint();
		mPlotPaint.setColor(getResources().getColor(R.color.plotView_plotColor));
		mPlotPaint.setAntiAlias(true);
		mPlotPaint.setStyle(Paint.Style.STROKE);
		
		mTextPaint = new Paint();
		mTextPaint.setColor(Color.BLACK);
		mTextPaint.setTextSize(20);
		mTextPaint.setAntiAlias(true);
		
		mPlot = new Path();
	}
	
	public PlotModel getModel() {
		return mModel;
	}
	
	public void setModel(PlotModel model) {
		if (mModel != null) {
			mModel.removeEventListener(this);
		}
		mModel = model;
		if (mModel != null) {
			mModel.addEventListener(this);
			onModelChanged();
		}
		else {
			invalidate();
		}
	}
	
	@Override
	public void onModelChanged() {
		// makePath();
		invalidate();
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		drawGrid(canvas);
		
		if (mModel != null) {
			Matrix m = new Matrix();
			m.preTranslate(0, getHeight());
			m.preScale(getWidth(), -getHeight());
			
			makePath();
			mPlot.transform(m);
			canvas.drawPath(mPlot, mPlotPaint);
			
			drawUnits(canvas);
			drawPlotTitle(canvas);
		}
	}
	
	private void drawPlotTitle(Canvas c) {
		String text = mModel.getTitle();
		if (text != null) {
			mTextPaint.getTextBounds(text, 0, text.length() - 1, mRect);
			c.drawText(text, TEXT_BORDER, mRect.height() + TEXT_BORDER, mTextPaint);
		}
	}
	
	private void drawUnits(Canvas c) {
		String text = mModel.getOrdinateUnits();
		if (text != null) {
			c.drawText(text, TEXT_BORDER, getHeight() - TEXT_BORDER, mTextPaint);
		}
	}
	
	private void drawGrid(Canvas c) {
		int w = getWidth();
		int h = getHeight();
		int gridLineX = GRID_LINE_COUNT, gridLineY = GRID_LINE_COUNT;
		if (w < h) {
			gridLineY *= (h / w);
		}
		else if (w > h) {
			gridLineX *= (w / h);
		}
		float stepX = w / gridLineX;
		for (int i = 1; i < gridLineX; i++) {
			c.drawLine(i * stepX, 0, i * stepX, h, mGridPaint);
		}
		float stepY = h / gridLineY;
		for (int i = 1; i < gridLineY; i++) {
			c.drawLine(0, i * stepY, w, i * stepY, mGridPaint);
		}
		c.drawLine(0.0f, 0.0f, 1.0f, 1.0f, mGridPaint);
	}
	
	private void makePath() {
		mPlot.reset();
		if (mModel.getPointCount() < 1) {
			return;
		}
		Iterator<Point> points = mModel.getPoints();
		Point p = points.next();
		mPlot.moveTo(p.x, p.y);
		while (points.hasNext()) {
			p = points.next();
			mPlot.lineTo(p.x, p.y);
		}
	}
	
	private PlotModel mModel;
	private Paint mGridPaint;
	private Paint mPlotPaint;
	private Paint mTextPaint;
	private Path mPlot;
	private Rect mRect;
	
	private static final int TEXT_BORDER = 5;
	private static final int GRID_LINE_COUNT = 5;
	
	private static final String LOG_TAG = PlotView.class.getSimpleName();
}
