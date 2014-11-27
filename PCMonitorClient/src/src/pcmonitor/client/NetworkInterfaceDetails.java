package src.pcmonitor.client;

import src.pcmonitor.net.NetInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import static java.lang.String.format;

public class NetworkInterfaceDetails extends DialogFragment {
	
	public static NetworkInterfaceDetails newInstance(NetInterface iface) {
		Bundle b = new Bundle();
		b.putSerializable(IFACE_EXTRA, iface);
		NetworkInterfaceDetails result = new NetworkInterfaceDetails();
		result.setArguments(b);
		return result;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		NetInterface iface = (NetInterface) getArguments().getSerializable(IFACE_EXTRA);
		
		View result = inflater.inflate(R.layout.fragment_network_interface_details, container, false);
		TextView hwAddressView = (TextView) result.findViewById(R.id.networkInterfaceDetails_hwAddress);
		TextView typeView = (TextView) result.findViewById(R.id.networkInterfaceDetails_type);
		TextView mtuView = (TextView) result.findViewById(R.id.networkInterfaceDetails_mtu);
		TextView metricView = (TextView) result.findViewById(R.id.networkInterfaceDetails_metric);
		
		Resources r = getResources();
		String hwAddressFmt = r.getString(R.string.networkInterfaceDetails_hwAddressFormat);
		String typeFmt = r.getString(R.string.networkInterfaceDetails_typeFormat);
		String mtuFmt = r.getString(R.string.networkInterfaceDetails_mtuFormat);
		String metricFmt = r.getString(R.string.networkInterfaceDetails_metricFormat);
		
		hwAddressView.setText(format(hwAddressFmt, iface.getHwAddress()));
		typeView.setText(format(typeFmt, iface.getType()));
		mtuView.setText(format(mtuFmt, iface.getMtu()));
		metricView.setText(format(metricFmt, iface.getMetric()));
		
		getDialog().setCanceledOnTouchOutside(true);
		getDialog().setTitle(iface.getName());
		
		return result;
	}
	
	private static final String IFACE_EXTRA = "iface";
}
