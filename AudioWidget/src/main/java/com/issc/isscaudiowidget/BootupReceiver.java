package com.issc.isscaudiowidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootupReceiver extends BroadcastReceiver{

	private boolean D = false;
	String TAG = "BootupReceiver";
	
	@Override
	public void onReceive(Context c, Intent intent) {
		if (D) Log.d(TAG,"[BootupReceiver] onReceive");
		if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
			Intent sIntent = new Intent(c, BootupService.class);
			c.startService(sIntent);
			Intent i = new Intent(c, CallerNameService.class);
	        c.startService(i);
		}
	}
}
