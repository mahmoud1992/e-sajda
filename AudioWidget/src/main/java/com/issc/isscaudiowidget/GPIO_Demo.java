package com.issc.isscaudiowidget;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class GPIO_Demo extends Activity {
	
	private boolean D = false;
	private static final String TAG = "GPIO_control_DEMO";
	
	Switch pin1_value = null;
	Switch pin2_value = null;
	
	Button btn1 = null;
	Button btn2 = null;
	Button btn_pattern1 = null;
	Button btn_pattern2 = null;
	Button btn_stop = null;

	ImageView headset_status = null;
	ImageView SPP_status = null;
	ImageView LED1 = null;
	ImageView LED2 = null;
	
	PatternThread1 p1 = null;
	PatternThread2 p2 = null;
	
	byte [] buffer = new byte[17];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D) Log.d(TAG,"[onCreate] GPIO_Demo onCreate");
		setContentView(R.layout.gpio_demo);
		
		btn1 = (Button) findViewById (R.id.gpio_demo_btn1);
		btn2 = (Button) findViewById (R.id.gpio_demo_btn2);
		btn_pattern1 = (Button) findViewById (R.id.gpio_demo_pattern1);
		btn_pattern1.setOnClickListener(pattern1_listener);
		btn_pattern2 = (Button) findViewById (R.id.gpio_demo_pattern2);
		btn_pattern2.setOnClickListener(pattern2_listener);
		btn_stop = (Button) findViewById (R.id.gpio_demo_pattern_stop);
		btn_stop.setOnClickListener(stop_listener);
		
		headset_status = (ImageView) findViewById (R.id.gpio_demo_status);
		SPP_status = (ImageView) findViewById (R.id.gpio_demo_spp_status);
		LED1 = (ImageView) findViewById (R.id.gpio_demo_led1);
		LED2 = (ImageView) findViewById (R.id.gpio_demo_led2);
		
		pin1_value = (Switch) findViewById (R.id.gpio_demo_led1_switch);
		pin1_value.setOnCheckedChangeListener(pin1_value_listener);
		pin2_value = (Switch) findViewById (R.id.gpio_demo_led2_switch);
		pin2_value.setOnCheckedChangeListener(pin2_value_listener);
		
		if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
			headset_status.setImageResource(R.drawable.connect);
			SPP_status.setImageResource(R.drawable.datatransmission);
		} else {
			if ( ((Bluetooth_Conn) getApplication()).isHeadset() )
				headset_status.setImageResource(R.drawable.connect);
		}
		
		/* Command initialization */
		buffer[0]  = (byte) 0xaa; buffer[1]  = (byte) 0x00;
		buffer[2]  = (byte) 0x0d; buffer[3]  = (byte) 0x1e;
		buffer[4]  = (byte) 0xdd; buffer[5]  = (byte) 0xbf; // Mask
		buffer[6]  = (byte) 0x7f; buffer[7]  = (byte) 0xff;
		buffer[8]  = (byte) 0x02; buffer[9]  = (byte) 0x40; // In/Out		
		buffer[10] = (byte) 0x00; buffer[11] = (byte) 0x00;
		buffer[12] = (byte) 0x00; buffer[13] = (byte) 0x00; // Output value
		buffer[14] = (byte) 0x00; buffer[15] = (byte) 0x00;
		buffer[16] = (byte) 0x79;
		
		sendSPP();
		
		/* Register Broadcast receiver to get SPP command response and status*/
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("GPIO_EVENT");
		intentFilter.addAction("CMD_ACK");
		intentFilter.addAction("Headset_Disconnect");
		intentFilter.addAction("Headset_Connect");
		intentFilter.addAction("SPP_disconnect");
		intentFilter.addAction("SPP_setup");
		registerReceiver(mBroadcast, intentFilter);
	}
	
	public class PatternThread1 implements Runnable {
		Message message;
		String obj = "p1_run";
		boolean flag = false;
		boolean life = true;
		
		private Thread thread = null;
		
		public PatternThread1() {
			this.thread = new Thread(this);
		}
		
		public void start() {
    		this.thread.start();
    	}
		
		public void stop() {
			life = false;
			this.thread.interrupt();
		}
		
		@Override
		public void run() {
			if (D) Log.v(TAG,"P1 run");
			while(life) {
				try {
					if (D) Log.v(TAG,"P1 in while");
					if (flag == false) {
						flag = true;
						message = handler.obtainMessage(1,obj);
					} else {
						flag = false;
						message = handler.obtainMessage(0,obj);
					}
					handler.sendMessage(message);
					Thread.sleep(1000);
				} catch (Exception e) {
					if (D) Log.v(TAG,"Pattern1 Thread exception");
					e.printStackTrace();
				}
			}
			this.thread = null;
		}
		
	}
	
	public class PatternThread2 implements Runnable {
		Message message;
		String obj = "p2_run";
		boolean flag = false;
		boolean life = true;
		
		private Thread thread = null;
		
		public PatternThread2() {
			this.thread = new Thread(this);
		}
		
		public void start() {
    		this.thread.start();
    	}
		
		public void stop() {
			life = false;
			//this.thread.interrupt();
		}
		
		@Override
		public void run() {
			if (D) Log.v(TAG,"P2 run");
			while(life) {
				try {
					if (D) Log.v(TAG,"P2 in while");
					if (flag == false) {
						flag = true;
						message = handler.obtainMessage(1,obj);
					} else {
						flag = false;
						message = handler.obtainMessage(0,obj);
					}
					handler.sendMessage(message);
					Thread.sleep(1000);
				} catch (Exception e) {
					Log.e(TAG,"Pattern2 Thread exception");
				}
			}
			this.thread = null;
		}
		
	}
	
	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String s = (String)msg.obj;
			if (s.equals("p1_run")) {
				if (msg.what == 1) {
					buffer[13] |= (byte) 0x40;
					buffer[12] ^= (byte) 0x02;
					sendSPP();
					LED1.setImageResource(R.drawable.ledblue);
					LED2.setImageResource(R.drawable.ledblack);
				} else if (msg.what == 0) {
					buffer[13] ^= (byte) 0x40;
					buffer[12] |= (byte) 0x02;
					sendSPP();
					LED1.setImageResource(R.drawable.ledblack);
					LED2.setImageResource(R.drawable.ledblue);
				}
			}
			if (s.equals("p2_run")) {
				if (msg.what == 1) {
					buffer[13] |= (byte) 0x40;
					buffer[12] |= (byte) 0x02;
					sendSPP();
					LED1.setImageResource(R.drawable.ledblue);
					LED2.setImageResource(R.drawable.ledblue);			
				} else if (msg.what == 0) {
					buffer[13] ^= (byte) 0x40;
					buffer[12] ^= (byte) 0x02;
					sendSPP();
					LED1.setImageResource(R.drawable.ledblack);
					LED2.setImageResource(R.drawable.ledblack);
				}
				
			}
		}
	};
	
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
        		if (headset_status != null) headset_status.setImageResource(R.drawable.connect);
        		if (SPP_status != null) SPP_status.setImageResource(R.drawable.datatransmission);
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
				
				if (D) Log.i(TAG,"Mask: "+s.charAt(0)+s.charAt(1)+s.charAt(2)+s.charAt(3)
										 +s.charAt(4)+s.charAt(5)+s.charAt(6)+s.charAt(7));
				if (D) Log.i(TAG,"IO level: "+s.charAt(8)+s.charAt(9)+s.charAt(10)+s.charAt(11)
						 					 +s.charAt(12)+s.charAt(13)+s.charAt(14)+s.charAt(15));
				
				// Button1 event
				//if (D) Log.d(TAG,"position 0: "+s.codePointAt(0)+"position 8: "+s.codePointAt(8));
				if ( s.codePointAt(0) == 'D' && s.codePointAt(8) == '2' ) {
					if (D) Log.d(TAG,"Input High");
					btn1.setPressed(false);
				} else if ( s.codePointAt(0) == 'D' && s.codePointAt(8) == '0' ) {
					if (D) Log.d(TAG,"Input Low");
					btn1.setPressed(true);
				}
				
				// Button2 event
				//if (D) Log.d(TAG,"position 4: "+s.codePointAt(4)+"position 12: "+s.codePointAt(12));
				if ( s.codePointAt(4) == '7' && s.codePointAt(12) == '8' ) {
					if (D) Log.d(TAG,"Input High");
					btn2.setPressed(false);
				} else if ( s.codePointAt(4) == '7' && s.codePointAt(12) == '0' ) {
					if (D) Log.d(TAG,"Input Low");
					btn2.setPressed(true);
				}
			} 
		}
	};
	
	public void sendSPP() {
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
	
	/* pin1 value listener, P1_6*/
	public OnCheckedChangeListener pin1_value_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				if (D) Log.w(TAG,"In pin1_value_listener: Checked");
				buffer[13] |= (byte) 0x40;
				LED1.setImageResource(R.drawable.ledblue);
			} else {
				if (D) Log.w(TAG,"In pin1_value_listener: UnChecked");
				buffer[13] ^= (byte) 0x40;
				LED1.setImageResource(R.drawable.ledblack);
			}
			sendSPP();
		}
	};
	
	
	/* pin2 value listener, P0_1*/
	public OnCheckedChangeListener pin2_value_listener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				if (D) Log.w(TAG,"In pin2_value_listener: Checked");
				buffer[12] |= (byte) 0x02;
				LED2.setImageResource(R.drawable.ledblue);
			} else {
				if (D) Log.w(TAG,"In pin2_value_listener: UnChecked");
				buffer[12] ^= (byte) 0x02;
				LED2.setImageResource(R.drawable.ledblack);
			}
			sendSPP();
		}
	};
	
	public OnClickListener pattern1_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
				if (p2 != null) {
					if (D) Log.d(TAG,"p2 != null");
					p2.stop();
					p2 = null;
				}
				if (p1 == null) {
					p1 = new PatternThread1();
					p1.start();
				}
			}
		}
	};
	
	public OnClickListener pattern2_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
				if (p1 != null) {
					if (D) Log.d(TAG,"p1 != null");
					p1.stop();
					p1 = null;
				}
				if (p2 == null) {
					p2 = new PatternThread2();
					p2.start();
				}
			}
		}
	};
	
	public OnClickListener stop_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (p1 != null) {
				if (D) Log.d(TAG,"p1 != null");
				p1.stop();
				p1 = null;
			}
			if (p2 != null) {
				if (D) Log.d(TAG,"p2 != null");
				p2.stop();
				p2 = null;
			}
			
			if ( pin1_value.isChecked()) {
				LED1.setImageResource(R.drawable.ledblue);
				buffer[13] |= (byte) 0x40;
			} else { 
				LED1.setImageResource(R.drawable.ledblack);
				if ( (buffer[13] & (byte)0x40) == (byte)0x40 )
				   buffer[13] ^= (byte) 0x40;
			}
			if ( pin2_value.isChecked()) {
				LED2.setImageResource(R.drawable.ledblue);
				buffer[12] |= (byte) 0x02;
			} else {
				LED2.setImageResource(R.drawable.ledblack);
				if ( (buffer[12] & (byte)0x02) == (byte)0x02 )
				   buffer[12] ^= (byte) 0x02;
			}
			sendSPP();
		}
	};
	
	@Override
	protected void onDestroy() {
	   super.onDestroy();
	   unregisterReceiver(mBroadcast);
	   if (p1 != null) {
		   p1.stop();
		   p1 = null;
	   }
	   if (p2 != null) {
		   p2.stop();
		   p2 = null;
	   }
	   
	   if ( (buffer[13] & (byte)0x40) == (byte)0x40 )
		   buffer[13] ^= (byte) 0x40;
	   
	   if ( (buffer[12] & (byte)0x02) == (byte)0x02 )
		   buffer[12] ^= (byte) 0x02;

	   sendSPP();
	   
	   if (D) Log.d(TAG,"[GPIO_Demo] Activity destroy");
	}
}