package com.issc.isscaudiowidget;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class GPIO_Activity extends Activity {
	
	private boolean D = false;
	private static final String TAG = "GPIO_control";
	
	Switch pin1_dir = null;
	Switch pin1_value = null;
	Switch pin2_dir = null;
	Switch pin2_value = null;
	Switch pin3_dir = null;
	Switch pin3_value = null;
	Switch pin4_dir = null;
	Switch pin4_value = null;
	ImageView headset_status = null;
	ImageView SPP_status = null;
	Button bt_apply = null;
	
	byte [] buffer = new byte[17];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D) Log.d(TAG,"[onCreate] GPIO_Activity onCreate");
		setContentView(R.layout.gpio);
		
		pin1_dir = (Switch) findViewById (R.id.pin1_switch_dir);
		pin1_dir.setOnCheckedChangeListener(pin1_dir_listener);
		
		pin1_value = (Switch) findViewById (R.id.pin1_switch_value);
		pin1_value.setOnCheckedChangeListener(pin1_value_listener);
		
		pin2_dir = (Switch) findViewById (R.id.pin2_switch_dir);
		pin2_dir.setOnCheckedChangeListener(pin2_dir_listener);
		
		pin2_value = (Switch) findViewById (R.id.pin2_switch_value);
		pin2_value.setOnCheckedChangeListener(pin2_value_listener);
		
		pin3_dir = (Switch) findViewById (R.id.pin3_switch_dir);
		pin3_dir.setOnCheckedChangeListener(pin3_dir_listener);
		
		pin3_value = (Switch) findViewById (R.id.pin3_switch_value);
		pin3_value.setOnCheckedChangeListener(pin3_value_listener);
		
		pin4_dir = (Switch) findViewById (R.id.pin4_switch_dir);
		pin4_dir.setOnCheckedChangeListener(pin4_dir_listener);
		
		pin4_value = (Switch) findViewById (R.id.pin4_switch_value);
		pin4_value.setOnCheckedChangeListener(pin4_value_listener);
		
		
		headset_status = (ImageView) findViewById (R.id.gpio_status);
		SPP_status = (ImageView) findViewById (R.id.gpio_spp_status);
		
		if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
			headset_status.setImageResource(R.drawable.connect);
			SPP_status.setImageResource(R.drawable.datatransmission);
		} else {
			if ( ((Bluetooth_Conn) getApplication()).isHeadset() )
				headset_status.setImageResource(R.drawable.connect);
		}
		
		bt_apply = (Button) findViewById (R.id.gpio_apply);
		bt_apply.setOnClickListener(apply_listener);
		
		/* Command initialization */
		buffer[0]  = (byte) 0xaa; buffer[1]  = (byte) 0x00;
		buffer[2]  = (byte) 0x0d; buffer[3]  = (byte) 0x1e;


		buffer[8]  = (byte) 0x00; buffer[9]  = (byte) 0x00; // In/Out
		buffer[10] = (byte) 0x00; buffer[11] = (byte) 0x00;
		buffer[12] = (byte) 0x00; buffer[13] = (byte) 0x00; // Output value
		buffer[14] = (byte) 0x00; buffer[15] = (byte) 0x00;
		buffer[16] = (byte) 0xbb;
		
		/* Register Broadcast receiver to get SPP command response and status */
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("GPIO_EVENT");
		intentFilter.addAction("CMD_ACK");
		intentFilter.addAction("Headset_Disconnect");
		intentFilter.addAction("Headset_Connect");
		intentFilter.addAction("SPP_disconnect");
		intentFilter.addAction("SPP_setup");
		registerReceiver(mBroadcast, intentFilter);
		
	}
	
	/* Broadcast Receiver for SPP link (Command ACK) */ 
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
        	} else if (arg1.getAction().equals("CMD_ACK")) {
				if (D) Log.i(TAG,"Receive Command ACK: CMD_ACK");
        		String s = arg1.getStringExtra("ack");
        		if ( s.startsWith("1E") && s.endsWith("00") ) {
        			if (D) Log.i(TAG,"Set GPIO success");
        		}
			} else if (arg1.getAction().equals("GPIO_EVENT")) {
				if (D) Log.i(TAG,"Receive Command ACK: GPIO Status");
				String s = arg1.getStringExtra("status");
				
				if (D) Log.d(TAG,"Mask: "+s.charAt(0)+s.charAt(1)+s.charAt(2)+s.charAt(3)
										 +s.charAt(4)+s.charAt(5)+s.charAt(6)+s.charAt(7));
				if (D) Log.d(TAG,"IO level: "+s.charAt(8)+s.charAt(9)+s.charAt(10)+s.charAt(11)
						 					 +s.charAt(12)+s.charAt(13)+s.charAt(14)+s.charAt(15));
				
				if ( !pin1_dir.isChecked() ) {
					//Log.d(TAG,"position 2: "+s.codePointAt(2)+"position 10: "+s.codePointAt(10));
					if ( s.codePointAt(2) == 'B' && s.codePointAt(10) == '4' ) {
						if (D) Log.d(TAG,"Input High");
						pin1_value.setChecked(true);
					} else if ( s.codePointAt(2) == 'B' && s.codePointAt(10) == '0' ) {
						if (D) Log.d(TAG,"Input Low");
						pin1_value.setChecked(false);
					}
				}
				
				if ( !pin2_dir.isChecked() ) {
					//Log.d(TAG,"position 1: "+s.codePointAt(1)+"position 9: "+s.codePointAt(9));
					if ( s.codePointAt(1) == 'D' && s.codePointAt(9) == '2' ) {
						if (D) Log.d(TAG,"Input High");
						pin2_value.setChecked(true);
					} else if ( s.codePointAt(1) == 'D' && s.codePointAt(9) == '0' ) {
						if (D) Log.d(TAG,"Input Low");
						pin2_value.setChecked(false);
					}
				}
				
				if ( !pin3_dir.isChecked() ) {
					//Log.d(TAG,"position 0: "+s.codePointAt(0)+"position 8: "+s.codePointAt(8));
					if ( s.codePointAt(0) == 'D' && s.codePointAt(8) == '2' ) {
						if (D) Log.d(TAG,"Input High");
						pin3_value.setChecked(true);
					} else if ( s.codePointAt(0) == 'D' && s.codePointAt(8) == '0' ) {
						if (D) Log.d(TAG,"Input Low");
						pin3_value.setChecked(false);
					}
				}
				
				if ( !pin4_dir.isChecked() ) {
					//Log.d(TAG,"position 4: "+s.codePointAt(4)+"position 12: "+s.codePointAt(12));
					if ( s.codePointAt(4) == '7' && s.codePointAt(12) == '8' ) {
						if (D) Log.d(TAG,"Input High");
						pin4_value.setChecked(true);
					} else if ( s.codePointAt(4) == '7' && s.codePointAt(12) == '0' ) {
						if (D) Log.d(TAG,"Input Low");
						pin4_value.setChecked(false);
					}
				}
			} 
		}
	};
	
	public OnClickListener apply_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Send Command only if SPP linked
			if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
				// calculate checksum before send command
				int i=1; int checksum = 0x1000;
				for (i=1;i<16;i++) checksum -= buffer[i];
				buffer[16] = (byte) (checksum % 0x100);
				
				((Bluetooth_Conn) getApplication()).setCurCmd(buffer[3]);
				((Bluetooth_Conn) getApplication()).setCurCmdPara((byte)0x00);
				((Bluetooth_Conn) getApplication()).write(buffer);
			}
		}
	};
	
	/* pin1 direction listener, P1_6 */
	public OnCheckedChangeListener pin1_dir_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {		
			if (isChecked) {
				if (D) Log.w(TAG,"In pin1_dir_listener: Checked");
				buffer[9] |= (byte) 0x40;
			} else {
				if (D) Log.w(TAG,"In pin1_dir_listener: UnChecked");
				buffer[9] ^= (byte) 0x40;
			}
		}
	};
	
	/* pin1 value listener, P1_6*/
	public OnCheckedChangeListener pin1_value_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				if (D) Log.w(TAG,"In pin1_value_listener: Checked");
				buffer[13] |= (byte) 0x40;
			} else {
				if (D) Log.w(TAG,"In pin1_value_listener: UnChecked");
				buffer[13] ^= (byte) 0x40;
			}
		}
	};
	
	/* pin2 direction listener, P0_1 */
	public OnCheckedChangeListener pin2_dir_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {		
			if (isChecked) {
				if (D) Log.w(TAG,"In pin2_dir_listener: Checked");
				buffer[8] |= (byte) 0x02;
			} else {
				if (D) Log.w(TAG,"In pin2_dir_listener: UnChecked");
				buffer[8] ^= (byte) 0x02;
			}
		}
	};
	
	/* pin2 value listener, P0_1*/
	public OnCheckedChangeListener pin2_value_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				if (D) Log.w(TAG,"In pin2_value_listener: Checked");
				buffer[12] |= (byte) 0x02;
			} else {
				if (D) Log.w(TAG,"In pin2_value_listener: UnChecked");
				buffer[12] ^= (byte) 0x02;
			}
		}
	};
	
	/* pin3 direction listener, P0_5 */
	public OnCheckedChangeListener pin3_dir_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {		
			if (isChecked) {
				if (D) Log.w(TAG,"In pin3_dir_listener: Checked");
				buffer[8] |= (byte) 0x20;
			} else {
				if (D) Log.w(TAG,"In pin3_dir_listener: UnChecked");
				buffer[8] ^= (byte) 0x20;
			}
		}
	};
	
	/* pin3 value listener, P0_5*/
	public OnCheckedChangeListener pin3_value_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				if (D) Log.w(TAG,"In pin3_value_listener: Checked");
				buffer[12] |= (byte) 0x20;
			} else {
				if (D) Log.w(TAG,"In pin3_value_listener: UnChecked");
				buffer[12] ^= (byte) 0x20;
			}
		}
	};
	
	/* pin4 direction listener, P2_7 */
	public OnCheckedChangeListener pin4_dir_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {		
			if (isChecked) {
				if (D) Log.w(TAG,"In pin4_dir_listener: Checked");
				buffer[10] |= (byte) 0x80;
			} else {
				if (D) Log.w(TAG,"In pin4_dir_listener: UnChecked");
				buffer[10] ^= (byte) 0x80;
			}
		}
	};
	
	/* pin4 value listener, P2_7*/
	public OnCheckedChangeListener pin4_value_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				if (D) Log.w(TAG,"In pin4_value_listener: Checked");
				buffer[14] |= (byte) 0x80;
			} else {
				if (D) Log.w(TAG,"In pin4_value_listener: UnChecked");
				buffer[14] ^= (byte) 0x80;
			}
		}
	};
	
	
	@Override
	protected void onDestroy() {
	   super.onDestroy();
	   unregisterReceiver(mBroadcast);
	   if (D) Log.d(TAG,"[GPIO Engineer] Activity destroy");
	}
}