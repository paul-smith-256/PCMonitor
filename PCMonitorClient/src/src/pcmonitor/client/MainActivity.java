package src.pcmonitor.client;

import src.pcmonitor.net.Server;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class MainActivity 
		extends ActionBarActivity
		implements ServerListFragment.SystemInfoLink {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Intent i = new Intent(this, SystemInfoActivity.class);
        // startActivity(i);
    }
    
    @Override
    public void showSystemInfo(Server server, SystemInfoGroup infoGroup) {
    	Intent intent = new Intent(this, SystemInfoActivity.class);
    	intent.putExtra(SystemInfoFragment.EXTRA_TAB_ID, infoGroup.ordinal());
    	intent.putExtra(SystemInfoFragment.EXTRA_SERVER, server);
    	startActivity(intent);
    }
}
