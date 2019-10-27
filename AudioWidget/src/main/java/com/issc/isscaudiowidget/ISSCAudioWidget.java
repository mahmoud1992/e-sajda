package com.issc.isscaudiowidget;

import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class ISSCAudioWidget extends Activity implements OnCompletionListener, OnErrorListener {
	
	private boolean D = false;
	private static final String TAG = "ISSCAudioWidget";
	private boolean version_demo = true;    // set false/true as engineer/demo version

    /* components declaration */
	private Handler mHandler = new Handler();
	public  TextView text_eq = null;
	public  TextView text_st = null;
	private TextView text_fw = null;
	private TextView text_addr = null;
	private TextView batteryStatus = null;
	private Switch switch_noise = null;
	private Switch switch_Eq = null;
	private Switch enable_tts = null;
	private Button button_name = null;
	private Button voicePromptSwitch_btn = null;
	private Button batteryStatus_btn = null;
	private Button drc_prev_btn = null;
	private Button drc_next_btn = null;
	private EditText edit1 = null;
	private ImageView img_status = null;
	private ImageView img_spp_status = null;
	private ImageView img_about  = null;
	private BluetoothDevice  device   = null;
	private SharedPreferences dsp_data = null;
	
	
	/* flag for maintain switch button status */
	private boolean switch_back = false;

	/* flag for maintain equalizer switch button status */
	private boolean switch_eq_back = false;
	
	/* Command queue for initialized commands */
	private int [][] initial_cmd_queue = new int [2][2];
	
	/* elements for send tone */
	private Button  sendtone_send = null;
	private Button  sendtone_stop = null;
	private SeekBar sendtone_bar1 = null;
	int pro = 20;
	float init_vol = (float) 0.2;
	boolean click = false;
	boolean play = false;
	boolean enable_bt = false;
	MediaPlayer mp1;
	
	public AudioManager au = null;
	
    private final int REQUEST_ENABLE_BT = 1;

	/* RD version */
	/*Button btn_con_spp = null;
	Button btn_con_spp_a2dp = null;
	Button btn_server_socket_state = null;*/
	/* RD version */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (D) Log.v(TAG,"onCreate");
		super.onCreate(savedInstanceState);
		
		Intent sIntent = new Intent(this, BootupService.class);
		this.startService(sIntent);
		Intent i = new Intent(this, CallerNameService.class);
        this.startService(i);
		
		setContentView(R.layout.activity_front);
	    
		mHandler.postDelayed(new Runnable() {
			public void run() {
				setContentView(R.layout.activity_isscaudiowidget);

				//Equalizer init set-up
				switch_Eq = (Switch) findViewById (R.id.Eqswitch);
				switch_Eq.setOnCheckedChangeListener(Eq_checklistener);
				SharedPreferences s = getSharedPreferences("com.issc.isscaudiowidget", 0);
				boolean Eq_Sw = s.getBoolean("EqEnabled", false);
				int Eqlastprefval= s.getInt("Eqlastmode", -1); //get from shared preference.
				if(Eqlastprefval == -1) {
					Short[] eqcoef = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16384, 0};

					SharedPreferences.Editor edit = s.edit();
					for (int i = 0; i < eqcoef.length; i++) {
						edit.putInt("EqData_"+ Integer.toString(i), eqcoef[i]);
					}
					edit.putInt("Eqlastmode",0xA);
					edit.commit();
				}

				text_st = (TextView) findViewById (R.id.text2);
				text_st.setText("Tone1");
				text_fw = (TextView) findViewById (R.id.text3);
				text_addr = (TextView) findViewById (R.id.bdaddr);
				edit1 = (EditText) findViewById (R.id.edit1);
				
				switch_noise = (Switch) findViewById (R.id.switch1);
				switch_noise.setOnCheckedChangeListener(noise_checklistener);
				
				enable_tts = (Switch) findViewById(R.id.switch2);
				enable_tts.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						SharedPreferences s = getSharedPreferences("com.issc.isscaudiowidget", 0);
						Editor editor = s.edit();
						editor.putBoolean("SendTTS", isChecked);
						editor.commit();
				   		Intent i = new Intent(BootupService.ACTION_ENABLE_TTS);
						sendBroadcast(i);
					}
				});

				s = getSharedPreferences("com.issc.isscaudiowidget", 0);
           		enable_tts.setChecked(s.getBoolean("SendTTS", false));
           		
 				button_name = (Button) findViewById (R.id.btn1);
				button_name.setOnClickListener(name_clicklistener);
				voicePromptSwitch_btn = (Button) findViewById (R.id.voicePromptSwitch_btn);
				voicePromptSwitch_btn.setOnClickListener(voicePromptSwitch_clicklistener);
				batteryStatus_btn = (Button) findViewById (R.id.batteryStatus_btn);
				batteryStatus_btn.setOnClickListener(batteryStatus_clicklistener);
				batteryStatus = (TextView) findViewById(R.id.batteryStatus);
				//drc_prev_btn = (Button) findViewById (R.id.drc_prev_btn);
				//drc_prev_btn.setOnClickListener(drc_prev_clicklistener);
				drc_next_btn = (Button) findViewById (R.id.drc_next_btn);
				drc_next_btn.setOnClickListener(drc_next_clicklistener);
				
				/* RD version */
				/*btn_con_spp = (Button) findViewById (R.id.btn_con_spp);
				btn_con_spp.setOnClickListener(btn_con_spp_listener);
				btn_con_spp_a2dp = (Button) findViewById (R.id.btn_con_spp_a2dp);
				btn_con_spp_a2dp.setOnClickListener(btn_con_spp_a2dp_listener);
				btn_server_socket_state = (Button) findViewById (R.id.btn_server_socket_state);
				btn_server_socket_state.setOnClickListener(btn_server_socket_state_listener);*/
				/* RD version */
				
				img_spp_status = (ImageView) findViewById (R.id.main_spp_status);
				img_status = (ImageView) findViewById (R.id.main_status);
				img_about  = (ImageView) findViewById (R.id.main_about);
				
				au = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				
				// Register Broadcast receiver to get SPP command response
				IntentFilter intentFilter = new IntentFilter();
				intentFilter.addAction("FW_VER");
				intentFilter.addAction("SPP_setup");
				intentFilter.addAction("SPP_disconnect");
				intentFilter.addAction("CMD_ACK");
				intentFilter.addAction("Headset_Disconnect");
				intentFilter.addAction("Headset_Connect");
				intentFilter.addAction(BootupService.ACTION_REPLY_BATTERY_LEVEL);
				registerReceiver(mBroadcast, intentFilter);
				
				initial_contents();
				initial_SendTone();
				
				// notify user that device does not support Bluetooth
				if (((Bluetooth_Conn) getApplication()).getAdapter() == null) {
					CustomToast.showToast(getBaseContext(), "Device does not support Bluetooth!", 5000);
		            finish();
		        }
				// notify user to enable Bluetooth
		   		Intent i = new Intent(BootupService.ACTION_GET_BATTERY_LEVEL);
			    sendBroadcast(i);
			    Button camera = (Button)findViewById(R.id.btn_camera);
			    camera.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(ISSCAudioWidget.this, CameraActivity.class);
						startActivity(intent);
					}
				});
			}
		}, 2500);
	}

	  @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
	        super.onActivityResult(requestCode, resultCode, data);        
	        if(requestCode == REQUEST_ENABLE_BT){                     
	        	if(resultCode==RESULT_OK) {
	    			if (D) Log.v(TAG,"enable BT");
	    			//ShowDialog((int)4);
	            } else {
	            	//finish(); 
	            }
	        	Handler handler = new Handler();
	        	handler.postDelayed(new Runnable() {
					@Override
					public void run() {
			        	enable_bt = false;						
					}
				}, 1000);
	        }
	    }

	/* Initial each elements at different situation */
	public void initial_contents()
	{
		if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
			if (D) Log.v(TAG,"initial, not null");
			device = ((Bluetooth_Conn) ISSCAudioWidget.this.getApplication()).getDevice();
			img_status.setImageResource(R.drawable.connect);
			img_spp_status.setImageResource(R.drawable.datatransmission);
			text_addr.setText(device.getAddress());
			
			get_fw_ver();
			/* Sending DSP commands after receiving ack of fw version */
		}
		else {
			if (D) Log.v(TAG,"initial, null");
			if ( ((Bluetooth_Conn) getApplication()).isHeadset() ) 
				img_status.setImageResource(R.drawable.connect);
			edit1.setText("No supported headset");
			text_fw.setText("N/A");
			text_addr.setText("N/A");
		}
		img_about.setOnClickListener(about_listener);
	}
	
	OnClickListener about_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(ISSCAudioWidget.this, About_Activity.class);
    		startActivity(intent);
		}
	};
	
	/* Command: Show Firmware Version */
	public void get_fw_ver() {
		// Get FW version
		byte [] buffer = new byte[6];
		buffer[0] = (byte) 0xaa;  buffer[1] =  0x00;
		buffer[2] =  0x02;        buffer[3] =  0x08;
		buffer[4] =  0x01;        buffer[5] = (byte) 0xf5;
		
		((Bluetooth_Conn) getApplication()).write(buffer);
	}

	public void Eq_Sw_Disable()
	{
		SharedPreferences s = getSharedPreferences("com.issc.isscaudiowidget", 0);
		Editor editor = s.edit();
		editor.putBoolean("EqEnabled", false);
		editor.commit();
		switch_Eq = (Switch) findViewById (R.id.Eqswitch);
		switch_Eq.setChecked(false);
		//switch_eq_back = false;
		if (D) Log.i(TAG, "Eq_Sw_Disable called");
		CustomToast.showToast(getBaseContext(), "SPP mode not Enabled. \n Resetting Equalizer to off", 2000);
	}
	
	/* Broadcast Receiver for SPP link (Command ACK) */ 
	private BroadcastReceiver mBroadcast =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context mContext, Intent mIntent) {
        	if (mIntent.getAction().equals(BootupService.ACTION_REPLY_BATTERY_LEVEL)) {
        		String level = mIntent.getStringExtra("LEVEL");
        		if (level.equals("0%")) {
            		batteryStatus.setText("");
				}
        		else {
            		batteryStatus.setText(mIntent.getStringExtra("LEVEL"));
				}
        	}
        	else if (mIntent.getAction().equals("Headset_Disconnect")) {
        		if (D) Log.i(TAG,"Receive: Headset_Disconnect");
        		if (img_status != null) img_status.setImageResource(R.drawable.disconnect);
        		if (img_spp_status != null) img_spp_status.setImageResource(R.drawable.nospp);
        	} else if (mIntent.getAction().equals("Headset_Connect")) {
        		if (D) Log.i(TAG,"Receive: Headset_Connect");
        		if (img_status != null) img_status.setImageResource(R.drawable.connect);
        	} else if (mIntent.getAction().equals("SPP_disconnect")) {
        		if (D) Log.i(TAG,"Receive: SPP_disconnect");
        		if ( ((Bluetooth_Conn) getApplication()).isHeadset() ) {
        			if (img_status != null) img_status.setImageResource(R.drawable.connect);
        		} else {
        			if (img_status != null) img_status.setImageResource(R.drawable.disconnect);
        		}
        		if (img_spp_status != null) img_spp_status.setImageResource(R.drawable.nospp);
        		if (edit1     != null) edit1.setText("No supported headset");
        		if (text_fw   != null) text_fw.setText("N/A");
        		if (text_addr != null) text_addr.setText("N/A");
				Eq_Sw_Disable();
        	} else if (mIntent.getAction().equals("SPP_setup")) {
        		if (D) Log.i(TAG,"Receive: SPP_setup");     		
        		if (device == null)         device = ((Bluetooth_Conn) getApplication()).getDevice();
        		if (img_status != null)     img_status.setImageResource(R.drawable.connect);
        		if (img_spp_status != null) img_spp_status.setImageResource(R.drawable.datatransmission);
        		if (text_addr != null) text_addr.setText(device.getAddress());
        		
        		get_fw_ver();
        		/* Sending DSP commands after receiving ack of fw version */
        	} else if (mIntent.getAction().equals("FW_VER")) {
        		if (D) Log.i(TAG,"Receive Command ACK: FW_VER");
        		if (text_fw != null) text_fw.setText(mIntent.getStringExtra("version"));
        		
        		/* Sending DSP commands */
        		dsp_data = getSharedPreferences(device.getAddress(),0);
    			
    			String name = dsp_data.getString("device_name", "-8765");
    			if (name.equals("-8765")) {
    				if (D) Log.d(TAG,"-8765");
    				SharedPreferences.Editor edit = dsp_data.edit();
        			edit.putString("device_name", device.getName());
        			edit.commit();
        			if (edit1 != null) edit1.setText(device.getName());
    			} else {
    				if (D) Log.d(TAG,"Name not null");
    				setName(name);
    				if (edit1 != null) edit1.setText(name);
    			}
    			
    			boolean NR = dsp_data.getBoolean("NR_status", false);
				boolean EqMode = dsp_data.getBoolean("EqEnabled", false);
    			//int mode   = dsp_data.getInt("EQ_mode", 0);

				int mode;
				if(EqMode == true) {
					mode = 0xA; //custom mode
				}
				else
				{
					mode = 0;	//EQ off
				}
    			if (D) Log.d(TAG,"NR: "+NR+", mode: "+EqMode);
    			
    			initial_cmd_queue [0][0] = 0;  initial_cmd_queue [0][1] = 0; // initial value
    			initial_cmd_queue [1][0] = 0;  initial_cmd_queue [1][1] = 0; // initial value
    			
    			if (mode > 0 && mode < 11) {
    				initial_cmd_queue [0][0] = 0x1c;
    				initial_cmd_queue [0][1] = mode;
    				if (NR) {
    					initial_cmd_queue [1][0] = 0x1d;
        				initial_cmd_queue [1][1] = 0;
    				} else {
    					initial_cmd_queue [1][0] = 0;
        				initial_cmd_queue [1][1] = 0;
    				}
    			} else if (NR) {
    				initial_cmd_queue [0][0] = 0x1d;
    				initial_cmd_queue [0][1] = 0;
    				initial_cmd_queue [1][0] = 0;
    				initial_cmd_queue [1][1] = 0;
    			}
        	} else if (mIntent.getAction().equals("CMD_ACK")) {
        		if (D) Log.i(TAG,"Receive Command ACK: CMD_ACK");
        		String s = mIntent.getStringExtra("ack");
        		
        		if (D) Log.w(TAG,"device: "+device.getName()+device.getAddress());
        		SharedPreferences set_dsp = getSharedPreferences(device.getAddress(),0);
        		SharedPreferences.Editor edit = set_dsp.edit();
        		
        		if ( s.startsWith("1D") || s.startsWith("1C") || s.startsWith("05") || s.startsWith("30") ) {
        			if ( !s.endsWith("00") ) {
        				// command fail, error handling of NR
        				switch ( ((Bluetooth_Conn) getApplication()).getCurCmd() )
        				{
        				case 0x1d:
        					if ( ((Bluetooth_Conn) getApplication()).getCurCmdPara() == 0x18 ) {
        						switch_noise.setChecked(false);
        					} else if ( ((Bluetooth_Conn) getApplication()).getCurCmd() == 0x19 ) {
        						switch_noise.setChecked(true);
        					} else if ( ((Bluetooth_Conn) getApplication()).getCurCmd() == 0x1b ) {
        						switch_noise.setChecked(false);
        						((Bluetooth_Conn) getApplication()).setNextCmd((byte) 0x00);
        						((Bluetooth_Conn) getApplication()).setNextCmdPara((byte) 0x00);
        					} else if ( ((Bluetooth_Conn) getApplication()).getCurCmd() == 0x1c ) {
        						switch_noise.setChecked(true);
        						((Bluetooth_Conn) getApplication()).setNextCmd((byte) 0x00);
        						((Bluetooth_Conn) getApplication()).setNextCmdPara((byte) 0x00);
        					}
        					break;
        				}
        				switch_back = false;
        			} else {
        				byte PARA = ((Bluetooth_Conn) getApplication()).getCurCmdPara();
        				byte CMD  = ((Bluetooth_Conn) getApplication()).getCurCmd();
        				if (D) Log.d(TAG,"Cmd success: " + s + ", " + CMD + ", " + PARA );
        				
        				// command success, reply to main screen if needed
        				if ( ((Bluetooth_Conn) getApplication()).getReplyScreen() )
        				{
        					/*final String s_mode [] =
        						{"Off" , "Soft" , "Bass", "Treble", "Classic", "Rock", 
   		                         "Jazz", "POP"  , "Dance" , "R&B", "Custom" };*/
        					if ( CMD == 0x1c ) {
								byte para =((Bluetooth_Conn) getApplication()).getCurCmdPara();
								if ( para != 0x00 ) {
									switch_Eq.setChecked(true);
									edit.putBoolean("EqEnabled", switch_Eq.isChecked());
									edit.commit();
									if (D) Log.d(TAG,"EQ CMD_ACK success");
									switch_eq_back = false;
								}
								else {
									switch_Eq.setChecked(false);
									edit.putBoolean("EqEnabled",false );
									edit.commit();
									switch_eq_back = false;
									//switch_eq_back = false;
									//Eq_Sw_Disable();
								}

								if ( CMD == 0x30 ) {//EQ data command
									if (((Bluetooth_Conn) getApplication()).getCurCmdPara() == 0x00) {
										if (D) Log.d(TAG,"EQ Data CMD_ACK success");
									} else {
										if (D) Log.d(TAG,"EQ Data CMD_ACK failure");
										switch_eq_back = false;
									}
								}
        					}
        					else if ( CMD == 0x05 ) {
        						if (D) Log.d(TAG,"modified name success, save to settings");
        						edit.putString("device_name", edit1.getText().toString());
        						edit.commit();
        						//edit1.setText(edit1.getText().toString());
        						hideSoftKeyboard(ISSCAudioWidget.this);
        					}
        					((Bluetooth_Conn) getApplication()).setReplyScreen(false);
        				}
        				
        				switch (initial_cmd_queue [0][0])
        				{
        				case 0x1c:
        					//if (D) Log.d(TAG,"Cmd queue, EQ");
        					sendEQ(initial_cmd_queue [0][1]);
        					initial_cmd_queue [0][0] = initial_cmd_queue [1][0];
        					initial_cmd_queue [0][1] = initial_cmd_queue [1][1];
        					initial_cmd_queue [1][0] = 0;
        					initial_cmd_queue [1][1] = 0;
        					break;
        				case 0x1d:
        					//if (D) Log.d(TAG,"Cmd queue, NR");
        					switch_noise.setChecked(true);
        					initial_cmd_queue [0][0] = 0; initial_cmd_queue [0][1] = 0;
        					break;
        				}
        				
        				switch ( ((Bluetooth_Conn) getApplication()).getNextCmd() )
        				{
        				case 0x00:
        					if ( s.startsWith("1D") ) {
        						edit.putBoolean("NR_status", switch_noise.isChecked());
            					edit.commit();
            					CustomToast.showToast(getBaseContext(), "NR set success", 1000);
            					switch_back = false;
        					}
        					break;
        				case 0x1d:
        					if (D) Log.v(TAG,"2nd cmd: 1D");
        					sendNR((byte)0x1d, ((Bluetooth_Conn) getApplication()).getNextCmdPara(), (byte)0x00, (byte)0x00);
        					break;
        				default:
        					if (D) Log.v(TAG,"2nd cmd: default");
        					break;
        				}
        			}	
        		}
        	}
        }
    };
    
    /* Hide soft keyboard after modifying device name successfully*/
    public static void hideSoftKeyboard(Activity activity) {
    	if (activity.getCurrentFocus() != null) {
    		InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        	inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    	}
    }
	
    /* Handle Textview Click Event */
	public void onClickText(View v) 
	{
		/*if (v.getId() == R.id.text1) {
			// EQ mode selection
			ShowDialog((int)3);
		} else
		*/
		SharedPreferences sharedPreferences= getSharedPreferences("com.issc.isscaudiowidget", 0);
		if (v.getId() == R.id.text2) {
			// Tone selection
			ShowDialog((int)2);
		} /*else if (v.getId() == R.id.text_gpio) {
			// GPIO page
			Intent intent_gpio = null;
			if (!version_demo) intent_gpio = new Intent(ISSCAudioWidget.this, GPIO_Activity.class);
			else               intent_gpio = new Intent(ISSCAudioWidget.this, GPIO_Demo.class);  //for demo
			startActivity(intent_gpio);
		}*//* else if (v.getId() == R.id.text_recorder) {
			Intent intent_gpio = new Intent(ISSCAudioWidget.this, RecorderMain.class);
			startActivity(intent_gpio);
		}*/ else if (v.getId() == R.id.text_onetap) {
			// One tap page
			ShowDialog((int)4);
		} else if (v.getId() == R.id.text_tts) {
			Intent intent_tts = new Intent(ISSCAudioWidget.this, IsscTtsDemo.class);
			startActivity(intent_tts);
		}
		else if (v.getId() == R.id.text_EqConfig) {
			if (sharedPreferences.getBoolean("EqEnabled", false)) {
				Intent intent_EqPreset = new Intent(ISSCAudioWidget.this, IsscEqPreset.class);
				startActivity(intent_EqPreset);
			}
			else {
				CustomToast.showToast(getBaseContext(), "Equalizer Switch is not enabled", 3000);
			}
		}
	}
	
	public void startOneTap() {
		// It would be no use if device is connecting another accessory
		if ( ((Bluetooth_Conn) getApplication()).isHeadset() ) {
			CustomToast.showToast(getBaseContext(), "Another accessory is connecting", 3000);
		} else {
			Intent intent_onetap = new Intent(ISSCAudioWidget.this, Onetap_Activity.class);
			startActivity(intent_onetap);
		}
	}
	
	/* Send Tone function */
	public void initial_SendTone() {	
		mp1 = MediaPlayer.create(getApplicationContext(), R.raw.buzzer_1);
        mp1.setVolume(init_vol, init_vol);
		
		sendtone_send = (Button) findViewById(R.id.sendtone_btn1);
		sendtone_send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
            	if (click == false) { 
            		ShowDialog((int)1);
            	}
            	else {
            		if (play == false) {
                		play = true;
                		try {
                			int result = au.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                			if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                				if (D) Log.v(TAG,"[AudioFocus] Request Fail");
                			}
                			if(mp1 != null) mp1.stop();       
    						mp1.prepare();
    						mp1.start();
    					} catch (IllegalStateException e) {
    						e.printStackTrace();
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
                	} else {
                		CustomToast.showToast(getBaseContext(), "Tone is playing", 2000);
                	}
            	}
            }
        });
		
		sendtone_stop = (Button) findViewById(R.id.sendtone_btn2);
		sendtone_stop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (play == true) {
					play = false;
					mp1.stop();
					
					int result = au.abandonAudioFocus(focusListener);
					if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
						if (D) Log.v(TAG,"[AudioFocus] Request abandon Fail");
					}
				}					
			}
		});
		
		sendtone_bar1 = (SeekBar) findViewById (R.id.seekbar1);
		sendtone_bar1.setProgress((int) (init_vol * 100));	    
		sendtone_bar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mp1.setVolume((float) pro / 100, (float) pro / 100);
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				pro = progress;
			}
		});
		
		mp1.setOnCompletionListener(this);		
		mp1.setOnErrorListener(this);
	}
	
	/* Dialogs, implemented in Dialogs.java */
	public void ShowDialog(int i) {
		if (i == 1) { 
			Dialogs dialog1 = Dialogs.newInstance(Dialogs.Send_Tone_Alert);
			dialog1.show(getFragmentManager(), "Warning");
		}
		else if (i == 2) { 
			Dialogs dialog2 = Dialogs.newInstance(Dialogs.Send_Tone_Menu);
			dialog2.show(getFragmentManager(), "Select a ring tone");
		}
		else if (i == 3) { 
			Dialogs dialog3 = Dialogs.newInstance(Dialogs.Equalizer_Mode);
			dialog3.show(getFragmentManager(), "Select a mode");
		}
		else if (i == 4) { 
			Dialogs dialog4 = Dialogs.newInstance(Dialogs.OneTapPairing);
			dialog4.show(getFragmentManager(), "Notice");
		}
	}

	/* Handle Equalizer  Switch actions */
	public OnCheckedChangeListener Eq_checklistener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
									 boolean isChecked) {

			SharedPreferences s = getSharedPreferences("com.issc.isscaudiowidget", 0);
			Editor editor = s.edit();
			editor.putBoolean("EqEnabled", isChecked);
			editor.commit();
			if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true )
			{
				if (switch_eq_back == false) {
					switch_eq_back = true;
					if (isChecked) {
						if (D) Log.d(TAG,"[Switch] Equalizer Tx on");
						int mode = 0xA; //custom or user defined mode always
						sendEQ(mode);
					} else {
						if (D) Log.d(TAG,"[Switch] Equalizer Tx off");
						sendEQ(0); // Eq off mode.
					}
				}
			}
			else
			{
				if (D) Log.d(TAG,"[Switch] Eq_checklistener Equalizer Tx off");
				Eq_Sw_Disable();
			}
		}
	};
	
	/* Handle Noise Cancellation Switch actions */
	public OnCheckedChangeListener noise_checklistener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true )
			{
				if (switch_back == false) {
					switch_back = true;
					if (isChecked) {
						if (D) Log.d(TAG,"[Switch] Noise Tx on, next cmd: Rx on");
						sendNR((byte)0x1d, (byte)0x1b, (byte)0x1d, (byte)0x18);
					} else {
						if (D) Log.d(TAG,"[Switch] Noise Tx off, next cmd: Rx off");
						sendNR((byte)0x1d, (byte)0x1c, (byte)0x1d, (byte)0x19);
					}
				}
			}
		}	
	};
	
	/* Send NR command */
	public void sendNR(byte cmd, byte para, byte next, byte next_para) {
		byte [] buffer = new byte[6];
		buffer[0] = (byte) 0xaa;  buffer[1] =  0x00;
		buffer[2] =  0x02;        buffer[3] =  cmd;
		buffer[4] =  para;        buffer[5] = (byte) (0x100 - 0x02 - cmd - para);
		
		((Bluetooth_Conn) getApplication()).setCurCmd(cmd);
		((Bluetooth_Conn) getApplication()).setCurCmdPara(para);
		((Bluetooth_Conn) getApplication()).setNextCmd(next);
		((Bluetooth_Conn) getApplication()).setNextCmdPara(next_para);
		((Bluetooth_Conn) getApplication()).write(buffer);
	}
	
	/* Set EQ mode command*/
	protected void sendEQ(int which) {
		if (D) Log.d(TAG,"send EQ, mode: "+which);
		int checksum = 0x100;
		checksum = checksum - 2 - 0x1c - which;
			
		byte [] buffer = new byte[6];
		buffer[0] = (byte) 0xaa;   buffer[1] =  0x00;
		buffer[2] =  0x02;         buffer[3] =  0x1c;
		buffer[4] =  (byte) which; buffer[5] =  (byte) checksum;
			
		((Bluetooth_Conn) getApplication()).setCurCmd(buffer[3]);
		((Bluetooth_Conn) getApplication()).setCurCmdPara(buffer[4]);
		((Bluetooth_Conn) getApplication()).setReplyScreen(true);
		((Bluetooth_Conn) getApplication()).write(buffer);
	}
	
	/* Handle Modify Device Name Button */
	public OnClickListener name_clicklistener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			String s = edit1.getText().toString();
			if (D) Log.d(TAG,s+", Length of device: "+s.length()); 
			if (s.length() == 0) {
				CustomToast.showToast(getBaseContext(), "Device Name cannot be null", 2000);
			} else {
				if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
					setName(s);
				}
			}
		}
	};
	
	public void setName(String name) {		
		byte [] buffer = new byte [name.length()+5];
		if (D) Log.d(TAG,"length of byte array: "+buffer.length);
		buffer[0] = (byte) 0xaa;             buffer[1] = 0x00;
		buffer[2] = (byte) (name.length() + 1); buffer[3] = 0x05;
		int index = 0; int checksum = 0x2000 - name.length() - 1 - 5;
		while (index < name.length()) {
			buffer[4+index] = (byte) name.charAt(index);
			checksum -= buffer[4+index];
			index++;
		}
		buffer[4+index] = (byte)(checksum % 0x100);
				
		((Bluetooth_Conn) getApplication()).setCurCmd(buffer[3]);
		((Bluetooth_Conn) getApplication()).setCurCmdPara((byte)0x00);
		((Bluetooth_Conn) getApplication()).setReplyScreen(true);
		((Bluetooth_Conn) getApplication()).write(buffer);
	}
	
	/* Handle Voice Prompt Switch Button */
	public OnClickListener voicePromptSwitch_clicklistener = new OnClickListener() {
		@Override
		public void onClick(View v) { 
			if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
				byte [] buffer = new byte [7];
				buffer[0] = (byte) 0xaa;	buffer[1] = (byte) 0x00;
				buffer[2] = (byte) 0x03;	buffer[3] = (byte) 0x02;
				buffer[4] = (byte) 0x00;	buffer[5] = (byte) 0x63;
				buffer[6] = (byte) 0x98;
				
				((Bluetooth_Conn) getApplication()).setCurCmd(buffer[3]);
				((Bluetooth_Conn) getApplication()).setCurCmdPara((byte)0x00);
				((Bluetooth_Conn) getApplication()).setReplyScreen(false);
				((Bluetooth_Conn) getApplication()).write(buffer);
			}
		}
	};
	
	/* Handle Battery Status Button */
	public OnClickListener batteryStatus_clicklistener = new OnClickListener() {
		@Override
		public void onClick(View v) { 
			if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
				byte [] buffer = new byte [7];
				buffer[0] = (byte) 0xaa;	buffer[1] = (byte) 0x00;
				buffer[2] = (byte) 0x03;	buffer[3] = (byte) 0x02;
				buffer[4] = (byte) 0x00;	buffer[5] = (byte) 0x6a;
				buffer[6] = (byte) 0x91;
				
				((Bluetooth_Conn) getApplication()).setCurCmd(buffer[3]);
				((Bluetooth_Conn) getApplication()).setCurCmdPara((byte)0x00);
				((Bluetooth_Conn) getApplication()).setReplyScreen(false);
				((Bluetooth_Conn) getApplication()).write(buffer);
			}
		}
	};
	
	/* Handle DRC Prev Button */
	public OnClickListener drc_prev_clicklistener = new OnClickListener() {
		@Override
		public void onClick(View v) { 
			if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
				byte [] buffer = new byte [7];
				buffer[0] = (byte) 0xaa;	buffer[1] = (byte) 0x00;
				buffer[2] = (byte) 0x03;	buffer[3] = (byte) 0x02;
				buffer[4] = (byte) 0x00;	buffer[5] = (byte) 0x3d;
				buffer[6] = (byte) 0xbe;
				
				((Bluetooth_Conn) getApplication()).setCurCmd(buffer[3]);
				((Bluetooth_Conn) getApplication()).setCurCmdPara((byte)0x00);
				((Bluetooth_Conn) getApplication()).setReplyScreen(false);
				((Bluetooth_Conn) getApplication()).write(buffer);
			}
		}
	};
	
	/* Handle DRC Next Button */
	public OnClickListener drc_next_clicklistener = new OnClickListener() {
		@Override
		public void onClick(View v) { 
			if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
				byte [] buffer = new byte [7];
				buffer[0] = (byte) 0xaa;	buffer[1] = (byte) 0x00;
				buffer[2] = (byte) 0x03;	buffer[3] = (byte) 0x02;
				buffer[4] = (byte) 0x00;	buffer[5] = (byte) 0x3c;
				buffer[6] = (byte) 0xbf;
				
				StringBuilder sb = new StringBuilder();
				for (byte b : buffer) {
				    sb.append(String.format("%02X ", b));
				}
				if (D) Log.d(TAG,"Send: "+sb.toString());
				
				((Bluetooth_Conn) getApplication()).setCurCmd(buffer[3]);
				((Bluetooth_Conn) getApplication()).setCurCmdPara((byte)0x00);
				((Bluetooth_Conn) getApplication()).setReplyScreen(false);
				((Bluetooth_Conn) getApplication()).write(buffer);
			}
		}
	};
	
	/* RD version */
	/*public OnClickListener btn_con_spp_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Log.d(TAG,"[RD version] connect with hsp btn click");
			if ( !((Bluetooth_Conn) getApplication()).getSppStatus() ) {
				BluetoothDevice Device = ((Bluetooth_Conn) getApplication()).get_HSP_device();
				if ( Device != null ) {
					((Bluetooth_Conn) getApplication()).setHeadset(true);
            		((Bluetooth_Conn) getApplication()).SetSpp(Device);
				}
			}
		}
	};
	
	public OnClickListener btn_con_spp_a2dp_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Log.d(TAG,"[RD version] connect with a2dp btn click");
			if ( !((Bluetooth_Conn) getApplication()).getSppStatus() ) {
				BluetoothDevice Device = ((Bluetooth_Conn) getApplication()).get_HSP_device();
				if ( Device != null ) {
					((Bluetooth_Conn) getApplication()).setHeadset(true);
            		((Bluetooth_Conn) getApplication()).SetSpp(Device);
				}
			}
		}
	};
	
	public OnClickListener btn_server_socket_state_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Log.d(TAG,"[RD version] btn_server_socket_state click");
			if ( ((Bluetooth_Conn) getApplication()).mServerSocketThread == null ) {
				CustomToast.showToast(getBaseContext(), "mServerSocketThread NULL", 3000);
			} else {
				CustomToast.showToast(getBaseContext(), "mServerSocketThread not NULL", 3000);
			}
		}
	};*/
	/* RD version */
	
	OnAudioFocusChangeListener focusListener = new OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			if (D) Log.i(TAG,"[FocusChangeListener] Focus: "+focusChange);
		}
    };
	
	@Override
	protected void onDestroy() {
	   super.onDestroy();
	   unregisterReceiver(mBroadcast);
	   //mp1.release();
	   if (D) Log.d(TAG,"[Main Activity] Activity onDestroy");
	}

	@Override
	protected void onStop() {
		super.onStop();
		//this need to be moved to required place after usage of Eqlastmode - sateesh
		SharedPreferences s = getSharedPreferences("com.issc.isscaudiowidget", 0);
		Editor editor = s.edit();
		editor.putInt("Eqlastmode", 0xA);
		editor.commit();
		//unregisterReceiver(mBroadcast);
		//mp1.release();

		if (D) Log.d(TAG,"[Main Activity] Activity onStop");
	}
	
	@Override
	protected void onResume() {
	   super.onResume();
	   if (D) Log.d(TAG,"[Main Activity] onResume");
	   if (this.getCurrentFocus() != null) this.getCurrentFocus().clearFocus();
		if (!((Bluetooth_Conn) getApplication()).getAdapter().isEnabled() && !enable_bt) {
			//CustomToast.showToast(getBaseContext(), "Please enable your BT and re-run this program", 5000);
            //finish();
		    //Enable Bluetooth if it is disabled
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
		    enable_bt = true;
        }
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		play = false;
		int result = au.abandonAudioFocus(focusListener);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			if (D) Log.v(TAG,"[AudioFocus] Request abandon Fail");
		}
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		try { 
        	mp1.reset(); 
        	int result = au.abandonAudioFocus(focusListener);
    		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
    			if (D) Log.v(TAG,"[AudioFocus] Request abandon Fail");
    		}
        } catch (Exception e) { 
        	e.printStackTrace();   
        } 
		return false;
	}
}
