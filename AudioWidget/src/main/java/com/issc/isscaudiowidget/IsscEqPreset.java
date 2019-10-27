package com.issc.isscaudiowidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.ImageView;

public class IsscEqPreset extends FragmentActivity {

    private boolean D = true;
    private static final String TAG = "EqualizePreset";

    ImageView headset_status = null;
    ImageView SPP_status = null;
    private FragmentManager fragmentManager = null;
    private EqualizerFragment eqFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eq_preset);

        CustomToast.showToast(getBaseContext(), "Equalizer Preset!", 3000);

        fragmentManager = getSupportFragmentManager();
        eqFragment = new EqualizerFragment();

        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();
        fragmentTransaction.add(R.id.eqMainRelativeLayout, eqFragment);
        fragmentTransaction.commit();

        headset_status = (ImageView) findViewById (R.id.eq_status);
        SPP_status = (ImageView) findViewById (R.id.eq_spp_status);

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

            fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager
                    .beginTransaction();

            if (action.equals("Headset_Connect")) {
                if (D) Log.i(TAG, "Receive: Headset_Connect");
                if (headset_status != null) headset_status.setImageResource(R.drawable.connect);
            } else if (action.equals("Headset_Disconnect")) {
                if (D) Log.i(TAG,"Receive: Headset_Disconnect");
                if (headset_status != null) headset_status.setImageResource(R.drawable.disconnect);
                finish();
            } else if (action.equals("SPP_setup")) {
                if (D) Log.i(TAG,"Receive: SPP_Connect");
                if(headset_status != null) headset_status.setImageResource(R.drawable.connect);
                if(SPP_status != null) SPP_status.setImageResource(R.drawable.datatransmission);
            } else if (action.equals("SPP_disconnect")) {
                if (D) Log.i(TAG, "Receive Command ACK: SPP_disconnect");
                if (((Bluetooth_Conn) getApplication()).isHeadset() == true) {
                    if (headset_status != null) headset_status.setImageResource(R.drawable.connect);
                } else {
                    if (headset_status != null)
                        headset_status.setImageResource(R.drawable.disconnect);
                }
                if (SPP_status != null) SPP_status.setImageResource(R.drawable.nospp);
            }
        }
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
