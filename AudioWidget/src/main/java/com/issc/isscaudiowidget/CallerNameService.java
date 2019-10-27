package com.issc.isscaudiowidget;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallerNameService extends Service 
{
    private static boolean D = true;
	private static final String TAG = "TTSdemo [Service]";
	private static String mFileName = "";
    private String FilePath = "";
    private int sdkNumber;
    TextToSpeech tts = null;
	HashMap<String, String> myHashAlarm;
	
	private convertVoicePrompt mConvertVoicePrompt = null;
	private synthesizeWav mSynthesizeWav = null;
	private String Number = null;
	private int phoneState = TelephonyManager.CALL_STATE_IDLE;
	
	private SharedPreferences sharedPreferences;
	@Override
	public void onCreate()
	{
		MyPhoneStateListener myPhoneStateListener = new MyPhoneStateListener();
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		sharedPreferences = getSharedPreferences("com.issc.isscaudiowidget", 0);
		myHashAlarm = new HashMap<String, String>();
		
		tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			
			@Override
			public void onInit(int status) {
				if (status == TextToSpeech.SUCCESS)
				{
					if (D) Log.v(TAG,"TTS OnInit success");
					tts.setSpeechRate((float) 0.85);
					/* Avoid the default TTS language is not English (US) */
					tts.setLanguage(Locale.US);
					
					int tt = tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
						@Override
						public void onDone(String utteranceId) {
							if (D) Log.d(TAG,"[TTS] on Done " + utteranceId);
							/* convert wav format to voice prompt may need lots of time, use thread to handle */
							mConvertVoicePrompt = new convertVoicePrompt(utteranceId);
							mConvertVoicePrompt.start();
						}

						@Override
						public void onError(String utteranceId) {
							if (D) Log.d(TAG,"[TTS] on Error " + utteranceId);
						}

						@Override
						public void onStart(String utteranceId) {
							if (D) Log.d(TAG,"[TTS] on Start " + utteranceId);
						}
				    });
					if (D) Log.v(TAG,"TTS listener status: " + tt);
				}
			}
		});
	    
	    String s = getSdcardPath();
	    if (s == null) {
	    	Log.e(TAG,"The device must have external storage to temporarily save tts files");
	    	((Bluetooth_Conn) getApplication()).setHasSD(false);
	    }
	    FilePath = s + "TTSTest/";
        
        File TTSdir = new File(FilePath);
        if (!TTSdir.exists())
        	TTSdir.mkdir();
        
        //register broadcast receiver
      	IntentFilter intentFilter = new IntentFilter();
      	intentFilter.addAction("synthesizeTXT");
      	intentFilter.addAction("ShowContact");
      	intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
      	registerReceiver(mReceiver, intentFilter);
	}
    
    public class convertVoicePrompt implements Runnable {
		private Thread thread = null;
		private String ttsid;
		
		public convertVoicePrompt(String Id) {
			this.thread = new Thread(this);
			ttsid = Id;
		}
		
		public void start() {
    		this.thread.start();
    	}
		
		@Override
		public void run() {
			int result = toVoicePrompt();
	        if (D) Log.i(TAG,"toVoicePrompt Result: "+result);
	        Intent i = new Intent();
	    	i.setAction("synthesizeSuccess");
	    	sendBroadcast(i);
	    	((Bluetooth_Conn) getApplication()).setSynthesizing(false);
	        if ( ((Bluetooth_Conn) getApplication()).getSppStatus() ) {
	        	if (ttsid.equals("0") && phoneState != TelephonyManager.CALL_STATE_RINGING) {
	        		/* do not read caller name if phone is not ringing */
	        		if (D) Log.d(TAG,"Phone is not ringing, no caller name speaking");
	        	} else {
	        		if (D) Log.d(TAG,"SendVoicePrompt");
	        		if (sharedPreferences.getBoolean("SendTTS", false)) {
		        		((Bluetooth_Conn) getApplication()).startSendVoicePrompt();
					}
	        	} 
	        }
	        mConvertVoicePrompt = null;
		}
	}
    
    public class synthesizeWav implements Runnable {
		private Thread thread = null;
		private String text = null;
		
		public synthesizeWav(String s) {
			this.thread = new Thread(this);
			text = s;
		}
		
		public void start() {
    		this.thread.start();
    	}
		
		@Override
		public void run() {
			/* To do: Read texts from file to an string buffer */
	        mFileName = FilePath + "tts.wav";
			myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1");
			
			tts.synthesizeToFile(text, myHashAlarm, mFileName);
			mSynthesizeWav = null;
		}
	}
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action  = intent.getAction();
        	if (action.equals("synthesizeTXT")){	
        		/* Synthesize a text to WAV file may need lots of time, use thread to handle */
        		if (  ((Bluetooth_Conn) getApplication()).isHasSD() ) {
        			String s = intent.getStringExtra("Text");
	        		((Bluetooth_Conn) getApplication()).setSynthesizing(true);
	        		mSynthesizeWav = new synthesizeWav(s);
	        		mSynthesizeWav.start();
        		}
        	} else if (action.equals("ShowContact")) {
        		if (Number != null) 
        			ShowContact(Number);
        	} else if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
        		if (D) Log.i(TAG,"New outgoing call");
        		if ( ((Bluetooth_Conn) getApplication()).isSendVoicePromptThread() ) {
					((Bluetooth_Conn) getApplication()).stopSendVoicePrompt();
				}
        	}
        }
    };
    
    public class MyPhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String phoneNumber) {
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				if (D) Log.i(TAG,"PhoneState: IDLE");
				phoneState = TelephonyManager.CALL_STATE_IDLE;
				/* End sendVoicePromptThread when call ended */
				if ( ((Bluetooth_Conn) getApplication()).isSendVoicePromptThread() )
					((Bluetooth_Conn) getApplication()).stopSendVoicePrompt();
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				if (D) Log.i(TAG,"PhoneState: OFFHOOK");
				phoneState = TelephonyManager.CALL_STATE_OFFHOOK;
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				if (D) Log.i(TAG,"PhoneState: RINGING");
				phoneState = TelephonyManager.CALL_STATE_RINGING;
				if ( ((Bluetooth_Conn) getApplication()).isHasSD() ) {
					if ( ((Bluetooth_Conn) getApplication()).isSendVoicePromptThread() ) {
						((Bluetooth_Conn) getApplication()).stopSendVoicePrompt();
						Number = phoneNumber;
					} else {
						ShowContact(phoneNumber);
					}
				}
			default:
				break;
			}
		}
	}
    
    public static String getSdcardPath() {
	    File sdDir = null;
	    boolean sdCardExist = Environment.getExternalStorageState().equals(
	        android.os.Environment.MEDIA_MOUNTED); 
	    if (sdCardExist) {
	      sdDir = Environment.getExternalStorageDirectory();
	      return sdDir.toString() + "/";
	    } else {
	    	return null;
	    }
	    
	}
    
    public void ShowContact(String IncomeNumber) {
    	Number = null;
		Cursor cur = null;		
		try {
            // Query using ContentResolver.query or Activity.managedQuery
            cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cur.moveToFirst()) {
                int idColumn = cur.getColumnIndex(ContactsContract.Contacts._ID);
                int displayNameColumn = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                // Iterate all users
                do {
                	String contactId;
                	String displayName;
                	// Get the field values
                	contactId = cur.getString(idColumn);
                	displayName = cur.getString(displayNameColumn);
                	// Get number of user's phoneNumbers
                	int numberCount = cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                	if (numberCount>0) {
                		Cursor phones = getContentResolver().query(
                			ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                			ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                          	null, null);
                		if (phones.moveToFirst()) {
                			int numberColumn = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                			// Iterate all numbers
                			do {
                				if ( phones.getString(numberColumn).replaceAll(" ", "").equals(IncomeNumber) ) 
                				{
                					/* Find contact, read caller name */
                					if (D) Log.i(TAG,"Find contact: "+displayName);
                					mFileName = FilePath + "tts.wav";
                					myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "0");
                					
                					if ( ((Bluetooth_Conn) getApplication()).isSynthesizing() == false ) {
                						((Bluetooth_Conn) getApplication()).setSynthesizing(true);
                						tts.synthesizeToFile("Call from " + displayName, myHashAlarm, mFileName);
                					}
                					return;
                				}
                			} while (phones.moveToNext());
                		} 
                		if (phones != null) phones.close();
                	}
                } while (cur.moveToNext());
                
                /* No match contact in contact book, speak caller number */
                mFileName = FilePath + "tts.wav";
                myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "0");
                
                if ( ((Bluetooth_Conn) getApplication()).isSynthesizing() == false ) {
                	((Bluetooth_Conn) getApplication()).setSynthesizing(true);
                	String newS = "";
                	for (int i = 0; i < IncomeNumber.length(); i++) {
						newS = newS + IncomeNumber.substring(i, i+1) + " ";
					}
                	tts.synthesizeToFile("Call from " + newS, myHashAlarm, mFileName);
    				if (D) Log.i(TAG,"Call from " + newS);
                }
            } else {
            	/* The contact book is empty, speak caller number */
                mFileName = FilePath + "tts.wav";
                myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "0");
                
                if ( ((Bluetooth_Conn) getApplication()).isSynthesizing() == false ) {
                	((Bluetooth_Conn) getApplication()).setSynthesizing(true);
                   	String newS = "";
                	for (int i = 0; i < IncomeNumber.length(); i++) {
						newS = newS + IncomeNumber.substring(i, i+1) + " ";
					}
                	tts.synthesizeToFile("Call from " + newS, myHashAlarm, mFileName);
    				if (D) Log.i(TAG,"Call from " + newS);
                }
            }
		} finally {
			if (cur != null) cur.close();
		}	
	}
    
    /* Declaration of JNI function */
    public native int toVoicePrompt();

    /* Load JNI library */
    static {
//        System.loadLibrary("native");
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}