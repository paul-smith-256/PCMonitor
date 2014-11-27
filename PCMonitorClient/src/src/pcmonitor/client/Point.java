package src.pcmonitor.client;

import java.io.Serializable;

public class Point implements Serializable {
	
	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public float x, y;
	
	private static final long serialVersionUID = 1;
}
