package com.microchip.eSajda_m;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.issc.ui.ScanDeviceListActivity;


public class mBIoT extends Activity {
    private ImageView splashImageView;
    boolean splashloading = false;

    //private AppDatabase db;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // splashImageView = new ImageView(this);

       // splashImageView.setImageResource(R.mipmap.splashble);
       // splashImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        setContentView(R.layout.activity_splash);
        splashloading = true;


       // db = Room.databaseBuilder(this, AppDatabase.class, AppDatabase.DB_NAME).build();


        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.progress_insert));



        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            public void run() {
                splashloading = false;
                startActivity(new Intent(mBIoT.this, ScanDeviceListActivity.class));
                finish();
            }

        }, 3000);
      //  new InsertTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_m_bio_t, menu);
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




   /* private class InsertTask extends AsyncTask<Void, Integer, Void>

    {

        @Override
        protected void onPreExecute() {
            progressDialog.show();

            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {

                List<City> cities=db.cityDao().getAllCity();

                if(cities.size()==0) {

                    AssetUtils.insertCity(mBIoT.this, db);
                }


            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {

            super.onPostExecute(aVoid);

            Log.e("onPostExecute","onPostExecute");

            progressDialog.cancel();


           *//**//*
        }
    }*/


}
