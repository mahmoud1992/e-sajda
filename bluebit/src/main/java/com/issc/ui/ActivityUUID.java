package com.issc.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.issc.Bluebit;
import com.issc.R;
import com.issc.util.Log;
import com.issc.util.UuidMatcher;

import java.util.UUID;

public class ActivityUUID extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_activity_uuid);
        Log.d("ActivityUUID");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_uuid, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClickBtnApply(View v) {
        String txt_serviceid;
        String txt_tx;
        String txt_rx;
        int service_uuid_error= 0;
        int tx_uuid_error = 0;
        int rx_uuid_error = 0;

        UuidMatcher match = new UuidMatcher();
        Log.d("Apply clicked");

        EditText et=(EditText)findViewById(R.id.edittext);
        txt_serviceid=et.getText().toString();

        if (et.getText().toString().matches("")) {
            Log.d("Empty Editbox 1");
        }
        else {
            if(match.addRule(txt_serviceid) == false){

                Log.d("Not Matching UUID ERROR");
                et.setError("Please Enter valid UUID");
                service_uuid_error = 1;
            }

            else {
                Log.d("SERVICE_ISSC_PROPRIETARY is set");
                Bluebit.SERVICE_ISSC_PROPRIETARY = UUID.fromString(txt_serviceid);

            }

        }

        et=(EditText)findViewById(R.id.edittext2);
        txt_tx=et.getText().toString();
        if (et.getText().toString().matches("")) {
            Log.d("Empty Editbox 2");
        }
        else {
            if(match.addRule(txt_tx) == false){

                Log.d("Not Matching UUID ERROR");
                tx_uuid_error =1;
                et.setError("Please Enter valid UUID");
            }

            else {
                Log.d("CHR_ISSC_TRANS_TX is set");
                Bluebit.CHR_ISSC_TRANS_TX = UUID.fromString(txt_tx);;

            }

        }

        et=(EditText)findViewById(R.id.edittext3);
        txt_rx=et.getText().toString();
        if (et.getText().toString().matches("")) {
            Log.d("Empty Editbox 3");
        }
        else {
            if(match.addRule(txt_rx) == false){
                rx_uuid_error =1;
                Log.d("Not Matching UUID ERROR");
                et.setError("Please Enter valid UUID");
            }

            else {
                Log.d("CHR_ISSC_TRANS_RX is set");
                Bluebit.CHR_ISSC_TRANS_RX = UUID.fromString(txt_tx);;

            }

        }
        if ((service_uuid_error == 0) && (tx_uuid_error == 0) && (rx_uuid_error == 0)){
            Context context = getApplicationContext();
            CharSequence text = "Applied the UUID as per input";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    public void onClickBtnReset(View v) {
            /* ISSC Proprietary */
        Bluebit.SERVICE_ISSC_PROPRIETARY  = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
        Bluebit.CHR_ISSC_TRANS_TX         = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
        Bluebit.CHR_ISSC_TRANS_RX         = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
        Context context = getApplicationContext();
        CharSequence text = "Applying defualt values";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

    }
}
