package com.issc.isscaudiowidget;

import java.util.List;

import com.issc.isscaudiowidget.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class BootupService extends Service {
    private static final boolean D = true;
	private static String TAG = "BootupService";
	
	private BluetoothAdapter adapter = null;
	private BluetoothHeadset Headset = null;
	private BluetoothDevice Device = null;
	private BluetoothA2dp a2dp = null;
	
	public static final String ACTION_GET_BATTERY_LEVEL ="GET_BATTERY_LEVEL";
	public static final String ACTION_REPLY_BATTERY_LEVEL ="REPLY_BATTERY_LEVEL";
	public static final String ACTION_ENABLE_TTS ="ENABLE_TTS";
	public CharSequence contentText = "";
	private SharedPreferences sharedPreferences;

	@Override
	public void onCreate(){
		if (D) Log.d(TAG,"onCreate");
		
		adapter = ((Bluetooth_Conn) getApplication()).getAdapter();
		adapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);
		adapter.getProfileProxy(this, mProfileListener, BluetoothProfile.A2DP);
		sharedPreferences = getSharedPreferences("com.issc.isscaudiowidget", 0);

		/* RD version */
		/*((Bluetooth_Conn) getApplication()).startSession();*/
		/* RD version */
	
		//register broadcast receiver
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
		intentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY+"."+Integer.toString(BluetoothAssignedNumbers.PLANTRONICS));
		intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
		intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
		intentFilter.addAction(ACTION_GET_BATTERY_LEVEL);
		intentFilter.addAction(ACTION_ENABLE_TTS);
		intentFilter.addAction("SPP_setup");
		registerReceiver(mReceiver, intentFilter);

	}
	
	private BluetoothProfile.ServiceListener mProfileListener = 
            new BluetoothProfile.ServiceListener() { 
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
        	
        	if (profile == BluetoothProfile.HEADSET) { 
        		if (D) Log.d(TAG,"on ProfileListener : HEADSET");
        		
                Headset = (BluetoothHeadset) proxy; 
                List<BluetoothDevice> connectedDevices = proxy.getConnectedDevices();
                
                for (BluetoothDevice device : connectedDevices) {
                	if (D) Log.d(TAG,"BluetoothDevice found :" + device);
                	Device = device;         	            	
                }
                
                int connectionState = Headset.getConnectionState(Device);
                if (D) Log.d(TAG,"connect state: "+connectionState);
                
                if (connectionState == BluetoothHeadset.STATE_CONNECTED) {
                	int bat;
                	SharedPreferences settings = getSharedPreferences("PREF_BATTERY", 0);
                	bat = settings.getInt("BATTERY_STAT", -1);
                	if (D) Log.d(TAG,"battery = "+bat);
            		batterynotify(bat, false);
            		((Bluetooth_Conn) getApplication()).setHeadset(true);
            		((Bluetooth_Conn) getApplication()).SetSpp(Device);
            		
            		/* RD version */
            		//((Bluetooth_Conn) getApplication()).set_HSP_device(Device);
            		/* RD version */
            	} else if (connectionState == BluetoothHeadset.STATE_DISCONNECTED) {
            		/* RD version */
            		//((Bluetooth_Conn) getApplication()).set_HSP_device(null);
            		/* RD version */
            		SharedPreferences settings = getSharedPreferences ("PREF_BATTERY", 0);
            	    SharedPreferences.Editor editor = settings.edit();
            	    editor.putInt("BATTERY_STAT", -1);
            	    editor.commit();
            		batterynotify(-1, true);
            		if (a2dp != null) {
            			if (D) Log.v(TAG,"a2dp profile: "+a2dp.getConnectionState(Device));
            			if ( a2dp.getConnectionState(Device) != BluetoothA2dp.STATE_CONNECTED ) {
            				((Bluetooth_Conn) getApplication()).setHeadset(false);
            				Intent i = new Intent();
                    		i.setAction("Headset_Disconnect");
                    		sendBroadcast(i);
            				if (Device != null) 
            					((Bluetooth_Conn) getApplication()).resetConnection(Device.getAddress());
            				/* Add TTS demo */
            				if ( ((Bluetooth_Conn) getApplication()).isSendVoicePromptThread() )
            					((Bluetooth_Conn) getApplication()).stopSendVoicePrompt();
            			}
            		} else {
            			if (D) Log.v(TAG,"a2dp null when headset disconnect");
            			((Bluetooth_Conn) getApplication()).setHeadset(false);
        				Intent i = new Intent();
                		i.setAction("Headset_Disconnect");
                		sendBroadcast(i);
        				if (Device != null) 
        					((Bluetooth_Conn) getApplication()).resetConnection(Device.getAddress());
        				/* Add TTS demo */
        				if ( ((Bluetooth_Conn) getApplication()).isSendVoicePromptThread() )
        					((Bluetooth_Conn) getApplication()).stopSendVoicePrompt();
            		}
    			}
            }
        	
        	if (profile == BluetoothProfile.A2DP) {
        		if (D) Log.d(TAG,"on ProfileListener : A2DP");
        		a2dp = (BluetoothA2dp) proxy;
        		List<BluetoothDevice> connectedDevices = proxy.getConnectedDevices();
                
                for (BluetoothDevice device : connectedDevices) {
                	if (D) Log.d(TAG,"BluetoothDevice found :" + device);
                	Device = device;         	            	
                }
                
                int connectionState = a2dp.getConnectionState(Device);
                if (D) Log.d(TAG,"connect state: "+connectionState);
                
                if (connectionState == BluetoothHeadset.STATE_CONNECTED){
                	((Bluetooth_Conn) getApplication()).setHeadset(true);
            		((Bluetooth_Conn) getApplication()).SetSpp(Device);
                	
                	/* RD version */
                	//((Bluetooth_Conn) getApplication()).set_A2DP_device(Device);
            		/* RD version */
            	} else if (connectionState == BluetoothA2dp.STATE_DISCONNECTED){
            		/* RD version */
            		//((Bluetooth_Conn) getApplication()).set_A2DP_device(null);
            		/* RD version */
            		if (Headset != null) {
            			if (D) Log.v(TAG,"headset profile: "+Headset.getConnectionState(Device));
            			if ( Headset.getConnectionState(Device) != BluetoothHeadset.STATE_CONNECTED ) {
            				((Bluetooth_Conn) getApplication()).setHeadset(false);
            				Intent i = new Intent();
                    		i.setAction("Headset_Disconnect");
                    		sendBroadcast(i);
            				if (Device != null)
            					((Bluetooth_Conn) getApplication()).resetConnection(Device.getAddress());
            				/* Add TTS demo */
            				if ( ((Bluetooth_Conn) getApplication()).isSendVoicePromptThread() )
            					((Bluetooth_Conn) getApplication()).stopSendVoicePrompt();
            			} 
            		} else {
            			if (D) Log.v(TAG,"headset null when a2dp disconnect");
            			((Bluetooth_Conn) getApplication()).setHeadset(false);
            			Intent i = new Intent();
                    	i.setAction("Headset_Disconnect");
                    	sendBroadcast(i);
            			if (Device != null) 
            				((Bluetooth_Conn) getApplication()).resetConnection(Device.getAddress());
            			/* Add TTS demo */
        				if ( ((Bluetooth_Conn) getApplication()).isSendVoicePromptThread() )
        					((Bluetooth_Conn) getApplication()).stopSendVoicePrompt();
            		}
    			}
        	}
        } 
        
        public void onServiceDisconnected(int profile) { 
            if (profile == BluetoothProfile.HEADSET) { 
            	if (D) Log.i(TAG,"headset null");
                Headset = null; 
            } 
            if (profile == BluetoothProfile.A2DP) { 
            	if (D) Log.i(TAG,"a2dp null");
                a2dp = null; 
            } 
        } 
    };

    /**
     * The receiver for 1. battery status from headset 2. battery status request from main screen. 
     * 3. connection status changed. 
     */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action  = intent.getAction();
        	if (D) Log.d(TAG,"Receive broadcast: "+action);
            
        	if (action.equals(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)){
        		if (D) Log.d(TAG,"ACTION_VENDOR_SPECIFIC_HEADSET_EVENT");
            	Bundle extras = intent.getExtras();

            	Object args[] = (Object [])extras.get(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS);
            	
            	if (args[0].equals("BATTERY") && args[1].hashCode() <= 9 && args[1].hashCode() >= 0){
            		int count = args[1].hashCode();
            		if (D) Log.d(TAG, "" + count);
            		
            		//Save battery status to SharedPreferences
            		SharedPreferences settings = getSharedPreferences ("PREF_BATTERY", 0);
            	    SharedPreferences.Editor editor = settings.edit();
            	    editor.putInt("BATTERY_STAT", count);
            	    editor.commit();
            		
            		batterynotify(count, false);
            		
            		//notify activity
            		Intent i = new Intent(BootupService.ACTION_REPLY_BATTERY_LEVEL);
                	i.putExtra("LEVEL", contentText);
                	sendBroadcast(i);
            	}         	
            	
            //reply to main screen to show battery status
            } else if (action.equals(BootupService.ACTION_GET_BATTERY_LEVEL)){
            	if (D) Log.d(TAG,"ACTION_GET_BATTERY_LEVEL");
            	Intent i = new Intent(BootupService.ACTION_REPLY_BATTERY_LEVEL);
            	i.putExtra("LEVEL", contentText);
            	sendBroadcast(i);
            	
            } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)){
            	int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,456);
            	BluetoothDevice Dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            	
            	if (state == BluetoothHeadset.STATE_DISCONNECTED) {
            		if (D) Log.d(TAG,"Headset disconnected");
            		Intent i = new Intent();
            		i.setAction("Headset_Disconnect");
            		sendBroadcast(i);
            		Device = Dev;
            		adapter.getProfileProxy(BootupService.this, mProfileListener, BluetoothProfile.HEADSET);
            	} else if (state == BluetoothHeadset.STATE_CONNECTING) {
            		if (D) Log.d(TAG,"Headset connecting");
            	} else if (state == BluetoothHeadset.STATE_CONNECTED) {
            		if (D) Log.d(TAG,"Headset connected");
            		Device = Dev;
            		adapter.getProfileProxy(BootupService.this, mProfileListener, BluetoothProfile.HEADSET);
            	} else if (state == BluetoothHeadset.STATE_DISCONNECTING) {    
            		if (D) Log.d(TAG,"Headset disconnecting");
            	}
            } else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)){
            	int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,123);
            	BluetoothDevice Dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            	
            	if (state == BluetoothA2dp.STATE_DISCONNECTED) {
            		if (D) Log.d(TAG,"A2dp disconnected");
            		Device = Dev;
            		adapter.getProfileProxy(BootupService.this, mProfileListener, BluetoothProfile.A2DP);
            	} else if (state == BluetoothA2dp.STATE_CONNECTING) {
            		if (D) Log.d(TAG,"A2dp connecting");
            	} else if (state == BluetoothA2dp.STATE_CONNECTED) {
            		if (D) Log.d(TAG,"A2dp connected");
            		Device = Dev;
            		adapter.getProfileProxy(BootupService.this, mProfileListener, BluetoothProfile.A2DP);
            	} else if (state == BluetoothA2dp.STATE_DISCONNECTING) {    
            		if (D) Log.d(TAG,"A2dp disconnecting");
            	}
            } else if (action.equals("SPP_setup")) {
            	/* Event Mask Setting, close unused event */
            	byte [] buffer = new byte [9];
            	buffer[0] = (byte) 0xaa;  buffer[1] =  0x00;
            	buffer[2] =  0x05;        buffer[3] =  0x03;
            	buffer[4] = (byte) 0xff;  buffer[5] = (byte) 0xff;
            	buffer[6] = (byte) 0xfe;  buffer[7] = (byte) 0xff;
            	buffer[8] = (byte) 0xfd;
            	((Bluetooth_Conn) getApplication()).write(buffer);
            	
            	/* Add delay between commands */
            	try {
    				Thread.sleep(30);
				} catch (InterruptedException e) {
					Log.e(TAG,"Add dalay between two commands, exception", e);
				}
            	
        		if (sharedPreferences.getBoolean("SendTTS", false)) {
                   	/* Notify BTM TTS feature, for BT5502 */
                	byte [] buffer2 = new byte [7];
                	buffer2[0] = (byte) 0xaa;  buffer2[1] = (byte) 0x00;
                	buffer2[2] = (byte) 0x03;  buffer2[3] = (byte) 0x13;
                	buffer2[4] = (byte) 0x05;  buffer2[5] = (byte) 0x01;
                	buffer2[6] = (byte) 0xe4;
                	((Bluetooth_Conn) getApplication()).write(buffer2);
				}
        		else {
                	byte [] buffer2 = new byte [7];
                	buffer2[0] = (byte) 0xaa;  buffer2[1] = (byte) 0x00;
                	buffer2[2] = (byte) 0x03;  buffer2[3] = (byte) 0x13;
                	buffer2[4] = (byte) 0x05;  buffer2[5] = (byte) 0x00;
                	buffer2[6] = (byte) 0xe5;
                	((Bluetooth_Conn) getApplication()).write(buffer2);
				}
             }
            else if (action.equals(BootupService.ACTION_ENABLE_TTS)){
            	if (D) Log.d(TAG,"ACTION_ENABLE_TTS");
            	if (((Bluetooth_Conn) getApplication()).isHeadset()) {
              		if (sharedPreferences.getBoolean("SendTTS", false)) {
                       	/* Notify BTM TTS feature, for BT5502 */
                    	byte [] buffer2 = new byte [7];
                    	buffer2[0] = (byte) 0xaa;  buffer2[1] = (byte) 0x00;
                    	buffer2[2] = (byte) 0x03;  buffer2[3] = (byte) 0x13;
                    	buffer2[4] = (byte) 0x05;  buffer2[5] = (byte) 0x01;
                    	buffer2[6] = (byte) 0xe4;
                    	((Bluetooth_Conn) getApplication()).write(buffer2);
    				}
            		else {
                    	byte [] buffer2 = new byte [7];
                    	buffer2[0] = (byte) 0xaa;  buffer2[1] = (byte) 0x00;
                    	buffer2[2] = (byte) 0x03;  buffer2[3] = (byte) 0x13;
                    	buffer2[4] = (byte) 0x05;  buffer2[5] = (byte) 0x00;
                    	buffer2[6] = (byte) 0xe5;
                    	((Bluetooth_Conn) getApplication()).write(buffer2);
    				}
				}
            }
        }
    };
    

    /**
     * to indicate the battery level, the flag close means close notification or not 
     */
    private void batterynotify(int level, boolean close) {
    	String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		int icon = -1;
		
		if (level >= 7) {
			icon = R.drawable.battery9;
		} else if(level >= 4) {
			icon = R.drawable.battery6;
		} else if(level >= 1) {
			icon = R.drawable.battery3;
		} else if(level == 0) {
			icon = R.drawable.battery1;
		} else {
			//icon = R.drawable.battery0;
		}
		
		level = (level+1)*10;
		contentText = ""+level+"%";
		CharSequence tickerText = "Battery Level";
		long when = System.currentTimeMillis();

		Notification.Builder builder = new Notification.Builder(getBaseContext());
    	CharSequence contentTitle = "Battery Level Indication";
    	
    	Notification notification;
    	
    	Intent notificationIntent = new Intent();
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    	//notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
    	if (icon >= 0) {
    		builder
    		.setSmallIcon(icon)
    		.setContentTitle(contentTitle)
    		.setContentText(contentText)
    		.setTicker(tickerText)
    		.setWhen(when)
    		.setContentIntent(contentIntent);
    	}
    	
    	notification = builder.getNotification();
		notification.flags = Notification.FLAG_NO_CLEAR;
    	
    	if (close) {
    		mNotificationManager.cancel(1);
    	} else {
    		mNotificationManager.notify(1, notification);
    	}
    	
    	//update battery status to main screen
    	Intent i = new Intent(BootupService.ACTION_REPLY_BATTERY_LEVEL);
    	i.putExtra("LEVEL", contentText);
    	sendBroadcast(i);
    }
 
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}