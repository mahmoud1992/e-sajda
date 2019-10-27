package com.issc.isscaudiowidget;

import com.issc.isscaudiowidget.R;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class IsscTtsDemo extends Activity {
	
	private boolean D = true;
	private static final String TAG = "TtsDemo";

	TextView main = null;
	Button btn_startVoicePrompt = null;
	Button btn_synthesizeTXT = null;
	EditText edit1 = null;
	ImageView headset_status = null;
	ImageView SPP_status = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ttsdemo);
		
		edit1 = (EditText) findViewById(R.id.ttsdemo_inputTXT);
		edit1.setText("ISSC Technologies Corp. is a Fab-less Bluetooth SoC design-house and " +
				"has committed to provide world class Bluetooth solutions. " +
				"With creativity in applications as well as sensitivity to consumers' requirements, " +
				"ISSC provides solutions that are economical, easy to manufacture and users friendly. ");
        
        Intent i = new Intent(this, CallerNameService.class);
        this.startService(i);
        
        btn_startVoicePrompt = (Button) findViewById (R.id.button1);
        btn_startVoicePrompt.setOnClickListener(btn_startVoicePromptListener);
        
        btn_synthesizeTXT = (Button) findViewById (R.id.button2);
        btn_synthesizeTXT.setOnClickListener(btn_synthesizeTXT_Listener);
        
        headset_status = (ImageView) findViewById (R.id.tts_status);
		SPP_status = (ImageView) findViewById (R.id.tts_spp_status);
		
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
		intentFilter.addAction("SPP_setup");
		intentFilter.addAction("SPP_disconnect");
		intentFilter.addAction("synthesizeSuccess");
		registerReceiver(mReceiver, intentFilter);
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action  = intent.getAction();
        	
        	if (action.equals("Headset_Connect")) {
				if (D) Log.i(TAG,"Receive: Headset_Connect");
        		if (headset_status != null) headset_status.setImageResource(R.drawable.connect);
        	} else if (action.equals("Headset_Disconnect")) {
        		if (D) Log.i(TAG,"Receive: Headset_Disconnect");
        		if (headset_status != null) headset_status.setImageResource(R.drawable.disconnect);
        	} else if (action.equals("SPP_setup")) {
        		if (D) Log.i(TAG,"Receive: SPP_Connect");
        		if(headset_status != null) headset_status.setImageResource(R.drawable.connect);
        		if(SPP_status != null) SPP_status.setImageResource(R.drawable.datatransmission);
        	} else if (action.equals("SPP_disconnect")) {
        		if (D) Log.i(TAG,"Receive Command ACK: SPP_disconnect");
        		if ( ((Bluetooth_Conn) getApplication()).isHeadset() == true ) {
        			if (headset_status != null) headset_status.setImageResource(R.drawable.connect);
        		} else {
        			if (headset_status != null) headset_status.setImageResource(R.drawable.disconnect);
        		}
        		if (SPP_status != null) SPP_status.setImageResource(R.drawable.nospp);
        	} else if (action.equals("synthesizeSuccess")) {
            	Toast.makeText(getBaseContext(), "Synthesize done", Toast.LENGTH_SHORT).show();
            }
        }
    };
    
    OnClickListener btn_startVoicePromptListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if ( !((Bluetooth_Conn) getApplication()).isSendVoicePromptThread() && 
				 !((Bluetooth_Conn) getApplication()).isSynthesizing() && 
				 ((Bluetooth_Conn) getApplication()).getSppStatus() &&
				 ((Bluetooth_Conn) getApplication()).isHasSD() ) 
			{
				((Bluetooth_Conn) getApplication()).startSendVoicePrompt();
			} else {
				if (D) Log.d("TTSdemo", "[Main] isSending or isTransferring");
			}
		}
    };
    
    OnClickListener btn_synthesizeTXT_Listener = new OnClickListener() {
    	@Override
    	public void onClick(View v) {
    		if ( !((Bluetooth_Conn) getApplication()).isSendVoicePromptThread() && 
    			 !((Bluetooth_Conn) getApplication()).isSynthesizing() && 
    			 ((Bluetooth_Conn) getApplication()).getSppStatus() &&
				 ((Bluetooth_Conn) getApplication()).isHasSD() ) 
    		{
    			if (edit1.length() > 0) {
		    		Intent i = new Intent();
		    		i.setAction("synthesizeTXT");
		    		i.putExtra("Text", edit1.getText().toString());
		    		sendBroadcast(i);
    			} else {
    				Toast.makeText(getBaseContext(), "Input text can't be null", Toast.LENGTH_SHORT).show();
    			}
    		} else {
				if (D) Log.d("TTSdemo", "[Main] isSending or isTransferring");
			}
    	}
    };
	
	@Override
	public void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}
}
