package com.issc.isscaudiowidget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;

public class Dialogs extends DialogFragment {
	public static final int Send_Tone_Alert = 1;
	public static final int Send_Tone_Menu  = 2;
	public static final int Equalizer_Mode  = 3;
	public static final int OneTapPairing   = 4;
	
	private boolean D = false;
	private static final String TAG = "Dialogs";
	
	public static Dialogs newInstance(int title) {
		Dialogs mydialogs = new Dialogs();  
        Bundle bundle = new Bundle();  
        bundle.putInt("title", title);  
        mydialogs.setArguments(bundle); 	
        return mydialogs;
	}
	
	@Override  
    public Dialog onCreateDialog(Bundle savedInstanceState) {  
        int args = getArguments().getInt("title");  
 
        switch (args) {  
        case Send_Tone_Alert:  
            return new AlertDialog.Builder(getActivity())
            .setIcon(R.drawable.sendtone)  
            .setTitle(getTag())
            .setMessage("Warning: be sure!\n" +
					"1. Nobody is using the headset\n" +
					"2. Headset volume is low\n")
            .setPositiveButton("Send", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					int result = 
							((ISSCAudioWidget) getActivity()).au.requestAudioFocus(((ISSCAudioWidget) getActivity()).focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
					if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
						if (D) Log.v(TAG,"[AudioFocus] Request Fail");
					}
					
					((ISSCAudioWidget) getActivity()).play = true;
					((ISSCAudioWidget) getActivity()).click = true;
					((ISSCAudioWidget) getActivity()).mp1.start();
				}
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {

				}
			})
            .create();

        case Send_Tone_Menu:
        	final String s2 [] = {"Tone1", "Tone2", "Tone3", "Tone4" , "Tone5"};
        	return new AlertDialog.Builder(getActivity())
            .setIcon(R.drawable.sendtone)  
            .setTitle(getTag())
            .setItems(s2, new DialogInterface.OnClickListener() {
		         public void onClick(DialogInterface dialog, int which) {
		            	
		             float vol = (float) ((ISSCAudioWidget) getActivity()).pro / 100;
		             
		             if(((ISSCAudioWidget) getActivity()).play == true) ((ISSCAudioWidget) getActivity()).play = false;
		             ((ISSCAudioWidget) getActivity()).mp1.reset();
		               
		             switch (which) 
		             {
		             case 0:            		   
		                 ((ISSCAudioWidget) getActivity()).mp1 = MediaPlayer.create(((ISSCAudioWidget) getActivity()).getApplicationContext(), R.raw.tone_1);
		                 break;
		             case 1:            		   
		                 ((ISSCAudioWidget) getActivity()).mp1 = MediaPlayer.create(((ISSCAudioWidget) getActivity()).getApplicationContext(), R.raw.tone_2);
		                 break;
		               case 2:            		   
		            	 ((ISSCAudioWidget) getActivity()).mp1 = MediaPlayer.create(((ISSCAudioWidget) getActivity()).getApplicationContext(), R.raw.tone_3);
		                 break;
		               case 3:            		   
		            	 ((ISSCAudioWidget) getActivity()).mp1 = MediaPlayer.create(((ISSCAudioWidget) getActivity()).getApplicationContext(), R.raw.tone_4);
		            	 break;
		               case 4:            		   
		            	 ((ISSCAudioWidget) getActivity()).mp1 = MediaPlayer.create(((ISSCAudioWidget) getActivity()).getApplicationContext(), R.raw.tone_5);
		            	 break;
		             }
		             ((ISSCAudioWidget) getActivity()).mp1.setOnCompletionListener( (ISSCAudioWidget) getActivity() );
	            	 ((ISSCAudioWidget) getActivity()).mp1.setOnErrorListener( (ISSCAudioWidget) getActivity() );           	 
	            	 ((ISSCAudioWidget) getActivity()).mp1.setVolume(vol, vol);
		             ((ISSCAudioWidget) getActivity()).text_st.setText(s2[which]);
		         }
		    })
            .create();
        	
        /* This case was moved from Dialog to ISSCEqPreset activity to handled custom Equalizer modes
        case Equalizer_Mode:

        	final String s [] = {"Off" , "Soft", "Bass" , "Treble", "Classic", "Rock", 
		                         "Jazz", "POP" , "Dance", "R&B"   , "Custom" };
        	return new AlertDialog.Builder(getActivity())
            .setIcon(R.drawable.equalizer)  
            .setTitle(getTag())
            .setItems(s, new DialogInterface.OnClickListener() {
            	public void onClick(DialogInterface dialog, int which) {        
            		if ( (((Bluetooth_Conn) getActivity().getApplication()).getSppStatus()) == true ) {
            			SendSppEQ(which);
            		}
            	}
            })
            .create();*/
        	
        case OneTapPairing:
        	return new AlertDialog.Builder(getActivity())
            .setIcon(R.drawable.onetap)  
            .setTitle(getTag())
            .setMessage("Warning: be sure!\n" +
					"1. Your accessory is in pairing mode and next to your device\n" +
					"2. No accessory is connected")
            .setPositiveButton("Start", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					((ISSCAudioWidget)getActivity()).startOneTap();
				}
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
			})					
            .create();
        }
        return null;
    }

	/*protected void SendSppEQ(int which) {
		if (D) Log.d(TAG,"Send EQ, mode: "+which);
		// Set EQ mode
		int checksum = 0x100;
		checksum = checksum - 2 - 0x1c - which;
			
		byte [] buffer = new byte[6];
		buffer[0] = (byte) 0xaa;   buffer[1] =  0x00;
		buffer[2] =  0x02;         buffer[3] =  0x1c;
		buffer[4] =  (byte) which; buffer[5] =  (byte) checksum;
			
		((Bluetooth_Conn) getActivity().getApplication()).setCurCmd(buffer[3]);
		((Bluetooth_Conn) getActivity().getApplication()).setCurCmdPara(buffer[4]);
		((Bluetooth_Conn) getActivity().getApplication()).setReplyScreen(true);
		((Bluetooth_Conn) getActivity().getApplication()).write(buffer);
	}*/
}