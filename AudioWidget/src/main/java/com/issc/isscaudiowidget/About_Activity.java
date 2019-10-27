package com.issc.isscaudiowidget;


import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class About_Activity extends Activity {
	
	private boolean D = false;
	private static final String TAG = "About";
	
	ImageView headset_status = null;
	ImageView SPP_status = null;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_activity);
		headset_status = (ImageView) findViewById (R.id.about_status);
		SPP_status = (ImageView) findViewById (R.id.about_spp_status);
		
		if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
			headset_status.setImageResource(R.drawable.connect);
			SPP_status.setImageResource(R.drawable.datatransmission);
		} else {
			if ( ((Bluetooth_Conn) getApplication()).isHeadset() )
				headset_status.setImageResource(R.drawable.connect);
		}
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("Headset_Disconnect");
		intentFilter.addAction("Headset_Connect");
		intentFilter.addAction("SPP_disconnect");
		intentFilter.addAction("SPP_setup");
		registerReceiver(mBroadcast, intentFilter);
	}
	
	private BroadcastReceiver mBroadcast =  new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (arg1.getAction().equals("Headset_Connect")) {
				if (D) Log.i(TAG,"Receive: Headset_Connect");
        		if (headset_status != null) headset_status.setImageResource(R.drawable.connect);
        	} else if (arg1.getAction().equals("Headset_Disconnect")) {
        		if (D) Log.i(TAG,"Receive: Headset_Disconnect");
        		if (headset_status != null) headset_status.setImageResource(R.drawable.disconnect);
        	} else if (arg1.getAction().equals("SPP_setup")) {
        		if (D) Log.i(TAG,"Receive: SPP_Connect");
        		if(headset_status != null) headset_status.setImageResource(R.drawable.connect);
        		if(SPP_status != null) SPP_status.setImageResource(R.drawable.datatransmission);
        	} else if (arg1.getAction().equals("SPP_disconnect")) {
        		if (D) Log.i(TAG,"Receive Command ACK: SPP_disconnect");
        		if ( ((Bluetooth_Conn) getApplication()).isHeadset() == true ) {
        			if (headset_status != null) headset_status.setImageResource(R.drawable.connect);
        		} else {
        			if (headset_status != null) headset_status.setImageResource(R.drawable.disconnect);
        		}
        		if (SPP_status != null) SPP_status.setImageResource(R.drawable.nospp);
        	} 
		}
	};
	
	@Override
	protected void onStop() {
		super.onStop();
		if(D) Log.d(TAG,"[About] onStop call finish");
		finish();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(D) Log.d(TAG,"[About] onDestroy");
		unregisterReceiver(mBroadcast);
	}
}
