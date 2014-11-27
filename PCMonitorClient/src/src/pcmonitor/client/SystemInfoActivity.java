package src.pcmonitor.client;

import src.pcmonitor.net.Server;
import android.content.Intent;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

public class SystemInfoActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_system_info);
		Intent intent = getIntent();
		Server server = (Server) intent.getSerializableExtra(
				SystemInfoFragment.EXTRA_SERVER);
		
		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentByTag(FRAGMENT_TAG) == null) {
			fm.beginTransaction().replace(R.id.systemInfo_fragmentHolder, 
					SystemInfoFragment.newInstance(intent.getExtras()), FRAGMENT_TAG).commit();
		}
		
		ActionBar bar = getSupportActionBar();
		bar.setTitle(server.getIdentity().getName());
	}
	
	private static final String FRAGMENT_TAG = "systemInfo";
}
