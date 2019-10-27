package com.issc.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.issc.ui.Constants;


/**
 * Created by kanivel.j on 21-12-2016.
 */

public class PrefUtils {


    public static final String PREFERENCE_NAME
            = "pref_jago";


    public static final String PREF_ACCESS_TOKEN
            = "pref_access_token";

    public static final String PREF_FCM_TOKEN
            = "pref_fcm_token";



    public static final String IS_FIRST_TIME
            = "pref_is_first_time";


    public static final String IS_ALREADY_LOGGEDIN
            = "pref_is_already_loggedin";

    public static final String PREF_USER_ID
            = "pref_user_id";

    public static final String PREF_PROFILE_IMAGE
            = "pref_user_image";

    public static final String PREF_USER_TYPE
            = "pref_user_type";
    public static final String PREF_BIT_STREAM
            = "pref_bit_stream";

    public static final String PREF_BIT_DOWNLOAD
            = "pref_bit_download";

    public static final String PREF_SALT
            = "pref_salt";

    public static final String PREF_SALT_ARR
            = "pref_salt_arr";

    public static final String PREF_USER_EMAIL
            = "pref_user_email";

    public static final String PREF_USER_MOBILE
            = "pref_user_mobile";

    public static final String PREF_USER_FNAME
            = "pref_user_fname";

    public static final String PREF_USER_LNAME
            = "pref_user_lname";

    public static final String PREF_USER_NAME
            = "pref_user_name";






    public static void setLanguage(Context context,String language) {
        SharedPreferences sp= context.getSharedPreferences(PREFERENCE_NAME,Context.MODE_PRIVATE);
        sp.edit().putString(Constants.LANGUAGE, language).apply();
    }

public static String getLanguage(Context context){

    SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,Context.MODE_PRIVATE);
    String token = sp.getString(Constants.LANGUAGE, "");
    return token;
}



}
