package com.microchip.eSajda_m;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.issc.isscaudiowidget.ISSCAudioWidget;
import com.issc.ui.ActivityMain;
import com.microchip.eSajda_m.R;


public class mBIoTMain extends Activity {
    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mbiotmain);
    }

    public void onClickbm70(View v) {

        Log.d(TAG, "onClickbm70 clicked");
        Intent i = new Intent(this, ActivityMain.class);
        i.putExtra("board",70);
        startActivity(i);


    }

    public void onClickbm78(View v) {

        Log.d(TAG,"onClickbm78 clicked");
        Intent i = new Intent(this, ActivityMain.class);
        i.putExtra("board",78);
        startActivity(i);


    }
    public void onClickbmaudowidget(View v) {
        Log.d(TAG, "onClickbmaudowidget clicked");
        Intent i = new Intent(this, ISSCAudioWidget.class);
        startActivity(i);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_m_bio_tmain, menu);
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
}
