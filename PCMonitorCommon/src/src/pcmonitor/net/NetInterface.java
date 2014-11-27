package src.pcmonitor.net;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.LinkedList;

public class NetInterface implements Serializable {
	
	public NetInterface(String name, String descritpion, String address,
			String netmask, String hwAddress, String type, long mtu, long metric) {
		super();
		mName = name;
		mDescritpion = descritpion;
		mAddress = address;
		mNetmask = netmask;
		mHwAddress = hwAddress;
		mType = type;
		mMtu = mtu;
		mMetric = metric;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	public String getDescritpion() {
		return mDescritpion;
	}

	public void setDescritpion(String descritpion) {
		mDescritpion = descritpion;
	}

	public String getAddress() {
		return mAddress;
	}

	public void setAddress(String address) {
		mAddress = address;
	}

	public String getNetmask() {
		return mNetmask;
	}

	public void setNetmask(String netmask) {
		mNetmask = netmask;
	}

	public String getHwAddress() {
		return mHwAddress;
	}

	public void setHwAddress(String hwAddress) {
		mHwAddress = hwAddress;
	}

	public String getType() {
		return mType;
	}

	public void setType(String type) {
		mType = type;
	}

	public long getMtu() {
		return mMtu;
	}

	public void setMtu(long mtu) {
		mMtu = mtu;
	}

	public long getMetric() {
		return mMetric;
	}

	public void setMetric(long metric) {
		mMetric = metric;
	}

	private String mName;
	private String mDescritpion;
	private String mAddress;
	private String mNetmask;
	private String mHwAddress;
	private String mType;
	private long mMtu;
	private long mMetric;
	
	private static final long serialVersionUID = 1;
}
