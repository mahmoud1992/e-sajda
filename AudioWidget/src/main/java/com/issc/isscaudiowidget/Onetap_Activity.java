package com.issc.isscaudiowidget;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;

public class Onetap_Activity extends Activity {
	
	private boolean D = true;
	private static final String TAG = "OneTap";
	
	ImageView headset_status = null;
	// 
    private static final ParcelUuid AudioSource = ParcelUuid.fromString("0000110a-0000-1000-8000-00805f9b34fb");
	private static final ParcelUuid AudioSink = ParcelUuid.fromString("0000110b-0000-1000-8000-00805f9b34fb");
	private static final ParcelUuid HeadsetService = ParcelUuid.fromString("00001108-0000-1000-8000-00805f9b34fb");
	private static final ParcelUuid Handsfree = ParcelUuid.fromString("0000111e-0000-1000-8000-00805f9b34fb");
	public static String ACTION_STATUS_PROCESSING ="STATUS_PROCESSING";
	public static String ACTION_STATUS_FINISH ="STATUS_FINISH";
	
	// Configuration of recognizing statement
	private static final int recognize_by_RSSI     = 1;
	private static final int recognize_by_Name     = 2;
	private static final int recognize_by_Address  = 3;
	private static final int recognize_by_First    = 4;
	private int recognizing_statement = recognize_by_First;
	private static String supported_Name = "Blue SPP test";
	private static String supported_Addr = "AA:BB:CC";
	
    
    // Member fields
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothHeadset headset = null;
    private BluetoothA2dp a2dp = null;
    private final int REQUEST_ENABLE_BT = 1;
    private ProgressDialog pdialog = null;
    private ArrayList<String> DeviceList;    
    
    //
    private boolean discovery_finish = false;
    private boolean has_a2dp = false;
    private boolean has_hsp = false;
    private boolean is_connecting;
    private String bd_addr;
    private int rssi_index = 0;
    private int rssi_value = -1000;
	
    private Handler mHandler = null;
    private Runnable mRunnable = null;
    
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_isscaudiowidget);
		
		discovery_finish = false;
		
		// Register for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
	    filter.addAction(ACTION_STATUS_PROCESSING);
	    filter.addAction(ACTION_STATUS_FINISH);
	    this.registerReceiver(mReceiver, filter);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
	    if (mBluetoothAdapter == null) {
	        // Device does not support Bluetooth
	    	CustomToast.showToast(getBaseContext(), "No Bluetooth on this device", 2500);
	        finish();
	        return;
	    }     

	    if (!mBluetoothAdapter.isEnabled()) {
	        //Enable Bluetooth if it is disabled
	        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);                                                
	    } else {
	        // Bluetooth was enabled and then start searching Bluetooth device
	    	if (D) Log.d(TAG,"Start discovery");
	    	mBluetoothAdapter.startDiscovery();
	    	String text = "";
	    	text = "Discovering...";
	    	Intent i = new Intent(ACTION_STATUS_PROCESSING);
			i.putExtra("TEXT", text);
		    sendBroadcast(i);
	    }
	    
	    DeviceList = new ArrayList <String> ();
	    
	    mHandler = new Handler();
	    mRunnable = new Runnable() {
			@Override
			public void run() {
				CustomToast.showToast(getBaseContext(), "Cannect connect to Device ", 1000);
				finish();
			}
		};
		mHandler.postDelayed(mRunnable, 30 * 1000);
	}
	
	/**
     * start to pair and calls getProfileProxy to connect or disconnect
     */
    public void pair() {
    	if (mBluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE){
    		/*start to pair*/
    		try {
    			Method m = mBluetoothDevice.getClass().getMethod("createBond", (Class[])null);
    			m.invoke(mBluetoothDevice, (Object[])null);
    		} catch(Exception e){
    			Log.e(TAG,"[Pair] Create Bond failed");
    		}
    	}
    }
    
    /**
     * The ServiceListener to Handle BluetoothHeadset and BluetoothA2dp profile
     */
    private BluetoothProfile.ServiceListener mProfileListener = 
            new BluetoothProfile.ServiceListener() { 
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
        	Intent i = new Intent(ACTION_STATUS_PROCESSING);
        	String text = "";
        	//connect headset
            if (profile == BluetoothProfile.HEADSET) {
            	if (D) Log.d(TAG,"Profile: Headset");
                headset = (BluetoothHeadset) proxy;
                
                if (D) Log.d(TAG,"headset state = "+headset.getConnectionState(mBluetoothDevice));
                //is_connecting : to sync the connection state with a2dp 
                if(headset.getConnectionState(mBluetoothDevice) != BluetoothHeadset.STATE_CONNECTED && (is_connecting && has_a2dp || !has_a2dp)){
                	if(!has_a2dp) {
                		text = "Connecting " + mBluetoothDevice.getName() + "...";
        				i.putExtra("TEXT", text);
       			    	sendBroadcast(i);
                	}
                	
                	try {
						Method m = headset.getClass().getMethod("connect", mBluetoothDevice.getClass());
						Object arg = mBluetoothDevice;
						m.invoke(headset, arg);
						if (D) Log.d(TAG,"Connect headset");
                	} catch(Exception e){
						Log.e(TAG,"Connect failed");
					}
                }
            } 
            
            //connect a2dp
            if (profile == BluetoothProfile.A2DP) { 
            	if (D) Log.d(TAG,"Profile: A2DP");
                a2dp = (BluetoothA2dp) proxy;
                
                if (D) Log.d(TAG,"a2dp state = "+a2dp.getConnectionState(mBluetoothDevice));
                if(a2dp.getConnectionState(mBluetoothDevice) != BluetoothA2dp.STATE_CONNECTED) {
                	is_connecting = true;
                	//show process dialog
                	text = "Connecting " + mBluetoothDevice.getName() + "...";
    				i.putExtra("TEXT", text);
   			    	sendBroadcast(i);
   			    	
                	try {
						Method m = a2dp.getClass().getMethod("connect", mBluetoothDevice.getClass());
						Object arg = mBluetoothDevice;
						m.invoke(a2dp, arg);
						if (D) Log.d(TAG,"Connect a2dp");
					} catch(Exception e){
						Log.e(TAG,"Connect a2dp failed");
					}
                }
            }
        } 
        public void onServiceDisconnected(int profile) { 
            if (profile == BluetoothProfile.HEADSET) {
                headset = null; 
            } else if (profile == BluetoothProfile.A2DP) {
            	a2dp = null;
            }
       } 
    };
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
        super.onActivityResult(requestCode, resultCode, data);        
        if(requestCode == REQUEST_ENABLE_BT){                     
        	if(resultCode==RESULT_OK) {
                // User enable the Bluetooth, start searching device             
        		if (D) Log.d(TAG,"Start discovery after enabling BT");
            	mBluetoothAdapter.startDiscovery();
            	String text = "";
    	    	text = "Discovering...";
    	    	Intent i = new Intent(ACTION_STATUS_PROCESSING);
    			i.putExtra("TEXT", text);
    		    sendBroadcast(i);
            } else {
            	finish(); 
            }
        }
    }
	
	/* The BroadcastReceiver */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String text = "";
            Intent i = new Intent(ACTION_STATUS_PROCESSING);
            Intent f = new Intent(ACTION_STATUS_FINISH);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            	if (D) Log.d(TAG,"Device found");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothClass device_cod = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);         
                
                if (device_cod != null && device_cod.getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                	// Only find accessories which are AUDIO/VIDEO
                	if (D) Log.i(TAG,"Device COD - Major: "+device_cod.getMajorDeviceClass());
                	switch (recognizing_statement) {
                	case recognize_by_RSSI:
                		int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                		if (rssi > rssi_value) {
                    		rssi_index = DeviceList.size();
                    		rssi_value = rssi;
                    	}
                    	if (D) Log.w(TAG,"Device: "+device.getName()+device.getAddress()+", RSSI: "+rssi);
                    	DeviceList.add(device.getAddress());
                    	break;
                	case recognize_by_Name:
                		if ( device.getName().equals(supported_Name) ) {
                			if (D) Log.d(TAG,"Found Supported Headset: "+device.getName());
                			mBluetoothAdapter.cancelDiscovery();
                			bd_addr = device.getAddress();
                		}
                		break;
                	case recognize_by_Address:
                		String s = device.getAddress();
                		if ( s.startsWith(supported_Addr) ) {
                			if (D) Log.d(TAG,"Found Supported Headset: "+device.getName());
                			mBluetoothAdapter.cancelDiscovery();
                			bd_addr = device.getAddress();
                		}
                		break;
                   	case recognize_by_First:
                		if (D) Log.d(TAG,"Found Supported Headset: "+device.getName());
                		mBluetoothAdapter.cancelDiscovery();
                		bd_addr = device.getAddress();
                		break;
                	}
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            	if (!discovery_finish) {
	            	if (recognizing_statement == recognize_by_RSSI) {
	            		if ( !DeviceList.isEmpty() ) {
	            			if (D) Log.v(TAG,"End Discovery, MAX RSSI: "+DeviceList.get(rssi_index));
	            			bd_addr = DeviceList.get(rssi_index);
	            		}
	            	}
	            	
	            	if (bd_addr != null) {
		            	mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(bd_addr.toUpperCase());
		            	
		            	if (mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
		            		if (D) Log.d(TAG,"Found device bonded, connect it");
		            		try {
		    	    			Method m = mBluetoothDevice.getClass().getMethod("getUuids", (Class[])null);
		    	    			ParcelUuid[] uuids = (ParcelUuid[])m.invoke(mBluetoothDevice, (Object[])null);
		    	    			for(ParcelUuid element : uuids) {
		    	    				if(element.equals(AudioSource)||element.equals(AudioSink))
		    	    					has_a2dp = true;
		    	    				if(element.equals(HeadsetService)||element.equals(Handsfree))
		    	    					has_hsp = true;
		    	    			}
		    	    		} catch(Exception e){
		    	    			Log.e(TAG,"getUuids failed when already paired");
		    	    		}
		    				
		    				if(has_a2dp) {
		    					mBluetoothAdapter.getProfileProxy(Onetap_Activity.this, mProfileListener, BluetoothProfile.A2DP);
		    				}
		    				if(has_hsp) {
		    					mBluetoothAdapter.getProfileProxy(Onetap_Activity.this, mProfileListener, BluetoothProfile.HEADSET);
		    				}
		            	} else {
		            		if (D) Log.d(TAG,"Found device not bonded, pair and then connect it");
		            		pair();
		            	}
	            	} else {
	            		/* No match device, end one tap pairing */
	            		sendBroadcast(f);
	            		CustomToast.showToast(getBaseContext(), "No supported headset found", 3000);
	            	}
	            	discovery_finish = true;
            	}
        	} else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
    			if(mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED){
    				//check a2dp uuid exited? add delay otherwise getUuids may fail
    				
    				try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
    				
    				try {
    	    			Method m = mBluetoothDevice.getClass().getMethod("getUuids", (Class[])null);
    	    			ParcelUuid[] uuids = (ParcelUuid[])m.invoke(mBluetoothDevice, (Object[])null);
    	    			for(ParcelUuid element : uuids) {
    	    				if(element.equals(AudioSource)||element.equals(AudioSink))
    	    					has_a2dp = true;
    	    				if(element.equals(HeadsetService)||element.equals(Handsfree))
    	    					has_hsp = true;
    	    			}
    	    		} catch(Exception e){
    	    			Log.e(TAG,"getUuids failed in Bonded intent");
    	    		}
    				
    				if(has_a2dp) {
    					mBluetoothAdapter.getProfileProxy(Onetap_Activity.this, mProfileListener, BluetoothProfile.A2DP);
    				}
    				if(has_hsp) {
    					mBluetoothAdapter.getProfileProxy(Onetap_Activity.this, mProfileListener, BluetoothProfile.HEADSET);
    				}
    			} else if(mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING){
   					text = "Pairing "+ mBluetoothDevice.getName() + "...";
   					i.putExtra("TEXT", text);
   			    	sendBroadcast(i);
    			} else if(mBluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
   					text = "Can not pair to device " + mBluetoothDevice.getName();
   					CustomToast.showToast(getBaseContext(), text, 2500);
   					sendBroadcast(f);
   				} else {
   					text = "error";
   				}
    		} else if(action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
    			if (D) Log.v(TAG,"Headset state change: "+headset.getConnectionState(mBluetoothDevice));
    			if ( headset.getConnectionState(mBluetoothDevice) == BluetoothHeadset.STATE_CONNECTED ) {
    				sendBroadcast(f);
    			} else{
    				text = "" + headset.getConnectionState(mBluetoothDevice);
        			//finish();
    			}
    		} else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
    			if (D) Log.v(TAG,"A2dp state change: "+a2dp.getConnectionState(mBluetoothDevice));
    			if ( a2dp.getConnectionState(mBluetoothDevice) == BluetoothA2dp.STATE_CONNECTED ) {
    				if (!has_hsp) {
    					sendBroadcast(f);
    				}
    			}
    			else {
        			//finish();
				}
    		} else if(action.equals(ACTION_STATUS_PROCESSING)) {
    			if (D) Log.d(TAG,"pdialog show");
    			if (pdialog != null) pdialog.dismiss();
    			text = intent.getExtras().getString("TEXT");
    			pdialog = ProgressDialog.show(Onetap_Activity.this, "", text, true, true, cancelListener);
    		} else if(action.equals(ACTION_STATUS_FINISH)) {
    			if (D) Log.w(TAG,"Action status finish, pdialog dismiss");
    			mHandler.removeCallbacks(mRunnable);
    			pdialog.dismiss();
    			finish();
    		} 
        }
    };
    
    OnCancelListener cancelListener = new OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialog) {
			if (D) Log.e(TAG,"ProgressDialog Cancel");
			finish();
		}
    };
    
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (D) Log.v(TAG,"on Key down");
		return super.onKeyDown(keyCode, event);
    }
	
	@Override
	protected void onStop() {
		super.onStop();
		if(D) Log.d(TAG,"onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (D) Log.d(TAG,"[OneTap] Activity onDestroy");
		unregisterReceiver(mReceiver);
		if (has_a2dp) {
			mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, a2dp);
		}
		mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, headset);
		if ( mBluetoothAdapter.isDiscovering() )  {
			if(D) Log.d(TAG,"Cancel discovering");
			mBluetoothAdapter.cancelDiscovery();
		}
	}
}