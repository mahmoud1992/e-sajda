package com.issc.ui;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.issc.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

public class AlarmReceiver extends BroadcastReceiver implements Constant{


    // The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmMgr;
    // The pending intent that is triggered when the alarm fires.
    private PendingIntent alarmIntent;

    Calendar nowNotif = Calendar.getInstance(TimeZone.getDefault());

    double lat;
    double lng;

    String latStr, lngStr;


    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("MY_TAG", "received AlarmReceiver");

        nowNotif.setTimeInMillis(System.currentTimeMillis());

        String prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME);
        long prayerTime = intent.getLongExtra(EXTRA_PRAYER_TIME, -1);

        boolean timePassed = (prayerTime != -1 && Math.abs(System.currentTimeMillis() - prayerTime) > FIVE_MINUTES);

        if (!timePassed) {
            if(prayerName != null){
                String formatString = "%2$tk:%2$tM %1$s";
                notificationDialog(context,String.format(formatString, prayerName, nowNotif), context.getString(R.string.notification_body, prayerName));
            }
//            Intent service = new Intent(context, SalaatSchedulingService.class);
//            service.putExtra(EXTRA_PRAYER_NAME, prayerName);

            // Start the service, keeping the device awake while it is launching.
            //startWakefulService(context, service);
            // END_INCLUDE(alarm_onreceive)

            // START THE ALARM ACTIVITY
//            Intent newIntent = new Intent(context, RingAlarmActivity.class);
            Log.d("MY_TAG", "Alarm Receiver Got " + prayerName);
//            newIntent.putExtra(EXTRA_PRAYER_NAME, prayerName);
//            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(newIntent);
        }


        setAlarm(context);
    }

    private void notificationDialog(Context context, String title, String msg) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "tutorialspoint_01";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            // Configure the notification channel.
            notificationChannel.setDescription("Sample Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Uri soundUri = Uri.parse("android.resource://"+context.getPackageName()+"/raw/azan");
        Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(soundUri)
                .setContentTitle(title)
                .setContentText(msg);
        notificationManager.notify(1, notificationBuilder.build());
    }

    public void setAlarm(final Context context) {
        Log.d("MY_TAG", "setAlarm: ");
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);

        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        now.setTimeInMillis(System.currentTimeMillis());
        // Set the alarm's trigger time to 8:30 a.m.

        Calendar then = Calendar.getInstance(TimeZone.getDefault());
        then.setTimeInMillis(System.currentTimeMillis());

        LinkedHashMap<String, String> prayerTimes = PrayerActivity.getPrayerTimes();
        List<String> prayerNames = new ArrayList<>(prayerTimes.keySet());

        boolean nextAlarmFound = false;
        String nameOfPrayerFound = null;
        for (String prayer : prayerNames) {

            then = getCalendarFromPrayerTime(then, prayerTimes.get(prayer));

            if (then.after(now)) {
                // this is the alarm to set
                nameOfPrayerFound = prayer;
                nextAlarmFound = true;

                Log.d("MY_TAG", "setAlarm: in for in if prayer => " + prayer);
                break;
            }
        }

        if (!nextAlarmFound) {
            for (String prayer : prayerNames) {

                then = getCalendarFromPrayerTime(then, prayerTimes.get(prayer));

                if (then.before(now)) {
                    // this is the next day.
                    nameOfPrayerFound = prayer;
                    nextAlarmFound = true;

                    Log.d("MY_TAG", "setAlarm: this is the next day => " + prayer);

                    then.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                }
            }
        }

        if (!nextAlarmFound) {
            return; //something went wrong, abort!
        }

        nameOfPrayerFound = getPrayerNameFromIndex(context, getPrayerIndexFromName(nameOfPrayerFound));
        intent.putExtra(EXTRA_PRAYER_NAME, nameOfPrayerFound);
        intent.putExtra(EXTRA_PRAYER_TIME, then.getTimeInMillis());

        alarmIntent = PendingIntent.getBroadcast(context, 1010, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            //lollipop_mr1 is 22, this is only 23 and above
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, then.getTimeInMillis(), alarmIntent);
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //JB_MR2 is 18, this is only 19 and above.
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, then.getTimeInMillis(), alarmIntent);
        } else {
            //available since api1
            alarmMgr.set(AlarmManager.RTC_WAKEUP, then.getTimeInMillis(), alarmIntent);
        }


    }

    private String getPrayerNameFromIndex(Context context, int prayerIndex) {
        String prayerName = null;
        switch (prayerIndex) {
            case 0:
                prayerName = "fajr";
                break;
            case 1:
                prayerName = "dhuhr";
                break;
            case 2:
                prayerName = "asr";
                break;
            case 3:
                prayerName = "maghrib";
                break;
            case 4:
                prayerName = "isha";
                break;
        }
        return prayerName;
    }

    private int getPrayerIndexFromName(String prayerName) {
        String name = prayerName.toLowerCase();
        char index = name.charAt(0);
        switch (index) {
            case 'f':
                return 0;
            case 'd':
                return 1;
            case 'a':
                return 2;
            case 'm':
                return 3;
            case 'i':
                return 4;
        }
        return -1;
    }
    private Calendar getCalendarFromPrayerTime(Calendar cal, String prayerTime) {
        Log.d("MY_TAG", "getCalendarFromPrayerTime: " + prayerTime);
        String[] time = prayerTime.split(":");
        cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time[0]));
        cal.set(Calendar.MINUTE, Integer.valueOf(time[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

}
