package com.issc.ui;

import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.issc.R;
import com.issc.Utils.PrefUtils;
import com.issc.util.LocaleHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

public class PrayerActivity extends AppCompatActivity {

    private RequestQueue mQueue;
    AlarmReceiver alarmReceiver;
    double lat;
    double lng;
    ProgressDialog progressDialog;

    String latStr, lngStr;

    private void setLanguage(String language){

        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = this.getResources().getConfiguration();
        configuration.setLocale(locale);

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer);
        new SharedData(this);

        mQueue = VolleySingleton.getInstance(this).getRequestQueue();

        progressDialog = new ProgressDialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        init();

        alarmReceiver = new AlarmReceiver();
        //alarmReceiver.setAlarm(this);


        latStr = SharedData.getPref("lat");
        lngStr = SharedData.getPref("lng");
        lat = Double.parseDouble(latStr);
        lng = Double.parseDouble(lngStr);
        jsonParse();
        Log.d("MY_TAG", "onCreate: " + lat + " " + lng);
//        if(SharedData.isPrefExists("lang")){
//            String mLanguageCode = SharedData.getPref("lang");
//            //LocaleHelper.setLocale(PrayerActivity.this, mLanguageCode);
//            setLanguage(mLanguageCode);
//            Log.d("MAHM", "onCreate: " + SharedData.getPref("lang"));
//            //recreate();
//            fajr.setText("fff");
//        }
    }

    TextView fajr, dhuhr, asr, maghrib, isha, sunrise;
    private void init() {
        fajr = findViewById(R.id.fajr);
        dhuhr = findViewById(R.id.dhuhr);
        asr = findViewById(R.id.asr);
        maghrib = findViewById(R.id.maghrib);
        isha = findViewById(R.id.isha);
        sunrise = findViewById(R.id.sunrise);
    }


    Calendar c = Calendar.getInstance();
    Date currentDate = c.getTime();

    SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy",new Locale("en"));
    String formattedDate = df.format(currentDate);

    int year = c.get(Calendar.YEAR);
    int month = c.get(Calendar.MONTH) +1;

    static ArrayList<String> prayerTimes = new ArrayList<>();
    public static LinkedHashMap<String, String> getPrayerTimes(){
//        prayerTimes = new ArrayList<>();
//        prayerTimes.add("14:58");
//        prayerTimes.add("14:59");
//        prayerTimes.add("15:00");
//        prayerTimes.add("15:01");
//        prayerTimes.add("15:02");
//        prayerTimes.add("15:03");

        Log.d("MY_TAG", "getPrayerTimes: prayerTimes.size() => " + prayerTimes.size());

        ArrayList<String> prayerNames = new ArrayList<>();
        prayerNames.add("Fajr");
        prayerNames.add("Sunrise");
        prayerNames.add("Dhuhr");
        prayerNames.add("Asr");
        prayerNames.add("Maghrib");
        prayerNames.add("Isha");

        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < prayerTimes.size(); i++) {
            Log.d("MY_TAG", "getPrayerTimes: " + prayerNames.get(i) + " - " + prayerTimes.get(i));
            result.put(prayerNames.get(i), prayerTimes.get(i));
        }

        return result;
    }

    private void jsonParse() {
        String url = "http://api.aladhan.com/v1/calendar?latitude="+lat+"&longitude="+lng+"&month="+month+"&year="+year+"&method=0";
        Log.d("TAGG", "jsonParse: "+ url);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("data");
                            Log.d("MAHM", "onResponse: jsonArray" );
                            if(prayerTimes.size() > 0) prayerTimes.clear();
                            for (int i = 0; i < jsonArray.length(); i++) {

                                JSONObject data = jsonArray.getJSONObject(i);
                                JSONObject date = data.getJSONObject("date");
                                JSONObject timings = data.getJSONObject("timings");
                                //Log.d("MAHM", "onResponse: jsonArray " + date.getString("readable") +" "+formattedDate );
                                if(date.getString("readable").equals(formattedDate)){
                                    Log.d("MAHM", "onResponse: " + timings.getString("Fajr").substring(0,5));

                                    prayerTimes.add(timings.getString("Fajr").substring(0,5));
                                    prayerTimes.add(timings.getString("Sunrise").substring(0,5));
                                    prayerTimes.add(timings.getString("Dhuhr").substring(0,5));
                                    prayerTimes.add(timings.getString("Asr").substring(0,5));
                                    prayerTimes.add(timings.getString("Maghrib").substring(0,5));
                                    prayerTimes.add(timings.getString("Isha").substring(0,5));


//                                            prayerTimes.add("15:30");
//                                            prayerTimes.add("15:32");
//                                            prayerTimes.add("15:33");
//                                            prayerTimes.add("15:36");
//                                            prayerTimes.add("15:37");
//                                            prayerTimes.add("15:38");

                                    fajr.setText(timings.getString("Fajr").substring(0,5));
                                    sunrise.setText(timings.getString("Sunrise").substring(0,5));
                                    dhuhr.setText(timings.getString("Dhuhr").substring(0,5));
                                    asr.setText(timings.getString("Asr").substring(0,5));
                                    maghrib.setText(timings.getString("Maghrib").substring(0,5));
                                    isha.setText(timings.getString("Isha").substring(0,5));

                                    Log.d("MAHM", "onResponse: "+ prayerTimes.size());
                                    alarmReceiver.setAlarm(PrayerActivity.this);
                                    break;
                                }

                            }
                            progressDialog.dismiss();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        mQueue.add(request);
    }

}
