package com.microchip.eSajda_m;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Created by kanivel.j on 11-03-2018.
 */

public class MBIotApp extends Application {


    // private AppComponent appComponent;
    private static Context mContext;;
    @Override
    public void onCreate() {

        Log.e("Application", "application");
        super.onCreate();
        mContext = getApplicationContext();


    }

    public static Context getAppContext() {
        return mContext;
    }


    }
