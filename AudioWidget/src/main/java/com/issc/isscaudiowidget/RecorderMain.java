package com.issc.isscaudiowidget;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class RecorderMain extends Activity implements OnCompletionListener, OnErrorListener {
	
	private boolean D = false;
	private static final String TAG = "RecordTest";

    private static String mFileName = "";
    private String FilePath = "";
    
    ListView listViewRecords;
    ArrayList<HashMap<String, Object>> mList = null;
    MyAdapter mAdapter;
    
    boolean mStartRecording = true;
    boolean mStartPlaying = true;
    boolean has_sdcard = false;
    boolean voiceRecognitionOn = false;

    private ImageButton mRecordButton = null;
    private ImageButton  mPlayButton = null;
    private ImageView headset_status = null;
    private ImageView SPP_status = null;
    private TextView text_playing = null;
    private EditText edit1 = null;
    
    private MediaRecorder mRecorder = null;
    private MediaPlayer   mPlayer = null;
    
    private BluetoothAdapter adapter = null;
    private BluetoothHeadset Headset = null;
	private BluetoothDevice  Device = null;   
	
	private AudioManager au = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recorder_main);
		
		mRecordButton = (ImageButton) findViewById (R.id.record);
		mRecordButton.setOnClickListener(record_listener);
		mPlayButton = (ImageButton) findViewById (R.id.play);
		mPlayButton.setOnClickListener(play_listener);
		
		headset_status = (ImageView) findViewById (R.id.record_status);
		SPP_status = (ImageView) findViewById (R.id.record_spp_status);
		listViewRecords = (ListView) findViewById (R.id.listViewRecords);
		text_playing = (TextView) findViewById (R.id.record_playing);
        
        String s = getSdcardPath();
        if (s == null) {
        	File fd = getDir("RecordTest", Context.MODE_PRIVATE);
        	FilePath = fd.toString() + "/";
        	if (D) Log.d(TAG,"No sdcard, internal storage path: "+FilePath);
        } else {
        	has_sdcard = true;
        	FilePath = s + "RecorderTest/";
        }
        
        File recordPath = new File(FilePath);
        
        if ( !recordPath.exists() )
        	recordPath.mkdir();

        putDataToListView();
        
        adapter = BluetoothAdapter.getDefaultAdapter();
        
        if ( (((Bluetooth_Conn) getApplication()).getSppStatus()) == true ) {
        	headset_status.setImageResource(R.drawable.connect);
			SPP_status.setImageResource(R.drawable.datatransmission);
			adapter.getProfileProxy(RecorderMain.this, mProfileListener, BluetoothProfile.HEADSET);
		} else {
			if ( ((Bluetooth_Conn) getApplication()).isHeadset() ) {
				headset_status.setImageResource(R.drawable.connect);
				adapter.getProfileProxy(RecorderMain.this, mProfileListener, BluetoothProfile.HEADSET);
			}
		}
        
        au = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("Headset_Disconnect");
		intentFilter.addAction("Headset_Connect");
		intentFilter.addAction("SPP_disconnect");
		intentFilter.addAction("SPP_setup");
		intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
		registerReceiver(mBroadcast, intentFilter);
	}
	
	private BluetoothProfile.ServiceListener mProfileListener = 
            new BluetoothProfile.ServiceListener() { 
		@Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
        	if (profile == BluetoothProfile.HEADSET) { 
        		if (D) Log.d(TAG,"on ProfileListener : HEADSET");
        		
                Headset = (BluetoothHeadset) proxy; 
                List<BluetoothDevice> connectedDevices = proxy.getConnectedDevices();
                
                for (BluetoothDevice device : connectedDevices) {
                	Device = device;
                	if (D) Log.d(TAG,"BluetoothDevice found :" + device + Headset.getConnectionState(Device));
                }
            }
        }
		@Override
		public void onServiceDisconnected(int profile) {
			// TODO Auto-generated method stub
			if (profile == BluetoothProfile.HEADSET)
				Headset = null;
		}
	};
	
	private BroadcastReceiver mBroadcast =  new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (arg1.getAction().equals("Headset_Connect")) {
				if (D) Log.i(TAG,"Receive: Headset_Connect");
        		if (headset_status != null) headset_status.setImageResource(R.drawable.connect);
        		adapter.getProfileProxy(RecorderMain.this, mProfileListener, BluetoothProfile.HEADSET);
        	} else if (arg1.getAction().equals("Headset_Disconnect")) {
        		if (D) Log.i(TAG,"Receive: Headset_Disconnect");
        		if (headset_status != null) headset_status.setImageResource(R.drawable.disconnect);
        		adapter.closeProfileProxy(BluetoothProfile.HEADSET, Headset);
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
        	} else if (arg1.getAction().equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
				int state = arg1.getIntExtra(BluetoothHeadset.EXTRA_STATE, -123);
				if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
					if (D) Log.d(TAG, "Audio Connected");
					
					mRecorder = new MediaRecorder();
			        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
			        mRecorder.setMaxDuration(180000);  // Maximum time for record, 3 minutes
			        String s;
			        if (has_sdcard) s = getSdcardPath() + "RecorderTest/" + edit1.getText().toString() + ".3gp";
			        else            s = FilePath + edit1.getText().toString() + ".3gp";
			        if (D) Log.d(TAG,"File with its path: "+s);
			        mRecorder.setOutputFile(s);

			        try {
			            mRecorder.prepare();
			        } catch (IOException e) {
			            Log.e(TAG, "MediaRecorder prepare() failed when VoiceRecogmition enabled");
			        }
			        
			        mRecorder.start();
				} else if (state == BluetoothHeadset.STATE_AUDIO_CONNECTING) {
					if (D) Log.d(TAG, "Audio Connecting");
				} else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
					if (D) Log.d(TAG, "Audio Disconnected");
				}
			}
		}
	};
	
	private void getData() {
		ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
		File recordPath = new File(FilePath);
		
		for ( File f:recordPath.listFiles() ) {
        	if (f.isFile()) {
        		HashMap<String, Object> map = new HashMap<String, Object>();
        		String s = f.getName().substring(0, f.getName().lastIndexOf("."));
        		map.put("ItemName", s);
        		Date lastMod = new Date(f.lastModified());
        		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.CHINESE);
        		String str = formatter.format(lastMod);
        		map.put("ItemInfo", str.toString());
        		list.add(map);
        	}
        }
        mList = list;
    }
	
	protected void putDataToListView()
	{
		getData();
		if (!mList.isEmpty())
			text_playing.setText((mList.get(0).get("ItemName")).toString());
	
		mAdapter = new MyAdapter(
			this,
			mList,
			R.layout.mylistview,
			new String[] {"ItemName", "ItemInfo","ItemButton"},
			new int[] {R.id.record_title,R.id.record_info,R.id.record_btn}
		);
		
		listViewRecords.setAdapter(mAdapter);
		
		listViewRecords.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	            if (D) Log.d(TAG, "ListViewItem = " + id + ", " + mList.get(position).get("ItemName"));
	            mFileName = FilePath + mList.get(position).get("ItemName") + ".3gp";
	            text_playing.setText((mList.get(position).get("ItemName")).toString());
	        }
	    });
	}
	
	public void updateListView() {
		getData();
		mAdapter.updateList(mList);
		mAdapter.notifyDataSetChanged();
		if (mList.size() == 1) {
			text_playing.setText((mList.get(0).get("ItemName")).toString());
			mFileName = FilePath + mList.get(0).get("ItemName") + ".3gp";
		} else if (!mList.isEmpty()) {
			if (mFileName == null) {
				text_playing.setText((mList.get(0).get("ItemName")).toString());
				mFileName = FilePath + mList.get(0).get("ItemName") + ".3gp";
			}
		} else {
			text_playing.setText("No records");
			mFileName = null;
		}
	}
	
	public static String getSdcardPath() {
	    File sdDir = null;
	    boolean sdCardExist = Environment.getExternalStorageState().equals(
	        android.os.Environment.MEDIA_MOUNTED); 
	    if (sdCardExist) {
	        sdDir = Environment.getExternalStorageDirectory();
	        return sdDir.toString() + "/";
	    } else 
	    	return null;
	}
	
	private void onRecord(boolean start) {
        if (start) {
        	if (mStartPlaying) {
        		startRecording();
        	} else {
        		if (D) Log.d(TAG,"[Recording] Phone is currently Playing");
        	}
        } else {
            stopRecording();
        }
    }
	
	private void onPlay(boolean start) {
    	if (mList.size() > 0) {
	        if (start) {
	        	if (mStartRecording) {
	        		startPlaying();
	        	} else {
	            	if (D) Log.d(TAG,"[Playing] Phone is currently recording");
	            }
	        } else {
	            stopPlaying();
	        }
    	} else {
    		if (D) Log.d(TAG,"No records can be play!");
    	}
    }

    private void startPlaying() {
	    mPlayer = new MediaPlayer();
	    mPlayer.setOnCompletionListener(this);
	    mPlayer.setVolume(90, 90);
	    try {
	    	if (has_sdcard) {
	    		mPlayer.setDataSource(mFileName);
	    	} else {
	    		File aa = new File(mFileName);
	    		FileInputStream fis = new FileInputStream(aa);
	    		mPlayer.setDataSource(fis.getFD());
	    	}
	    	
	    	int result = au.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				if (D) Log.v(TAG,"[AudioFocus] Request Fail");
			}
	        
	        mPlayer.prepare();
	        mPlayer.start();
	            
	        if (mStartPlaying) {
	          	mPlayButton.setImageResource(R.drawable.stop);
	        } else {
	           	mPlayButton.setImageResource(R.drawable.play);
	        }
	        mStartPlaying = !mStartPlaying;
	            
	    } catch (IOException e) {
	        Log.e(TAG, "MedaiPlayer prepare() failed");
	    }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        
        int result = au.abandonAudioFocus(focusListener);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			if (D) Log.v(TAG,"[AudioFocus] Request abandon Fail");
		}
		
        if (mStartPlaying) {
        	mPlayButton.setImageResource(R.drawable.stop);
        } else {
        	mPlayButton.setImageResource(R.drawable.play);
        }
        mStartPlaying = !mStartPlaying;
    }

    private void startRecording() {
    	AlertDialog.Builder dialog1 = new AlertDialog.Builder(RecorderMain.this);
    	dialog1.setTitle("Start Record");
    	dialog1.setMessage("Enter record file name");
    	
    	
    	SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.CHINESE);
		Date curDate = new Date(System.currentTimeMillis());
		String str = formatter.format(curDate);
		
		edit1 = new EditText(this);
    	edit1.setText(str);
    	dialog1.setView(edit1);
    	
    	dialog1.setPositiveButton("Record", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				int result = au.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
				if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
					if (D) Log.v(TAG,"[AudioFocus] Request Fail");
				}
				
				if (mStartRecording) {
					mRecordButton.setImageResource(R.drawable.stop);
	            } else {
	            	mRecordButton.setImageResource(R.drawable.rec);
	            }
	            mStartRecording = !mStartRecording;
	            
	            if ( ((Bluetooth_Conn) getApplication()).isHeadset() ) {
	            	if (D) Log.i(TAG,"Initial VoiceRecognition if headset is connected");
	    			Boolean value = Headset.startVoiceRecognition(Device);
	    			if (!value) {
	    				if (D) Log.d(TAG,"Device does NOT support Voice Recognition");
	    				CustomToast.showToast(getBaseContext(), "Headset does not support bluetooth recording", 2500);
	    			} else { 
	    				if (D) Log.d(TAG,"Device support Voice Recognition");
	    			}
	    			voiceRecognitionOn = true;
	            } else {
	            	CustomToast.showToast(getBaseContext(), "No headset connected. Use default MIC.", 2500);
	            	mRecorder = new MediaRecorder();
			        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
			        mRecorder.setMaxDuration(180000);  // Maximum time for record, 3 minutes
			        String s;
			        if (has_sdcard) s = getSdcardPath() + "RecorderTest/" + edit1.getText().toString() + ".3gp";
			        else            s = FilePath + edit1.getText().toString() + ".3gp";
			        if (D) Log.d(TAG,"File with its path: "+s);
			        
			        mRecorder.setOutputFile(s);
			        try {
			            mRecorder.prepare();
			        } catch (IOException e) {
			            Log.e(TAG, "MediaRecorder prepare() failed when default");
			        }
			        
			        if (D) Log.d(TAG,"record start");
			        mRecorder.start();
	            }
			}
		});
    	
    	dialog1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
    	
    	AlertDialog alertDialog = dialog1.create();
    	alertDialog.show();
    }

    private void stopRecording() {
    	if (D) Log.v(TAG,"[MediaRecoder] Stop");
    	if (mStartRecording) {
    		mRecordButton.setImageResource(R.drawable.stop);
        } else {
        	mRecordButton.setImageResource(R.drawable.rec);
        }
        mStartRecording = !mStartRecording;
    	
        if (mRecorder != null) {
	        mRecorder.stop();
	        mRecorder.reset();
	        mRecorder.release();
	        mRecorder = null;
        }
        
        if (voiceRecognitionOn) {
			if (D) Log.i(TAG,"stopVoiceRecognition");
			Headset.stopVoiceRecognition(Device);
		}
        
        int result = au.abandonAudioFocus(focusListener);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			if (D) Log.v(TAG,"[AudioFocus] Request abandon Fail");
		}
        
        updateListView();
    }
    
    OnAudioFocusChangeListener focusListener = new OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			if (D) Log.i(TAG,"[FocusChangeListener] Focus: "+focusChange);
		}
    };
    
    public void myDialog(final String str) {
    	if (D) Log.d(TAG, "myDialog = " + str);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete confirmation")
               .setMessage("Sure to delete record:\n    " + str )
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                	   if (!mStartPlaying) {
                		   onPlay(mStartPlaying);
                	   }
                	   if (D) Log.d(TAG, "Delete confirm " + str);
	                   String s = FilePath + str + ".3gp";
	                   if (D) Log.d(TAG,"delete name: "+s);
	                   File file = new File(s);
	                   boolean deleted = file.delete();
	                   if (deleted) {
	                       if (str.equals(text_playing.getText().toString()))
	                       mFileName = null;
	                       updateListView();
	                       if (D) Log.d(TAG, "deleted: "+deleted);
	                   }
                   }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                	   if (D) Log.d(TAG, "Delete regret " + str);
                   }
               });
     
        AlertDialog ad = builder.create();
        ad.show();
    }
    
    public OnClickListener record_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			onRecord(mStartRecording);
		}
    };
    
    public OnClickListener play_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (D) Log.v(TAG,"play click");
			onPlay(mStartPlaying);
		}
    };
    
    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		if (D) Log.d(TAG,"MediaPlayer onComplete");
		mPlayer.release();
        mPlayer = null;
        if (mStartPlaying) {
        	mPlayButton.setImageResource(R.drawable.stop);
        } else {
        	mPlayButton.setImageResource(R.drawable.play);
        }
        mStartPlaying = !mStartPlaying;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		if (D) Log.d(TAG,"MediaPlayer onError");
		return false;
	}
	
	@Override
	protected void onDestroy() {
		if (voiceRecognitionOn) Headset.stopVoiceRecognition(Device);
		adapter.closeProfileProxy(BluetoothProfile.HEADSET, Headset);
		unregisterReceiver(mBroadcast);
	    if (mPlayer != null)    mPlayer.release();
	    if (mRecorder != null)  mRecorder.release();
	    if (D) Log.d(TAG,"Destroy, UnregisterReceiver, release audio resource");
	    super.onDestroy();
	}

}