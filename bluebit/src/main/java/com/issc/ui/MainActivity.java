package com.issc.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.arch.persistence.room.Room;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.issc.Bluebit;
import com.issc.BuildConfig;
import com.issc.R;
import com.issc.Utils.AssetUtils;
import com.issc.Utils.GpsUtils;
import com.issc.Utils.KeyboardUtil;
import com.issc.Utils.PrefUtils;
import com.issc.data.local.AppDatabase;
import com.issc.data.local.CityDao;
import com.issc.data.model.City;
import com.issc.gatt.Gatt;
import com.issc.gatt.GattCharacteristic;
import com.issc.gatt.GattDescriptor;
import com.issc.gatt.GattService;
import com.issc.impl.GattTransaction;
import com.issc.impl.LeService;
import com.issc.reliableburst.ReliableBurstData;
import com.issc.util.TransactionQueue;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity implements
        TransactionQueue.Consumer<GattTransaction>, android.location.LocationListener {


    android.support.v7.widget.Toolbar toolbar;

    EditText edtLatitude, edtCountTwo, edtCountThree, edtCountFour, edtDays;

    EditText edtLongiitude;

    Button btnRunGps;

    Spinner spinCountry;

    Spinner spinCity;


    TextView txtCityLatLong;

    RadioGroup radioGroupLanguage;

    RadioButton radioButtonFrench;

    RadioButton radioButtonPersian;

    RadioButton radioButtonEnglish;

    RadioButton radioButtonArabic;

    TextView txtChooseLanguage;

    TextView txtCountFour;

    TextView txtCountThree;

    TextView txtCountTwo;

    TextView txtDays;

    Button btnClearCount;

    TextView txtLatitude;

    TextView txtLongitude;

    TextView txtCountry;

    TextView txtSelectedCityLatLong;

    TextView txtCity;

    Button btnSendLatLong;

    TextView editLog;

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    public static final int RC_SETTINGS_SCREEN = 125;


    //Any random number you can take
    public static final int REQUEST_PERMISSION_LOCATION = 10;

    /**
     * Constant used in the location settings dialog.
     */
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    protected final static String KEY_LOCATION = "location";
    protected final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;


    // Labels.
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected String mLastUpdateTimeLabel;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;


    int RQS_GooglePlayServices = 0;


    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    private Location mLastLocation;

    private boolean isFirstTime = true;


    List<City> countries = new ArrayList<>();
    List<City> cities = new ArrayList<>();

    private AppDatabase db;

    public String selectedCountryName;

    public double selectedLat;
    public double selectedLong;
    public String selectedCity;

    ProgressDialog progressDialog;

    ProgressDialog gpsProgressDialog;


    private LeService mService;
    private BluetoothDevice mDevice;
    private Gatt.Listener mListener;
    private SrvConnection mConn;

    private ProgressDialog mConnectionDialog;
    private ProgressDialog mTimerDialog;
    protected ViewHandler mViewHandler;

    private OutputStream mStream;
    private TransactionQueue mQueue;

    private final static int PAYLOAD_MAX = 20; // 90 bytes might be max

    private final static int CONNECTION_DIALOG = 1;
    private final static int TIMER_DIALOG = 2;
    private final static int CHOOSE_FILE = 0x101;
    private final static int COMPARE_FILE = 0x102;
    private final static int MENU_CLEAR = 0x501;

    private final static String INFO_CONTENT = "the_information_body";
    private final static String ECHO_ENABLED = "echo_function_is_enabled";

    private final static int SHOW_CONNECTION_DIALOG = 0x1000;
    private final static int DISMISS_CONNECTION_DIALOG = 0x1001;
    private final static int CONSUME_TRANSACTION = 0x1002;
    private final static int DISMISS_TIMER_DIALOG = 0x1003;
    private final static int APPEND_MESSAGE = 0x1004;
    private final static int ECHO_STATE = 0x1005;

    String FileString = "";
    static int countx = 0;

    private TabHost mTabHost;
    // private TextView mMsg;
    private EditText mInput;
    private Button mBtnSend;
    private ToggleButton mToggleEcho;
    private ToggleButton mToggleResponse;
    private CompoundButton mEchoIndicator;

    private Spinner mSpinnerDelta;
    private Spinner mSpinnerSize;
    private Spinner mSpinnerRepeat;

    private int[] mValueDelta;
    private int[] mValueSize;
    private int[] mValueRepeat;

    private GattCharacteristic mTransTx;
    private GattCharacteristic mTransRx;
    private GattCharacteristic mAirPatch;

    private int mSuccess = 0;
    private int mFail = 0;
    private int total_bytes = 0;
    private Calendar mStartTime = null;
    private Calendar mEndTime = null;
    private Calendar mTempStartTime = null;
    private Handler mHandler = null;
    private Runnable mRunnable = null;
    private Handler writeThread;

    private final static int MAX_LINES = 50;
    private ArrayList<CharSequence> mLogBuf;

    private ReliableBurstData transmit;

    private ReliableBurstData.ReliableBurstDataListener transmitListener;
    private boolean reTry = false;


    private boolean mRunning;


    private LocationManager mLocationManager;

    private LocationProvider mProvider;

    private static final String TAG = "GpsTestActivity";

    boolean mStarted;

    private ProgressDialog mDiscoveringDialog;
    private final static int DISCOVERY_DIALOG = 4;
    private final static int CONNECT_DIALOG = 2;

    boolean clearClicked = false;
    boolean sendLatLongClicked = false;

    byte bytes[] = null;

    SoundPool soundPool;
    private int sp1,sp2,sp3,sp4,sp5,sp6,sp7,sp8,sp9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new SharedData(this.getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initValues();

        mQueue = new TransactionQueue(this);

        mViewHandler = new ViewHandler();
        mDevice = getIntent().getParcelableExtra(Bluebit.CHOSEN_DEVICE);
        mListener = new GattListener();

        mLogBuf = new ArrayList<CharSequence>();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.progress_insert));

        gpsProgressDialog = new ProgressDialog(this);
        gpsProgressDialog.setMessage(getString(R.string.progress_loading));

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();

        setSupportActionBar(toolbar);
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

       /* mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();*/

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (mProvider == null) {
            Log.e(TAG, "Unable to get GPS_PROVIDER");
            Toast.makeText(this, getString(R.string.gps_not_supported),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        db = Room.databaseBuilder(this, AppDatabase.class, AppDatabase.DB_NAME).build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);


        new InsertTask().execute();

        KeyboardUtil.hideKeyboard(this);



        radioButtonFrench.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setLanguage(Constants.LANG_FR);


            }
        });

        radioButtonPersian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setLanguage(Constants.LANG_FA);


            }
        });



       radioButtonEnglish.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {

               setLanguage(Constants.LANG_EN);

           }
       });



        radioButtonArabic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setLanguage(Constants.LANG_AR);

            }
        });


        setLanguageChecked();




        /* Transparent is not a leaf activity. connect service in onCreate */
        mConn = new SrvConnection();
        bindService(new Intent(this, LeService.class), mConn, Context.BIND_AUTO_CREATE);

        //Log.d("MADHU LOG");

        transmit = new ReliableBurstData();
        transmitListener = new ReliableBurstData.ReliableBurstDataListener() {
            @Override
            public void onSendDataWithCharacteristic(
                    ReliableBurstData reliableBurstData,
                    final BluetoothGattCharacteristic transparentDataWriteChar) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        com.issc.util.Log.d("onSendDataWithCharacteristic runOnUiThread");
                        String s = String.format(
                                "%d bytes, success= %d, fail= %d, pending= %d",
                                transparentDataWriteChar.getValue().length,
                                mSuccess, mFail, mQueue.size());
                        didGetData(s);
                    }
                });

            }
        };
        transmit.setListener(transmitListener);
        HandlerThread thread = new HandlerThread("writeThread");
        thread.start();
        writeThread = new Handler(thread.getLooper());



        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(9)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(9, AudioManager.STREAM_MUSIC, 0);
        }

        sp1 = soundPool.load(this, R.raw.sp1, 1);
        sp2 = soundPool.load(this, R.raw.sp2, 1);
        sp3 = soundPool.load(this, R.raw.sp3, 1);
        sp4 = soundPool.load(this, R.raw.sp4, 1);
        sp5 = soundPool.load(this, R.raw.sp5, 1);
        sp6 = soundPool.load(this, R.raw.sp6, 1);
        sp7 = soundPool.load(this, R.raw.sp7, 1);
        sp8 = soundPool.load(this, R.raw.sp8, 1);
        sp9 = soundPool.load(this, R.raw.sp9, 1);

    }



    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        /* FIXME: this function is deprecated. */
        if (id == CONNECTION_DIALOG) {
            mConnectionDialog = new ProgressDialog(this);
            mConnectionDialog.setMessage(this.getString(R.string.connecting));
            mConnectionDialog.setCancelable(true);
            return mConnectionDialog;
        }
        else if (id == DISCOVERY_DIALOG) {
            mDiscoveringDialog = new ProgressDialog(this);
            mDiscoveringDialog.setMessage(this.getString(R.string.discovering));
            mDiscoveringDialog.setOnCancelListener(new Dialog.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    // this dialog should be closed, do not need to dismiss again
                    //dismissDiscovery();
                }
            });
            return mDiscoveringDialog;
        }
        return null;
    }






    private void didGetData(String s) {
        com.issc.util.Log.d("didGetData"+s);


        synchronized (mQueue) {
            mQueue.onConsumed();
            //msgShow("", "\n");
             // msgShow("wrote ", s);
           // msgShow("", "\n");mStartTime




            //if (mQueue.size() == 0 && mStartTime != null) {
            if (mQueue.size() == 0) {
               /* final long elapse = Calendar.getInstance()
                        .getTimeInMillis()
                        - mStartTime.getTimeInMillis();
        */        //Handler handler = new Handler();
                Runnable runnable = new Runnable() {

                    @Override
                    public void run() {


                        Log.e("Write completed","write completed");

                        /*msgShow("time", "spent " + (elapse / 1000)
                                + " seconds" + "  Throughput: " + (total_bytes/(elapse / 1000))
                                + " bytes/sec");*/
                        total_bytes = 0;
                        mSuccess = 0;
                        mFail = 0;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                                if(sendLatLongClicked||clearClicked)
                                 updatewriteAction();

                            }
                        });


                    }
                };

                writeThread.postDelayed(runnable, 3000);
                mTempStartTime = mStartTime;
                mStartTime = null;

            }



            updateView(CONSUME_TRANSACTION, null);

        }
    }

    @Override
    public void onDestroy() {

        soundPool.release();
        soundPool = null;
        //mQueue.clear();
        mQueue.destroy();
        //disableNotification();
        closeStream();
        mViewHandler.removeCallbacksAndMessages(null);

		/*
		 * Transparent is not a leaf activity. disconnect/unregister-listener in
		 * onDestroy
		 */
		try {
                mService.rmListener(mListener);
                mService = null;
            }
            catch(NullPointerException e)
            {
                e.printStackTrace();
            }
        unbindService(mConn);

        //stopLocationUpdates();

        mLocationManager.removeUpdates(this);

        /*btnRunGps.setText(getString(R.string.run_gps));
        btnRunGps.setBackgroundResource(R.drawable.btn_selector);
        mRequestingLocationUpdates=false;
        isFirstTime=true*/;

        unregisterReceiver(mReceiver);
        super.onDestroy();
    }





    public void onClickSend(View v) {
        com.issc.util.Log.d("onClickSend called");

        CharSequence cs = mInput.getText();
        //msgShow("onClickSend called",cs);
        //msgShow("send", "\n");
        //msgShow("", cs);
        write(cs);
        mInput.setText("");
    }




    private void openStream(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                com.issc.util.Log.w("Target file does not exist, create: " + path);
                File parent = file.getParentFile();
                com.issc.util.Log.w("make dirs:" + parent.getPath());
                parent.mkdirs();
                file.createNewFile();
            }

            mStream = new FileOutputStream(file, false);
        } catch (IOException e) {
           // msgShow("open stream fail", e.toString());
            e.printStackTrace();
        }
    }

    private void closeStream() {
        com.issc.util.Log.d("closeStream");
        try {
            if (mStream != null) {
                mStream.flush();
                mStream.close();
            }
        } catch (IOException e) {
            //msgShow("close stream fail", e.toString());
            e.printStackTrace();
        }

        mStream = null;
    }

    private void writeToStream(byte[] data) {
        com.issc.util.Log.d("inside writeToStream mStream:" + mStream );
        //msgShow("recv", data);
        if (mStream != null) {
            try {
                mStream.write(data, 0, data.length);
                mStream.flush();
            } catch (IOException e) {
               // msgShow("write fail", e.toString());
                e.printStackTrace();
            }
        }
    }


    private void onSetEcho(boolean enable) {
        if (enable) {
            // enableNotification();
            openStream(Bluebit.DEFAULT_LOG);
            Bundle state = new Bundle();
            state.putBoolean(ECHO_ENABLED, true);
            //updateView(ECHO_STATE, state);

        } else {
            // disableNotification();
            closeStream();
            Bundle state = new Bundle();
            state.putBoolean(ECHO_ENABLED, false);
            //updateView(ECHO_STATE, state);
        }
    }

    private void enableNotification() {
        com.issc.util.Log.d("calling mService.setCharacteristicNotification:Activity Transperent");
        boolean set = mService.setCharacteristicNotification(mTransTx, true);
        com.issc.util.Log.d("set notification:" + set);
        GattDescriptor dsc = mTransTx
                .getDescriptor(Bluebit.DES_CLIENT_CHR_CONFIG);
        dsc.setValue(dsc
                .getConstantBytes(GattDescriptor.ENABLE_NOTIFICATION_VALUE));
        mService.writeDescriptor(dsc);
        GattTransaction transaction = new GattTransaction(dsc,
                dsc.getConstantBytes(GattDescriptor.ENABLE_NOTIFICATION_VALUE));
        mQueue.add(transaction);
        // mQueue.process();

		/*
		 * boolean success = mService.writeDescriptor(dsc);
		 * Log.d("writing enable descriptor:" + success);
		 */

    }




    /**
     * Received data from remote when enabling Echo.
     *
     * Display the data and transfer back to device.
     */
    private void onEcho(byte[] data) {
        StringBuffer sb = new StringBuffer();
        if (data == null) {
            sb.append("Received empty data");
        } else {
            String recv = new String(data);
           // msgShow("recv", recv);
            write(data);
            //writeToStream(data);
           // msgShow("echo", recv);
        }
       /* Bundle msg = new Bundle();
        msg.putCharSequence(INFO_CONTENT, sb);
        updateView(APPEND_MESSAGE, msg);*/
    }

    private void onReciveData(byte[] data) {
        //Log.d("[R}");
        StringBuffer sb = new StringBuffer();
        if (data == null) {
            sb.append("Received empty data");
            Bundle msg = new Bundle();
            com.issc.util.Log.d("going for msg.putCharSequence(INFO_CONTENT, sb)");
            msg.putCharSequence(INFO_CONTENT, sb);
            //updateView(APPEND_MESSAGE, msg);
        } else {
            String recv = new String(data);
            msgShow("", recv);
            writeToStream(data);
        }

    }




    private void msgShow(CharSequence prefix, CharSequence cs) {
        StringBuffer sb = new StringBuffer();

        com.issc.util.Log.d("count:" + countx);
        countx++;
        sb.append(prefix);
        //sb.append(": ");
        sb.append(cs);
        //Log.d(sb.toString());
        Bundle msg = new Bundle();
        msg.putCharSequence(INFO_CONTENT, sb.toString());
        //for(int i = 30000; i > 0 ; i--);
        updateView(APPEND_MESSAGE, msg);
    }



    /**
     * Write string to remote device.
     */
    private void write(CharSequence cs) {
        byte[] bytes = cs.toString().getBytes();
        com.issc.util.Log.d("write(CharSequence cs)");
        write(bytes);
    }

    /**
     * Write data to remote device.
     */
    private void write(final byte[] bytes) {

        //this.bytes=bytes;

        com.issc.util.Log.d(" write before writeThread.post");
        //mService.()
        /*BluetoothGatt gatt = (BluetoothGatt) mService.getGatt().getImpl();
        gatt.requestMtu(60);*/
        writeThread.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mQueue) {
                    com.issc.util.Log.d(" write inside thread run");
                    ByteBuffer buf = ByteBuffer.allocate(bytes.length);
                    buf.put(bytes);
                    buf.position(0);
                    while (buf.remaining() != 0) {
                        int size = (buf.remaining() > transmit.transmitSize()) ? transmit
                                .transmitSize() : buf.remaining();

                       //int size=buf.remaining();
                        byte[] dst = new byte[size];
                        buf.get(dst, 0, size);
                        GattTransaction t = new GattTransaction(mTransRx, dst);
                        mQueue.add(t);
                        com.issc.util.Log.i("size" + mQueue.size());
                        if (mQueue.size() == 1) {
                            mQueue.process();
                        }
                    }
                }
            }
        });
    }




    public void updatewriteAction()
    {

        Log.e("Update Write Action","Update Write Action");

        if(sendLatLongClicked)
        {
            sendLatLongClicked=false;
            btnSendLatLong.setBackgroundResource(R.drawable.btn_selector);
            btnSendLatLong.setEnabled(true);
        }
        else if(clearClicked)
        {
            clearClicked=false;
            btnClearCount.setBackgroundResource(R.drawable.btn_selector);
            btnClearCount.setEnabled(true);

        }

    }



    /**
     * Add message to UI.
     */
    private void appendMsg(CharSequence msg) {

        StringBuffer sb = new StringBuffer();
        sb.append(msg);
        //sb.append("\n");
        mLogBuf.add(sb);
        // we don't want to display too many lines
        Log.e("ReceiveMsg",msg.toString()+"****");

        /*if (mLogBuf.size() > MAX_LINES) {
            mLogBuf.remove(0);
        }*/

        StringBuffer text = new StringBuffer();
        for (int i = 0; i < mLogBuf.size(); i++) {
            text.append(mLogBuf.get(i));
        }
        Log.e("appendMsg text",text.toString());



        editLog.setText(text);


        String msgSplit="";
        String rak2="";
        String rak3="";
        String rak4="";
        int lastRak2=0;
        int lastRak3=0;
        int lastRak4=0;
        boolean firstRAK2=true;
        boolean firstRAK3=true;
        boolean firstRAK4=true;


        if(text!=null)
        {

            try {
                msgSplit = String.valueOf(text);

                if (msgSplit.contains("RAK2=")&&msgSplit.contains("RAK3=")&&msgSplit.contains("RAK4=")) {
                    String[] arrSplit = msgSplit.split("=");


                    if (arrSplit.length > 0) {

                        rak4 =arrSplit[arrSplit.length-1];
                        rak2=arrSplit[1].substring(0,arrSplit[1].indexOf("RAK3"));
                        rak3=arrSplit[2].substring(0,arrSplit[2].indexOf("RAK4"));

                        edtCountTwo.setText(rak2);

                        if(firstRAK2)
                        {
                            edtDays.setText(rak2);
                        }
                        else if(Integer.parseInt(rak2)>lastRak2)
                        {
                            edtDays.setText(rak2);
                        }

                        if(firstRAK2)
                        {
                            firstRAK2=false;
                        }


                        lastRak2=Integer.parseInt(rak2);

                        edtCountThree.setText(rak3);


                        if(firstRAK3)
                        {
                            edtDays.setText(rak3);
                        }
                        else if(Integer.parseInt(rak3)>lastRak3)
                        {
                            edtDays.setText(rak3);

                        }

                        if(firstRAK3)
                        {
                            firstRAK3=false;
                        }


                        lastRak3=Integer.parseInt(rak3);






                        edtCountFour.setText(rak4);

                        if(firstRAK4)
                        {
                            edtDays.setText((Integer.parseInt(rak4)/3)+"");
                        }
                        else if(Integer.parseInt(rak4)>=(lastRak4+3))
                        {
                            edtDays.setText((Integer.parseInt(rak4)/3)+"");

                        }

                        if(firstRAK4)
                        {
                            firstRAK4=false;
                        }


                        lastRak4=Integer.parseInt(rak4);



                        //edtCountFour.setText(rak4);
                        //edtCountThree.setText(rak3);
                       /* int oneDay=0;

                        oneDay=(Integer.parseInt(rak2))+(Integer.parseInt(rak3))+(Integer.parseInt(rak4)/3);

                        int total=Integer.parseInt(rak2)+Integer.parseInt(rak3)+Integer.parseInt(rak4);

                        int days=total/oneDay;





                        edtDays.setText(days+"");*/


                        mLogBuf.clear();

                    }
                }
                else{
                    Log.d("MAH", "appendMsg: "+msg);
                    switch (msg+""){
                        case "SP1":
                            soundPool.play(sp1, 1, 1, 0, 0, 1);
                            break;
                        case "SP2":
                            soundPool.play(sp2, 1, 1, 0, 0, 1);
                            break;
                        case "SP3":
                            soundPool.play(sp3, 1, 1, 0, 0, 1);
                            break;
                        case "SP4":
                            soundPool.play(sp4, 1, 1, 0, 0, 1);
                            break;
                        case "SP5":
                            soundPool.play(sp5, 1, 1, 0, 0, 1);
                            break;
                        case "SP6":
                            soundPool.play(sp6, 1, 1, 0, 0, 1);
                            break;
                        case "SP7":
                            soundPool.play(sp7, 1, 1, 0, 0, 1);
                            break;
                        case "SP8":
                            soundPool.play(sp8, 1, 1, 0, 0, 1);
                            break;
                        case "SP9":
                            soundPool.play(sp9, 1, 1, 0, 0, 1);
                            break;
                    }
                }

                /*if(msgSplit.contains("RAK2"))
                {
                    String[] arrSplit = msgSplit.split("=");

                    if (arrSplit.length > 0) {

                        rak2=arrSplit[1];
                        edtCountTwo.setText(rak2);

                        if(firstRAK2)
                        {
                            edtDays.setText(rak2);
                        }
                        else if(Integer.parseInt(rak2)>lastRak2)
                        {
                            edtDays.setText(rak2);

                        }

                        if(firstRAK2)
                        {
                            firstRAK2=false;
                        }


                        lastRak2=Integer.parseInt(rak2);

                        mLogBuf.clear();
                    }


                    }
                else if(msgSplit.contains("RAK3"))
                {
                    String[] arrSplit = msgSplit.split("=");

                    if (arrSplit.length > 0) {

                        rak3=arrSplit[1];
                        edtCountThree.setText(rak3);


                        if(firstRAK3)
                        {
                            edtDays.setText(rak3);
                        }
                        else if(Integer.parseInt(rak3)>lastRak3)
                        {
                            edtDays.setText(rak3);

                        }

                        if(firstRAK3)
                        {
                            firstRAK3=false;
                        }


                        lastRak3=Integer.parseInt(rak3);


                        mLogBuf.clear();


                    }
                }
                else if(msgSplit.contains("RAK4"))
                {

                    String[] arrSplit = msgSplit.split("=");

                    if (arrSplit.length > 0) {


                        rak4=arrSplit[1];

                        edtCountFour.setText(rak4);

                        if(firstRAK4)
                        {
                            edtDays.setText((Integer.parseInt(rak4)/3)+"");
                        }
                        else if(Integer.parseInt(rak4)>=(lastRak4+3))
                        {
                            edtDays.setText((Integer.parseInt(rak4)/3)+"");

                        }

                        if(firstRAK4)
                        {
                            firstRAK4=false;
                        }


                        lastRak4=Integer.parseInt(rak4);


                        mLogBuf.clear();


                    }
                }*/


            }catch (IndexOutOfBoundsException e)
            {

                e.printStackTrace();
            }
            catch (Exception e)
            {

                e.printStackTrace();
            }
        }

        /*try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/




    }

    private void onConnected() {
        List<GattService> list = mService.getServices(mDevice);
        com.issc.util.Log.d("onConnected");
        if ((list == null) || (list.size() == 0)) {
            com.issc.util.Log.d("no services, do discovery");
            mService.discoverServices(mDevice);
        } else {
            onDiscovered();
        }
    }

    private void onDisconnected() {
        com.issc.util.Log.d("transparent activity disconnected, closing");
        //stopTimer();
        mStartTime = null;
        mTempStartTime =null;
        mQueue.clear();
        this.setResult(Bluebit.RESULT_REMOTE_DISCONNECT);
        this.finish();
    }

    private void onDiscovered() {
        updateView(DISMISS_CONNECTION_DIALOG, null);
        com.issc.util.Log.d(" onDiscovered ActivityTransparent Bluebit.CHR_ISSC_TRANS_TX" + Bluebit.CHR_ISSC_TRANS_TX);
        GattService proprietary = mService.getService(mDevice,
                Bluebit.SERVICE_ISSC_PROPRIETARY);

        try {
            mTransTx = proprietary.getCharacteristic(Bluebit.CHR_ISSC_TRANS_TX);
            mTransRx = proprietary.getCharacteristic(Bluebit.CHR_ISSC_TRANS_RX);
            mAirPatch = proprietary.getCharacteristic(Bluebit.CHR_AIR_PATCH);
            proprietary = mService.getService(mDevice, Bluebit.SERVICE_ISSC_AIR_PATCH_SERVICE);
            if (proprietary.getImpl() != null) {
                mAirPatch = proprietary.getCharacteristic(Bluebit.CHR_AIR_PATCH);
            }
            com.issc.util.Log.d(String.format("found Tx:%b, Rx:%b, AirPatch:%b",
                    mTransTx != null, mTransRx != null, mAirPatch != null));
            onSetEcho(false);
            enableNotification();
            // sendVendorMPEnable();
            BluetoothGatt gatt = (BluetoothGatt) mService.getGatt().getImpl();
            if (gatt != null) {
                BluetoothGattCharacteristic air_ch = (BluetoothGattCharacteristic) mAirPatch
                        .getImpl();
                transmit.enableReliableBurstTransmit(gatt, air_ch);
            }
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
            //displayConnecting();
            //connectToDevice();
            finish();
        }
    }

    @Override
    public void onTransact(GattTransaction t) {
        com.issc.util.Log.d("onTransact Activity transperant ");
        if (t.isForDescriptor()) {
            GattDescriptor dsc = t.desc;
            boolean success = mService.writeDescriptor(dsc);
            com.issc.util.Log.d("writing " + dsc.getCharacteristic().getUuid().toString()
                    + " descriptor:" + success);
        } else {

            try {
                com.issc.util.Log.d("onTransact t.isForDescriptor() false");
                t.chr.setValue(t.value);
                com.issc.util.Log.d("Value : " + t.value);
                String str1 = new String(t.value);
                com.issc.util.Log.d("Value (string) : " + str1);
                if (t.isWrite) {
                    com.issc.util.Log.d("GattCharacteristic.WRITE_TYPE_DEFAULT" + GattCharacteristic.WRITE_TYPE_DEFAULT);
                    com.issc.util.Log.d("GattCharacteristic.WRITE_TYPE_NO_RESPONSE" + GattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    int type = true ? GattCharacteristic.WRITE_TYPE_DEFAULT
                            : GattCharacteristic.WRITE_TYPE_NO_RESPONSE;


                    t.chr.setWriteType(type);
                    //com.issc.util.Log.d("mToggleResponse.isChecked()  ;  type : " + mToggleResponse.isChecked() + type);
                    com.issc.util.Log.d("!t.chr.getUuid().equals(Bluebit.CHR_AIR_PATCH)" + !t.chr.getUuid().equals(Bluebit.CHR_AIR_PATCH));
                    if (type == GattCharacteristic.WRITE_TYPE_NO_RESPONSE
                            && !t.chr.getUuid().equals(Bluebit.CHR_AIR_PATCH)) {
                        synchronized (mQueue) {
                            com.issc.util.Log.d("calling canSendReliableBurstTransmit");
                            if (transmit.canSendReliableBurstTransmit()) {
                                BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) t.chr
                                        .getImpl();
                                com.issc.util.Log.d("calling reliableBurstTransmit");
                                transmit.reliableBurstTransmit(t.value, ch);
                            } else {
                                mQueue.addFirst(t);
                                mQueue.onConsumed();
                            }
                        }
                    } else {
                        mService.writeCharacteristic(t.chr);
                    }
                } else {
                    mService.readCharacteristic(t.chr);
                }

            }
            catch(NullPointerException e)
            {

            }
        }
    }

    public void updateView(int tag, Bundle info) {
        //Log.d("Inside updateView tag :"+ tag);

        if (info == null) {
            //Log.d("info is null");
            info = new Bundle();
        }

        // remove previous log since the latest log
        // already contains needed information.
        //mViewHandler.removeMessages(tag);

        Message msg = mViewHandler.obtainMessage(tag);
        msg.what = tag;
        msg.setData(info);
        //Log.d("Sending message to view handler");
        mViewHandler.sendMessage(msg);
    }

    @Override
    public void onLocationChanged(Location location) {


        if(location!=null) {
            //Log.d("MY_TAG", "onLocationChanged: " + location.getLatitude()+" " + location.getLongitude());
//            42.92760149 -83.6845299
            SharedData.savePref("lat",location.getLatitude()+"");
            SharedData.savePref("lng",location.getLongitude()+"");

            updateLocationUI(location);

        }


    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    class ViewHandler extends Handler {
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            if (bundle == null) {
                com.issc.util.Log.d("ViewHandler handled a message without information");
                return;
            }

            int tag = msg.what;
            if (tag == SHOW_CONNECTION_DIALOG) {
                showDialog(CONNECTION_DIALOG);
            } else if (tag == DISMISS_CONNECTION_DIALOG) {
                if (mConnectionDialog != null && mConnectionDialog.isShowing()) {
                    dismissDialog(CONNECTION_DIALOG);
                }
            } else if (tag == DISMISS_TIMER_DIALOG) {
                if (mTimerDialog != null && mTimerDialog.isShowing()) {
                    dismissDialog(TIMER_DIALOG);
                }
            } else if (tag == CONSUME_TRANSACTION) {
                // mQueue itself will consume next transaction
                // mQueue.process();
            } else if (tag == APPEND_MESSAGE) {
                //Log.d("handleMessage called with APPEND_MESSAGE ");
                CharSequence content = bundle.getCharSequence(INFO_CONTENT);
                if (content != null) {
                    appendMsg(content);

					/* fot automaticaly scrolling to end */
                   /* final int amount = mMsg.getLayout().getLineTop(
                            mMsg.getLineCount())
                            - mMsg.getHeight();
                    if (amount > 0) {
                        mMsg.scrollTo(0, amount);
                    }*/
                }
            } else if (tag == ECHO_STATE) {
               /* mEchoIndicator.setChecked(bundle
                        .getBoolean(ECHO_ENABLED, false));*/
            }
        }
    }

    class GattListener extends Gatt.ListenerHelper {

        GattListener() {
            super("ActivityTransparent");
        }


       /* @Override
        public void onMtuChanged(Gatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);


            Log.e("OnMtuChanged","OnMtuChanged"+status);

            writeThread.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mQueue) {
                        com.issc.util.Log.d(" write inside thread run");
                        ByteBuffer buf = ByteBuffer.allocate(bytes.length);
                        buf.put(bytes);
                        buf.position(0);
                        while (buf.remaining() != 0) {
                            *//*int size = (buf.remaining() > transmit.transmitSize()) ? transmit
                                    .transmitSize() : buf.remaining();
*//*
                            int size=buf.remaining();
                            byte[] dst = new byte[size];
                            buf.get(dst, 0, size);
                            GattTransaction t = new GattTransaction(mTransRx, dst);
                            mQueue.add(t);
                            com.issc.util.Log.i("size" + mQueue.size());
                            if (mQueue.size() == 1) {
                                mQueue.process();
                            }
                        }
                    }
                }
            });



        }*/

        @Override
        public void onConnectionStateChange(Gatt gatt, int status, int newState) {

            com.issc.util.Log.d("onConnectionStateChange: DATA TRANSFER " );
            if (!mDevice.getAddress().equals(gatt.getDevice().getAddress())) {
                // not the device I care about
                return;
            }

            if (reTry) {
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mService.connectGatt(MainActivity.this, false, mDevice);
                }
                else {
                    transmit = null;
                    com.issc.util.Log.d("ReliableBurstData :DATA TRANSFER" );
                    transmit = new ReliableBurstData();
                    transmit.setListener(transmitListener);
                    onConnected();
                    com.issc.util.Log.d("setting board id for trnasmitdata" + Bluebit.board_id );
                    transmit.setBoardId(Bluebit.board_id);
                    reTry = false;
                }
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                dismissConnect();
                onConnected();

                com.issc.util.Log.d("connected to device, start discovery");
                //displayDiscovering();
                //mService.discoverServices(mDevice);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                dismissDiscovery();
                //Snackbar.make(btnSendLatLong,getString(R.string.disconnected),Snackbar.LENGTH_LONG).show();

               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {


                       Toast.makeText(MainActivity.this,getString(R.string.disconnected),Toast.LENGTH_LONG).show();
                       onDisconnected();

                   }
               });

               // onDiscovered();
            }
        }

        @Override
        public void onServicesDiscovered(Gatt gatt, int status) {
            dismissDiscovery();
            onDiscovered();
        }

        @Override
        public void onCharacteristicRead(Gatt gatt, GattCharacteristic charac,
                                         int status) {
            com.issc.util.Log.d("onCharacteristicRead");
            com.issc.util.Log.d("read char, uuid=" + charac.getUuid().toString());
            byte[] value = charac.getValue();
            com.issc.util.Log.d("get value, byte length:" + value.length);
            for (int i = 0; i < value.length; i++) {
                com.issc.util.Log.d("[" + i + "]" + Byte.toString(value[i]));
            }
            synchronized (mQueue) {
                mQueue.onConsumed();
            }
        }

        @Override
        public void onCharacteristicChanged(Gatt gatt, GattCharacteristic chrc) {
            com.issc.util.Log.d("on chr changed");
            byte arr[] = new byte[] {0x55, 0x44, 0x55};
            com.issc.util.Log.d(" onCharacteristicChanged Bluebit.CHR_ISSC_TRANS_TX" +Bluebit.CHR_ISSC_TRANS_TX);
            if (chrc.getUuid().equals(Bluebit.CHR_ISSC_TRANS_TX)) {
                com.issc.util.Log.d("onCharacteristicChanged:  getUuid successful ");
                com.issc.util.Log.d("mRunnable" + mRunnable);
                //com.issc.util.Log.d("onCharacteristicChanged mToggleEcho.isChecked " + mToggleEcho.isChecked());
                com.issc.util.Log.d("read char, uuid=" + chrc.getUuid().toString());
                byte[] value = chrc.getValue();
                com.issc.util.Log.d("get value, byte length:" + value.length);
                String buffer = "";
                for (int i = 0; i < value.length; i++) {
                    buffer = buffer+String.format("%02X ", value[i]);
                }
                com.issc.util.Log.d("[" + buffer + "]");
                //msgShow("send", cs);
                onReciveData(chrc.getValue());
                //onEcho(arr);

                if (true) {
                    onEcho(chrc.getValue());
                }
                if (mRunnable != null) {
                    if (mStartTime == null) {
                        mStartTime = Calendar.getInstance();
                    }
                    mEndTime = Calendar.getInstance();
                    if (mHandler == null) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mHandler = new Handler();
                                mHandler.postDelayed(mRunnable, 50*1000);
                            }
                        });
                    }
                    else {
                        mHandler.removeCallbacks(mRunnable);
                        mHandler.postDelayed(mRunnable, 50*1000);
                    }
                    com.issc.util.Log.d("read char, uuid=" + chrc.getUuid().toString());
                    //byte[] value = chrc.getValue();
                    com.issc.util.Log.d("get value, byte length:" + value.length);
                    //String buffer = "";
                    for (int i = 0; i < value.length; i++) {
                        buffer = buffer+String.format("%02X ", value[i]);
                    }
                    com.issc.util.Log.d("[" + buffer + "]");
                    //onReciveData(chrc.getValue());
                }
            }
            if (chrc.getUuid().equals(Bluebit.CHR_AIR_PATCH)) {
                transmit.decodeReliableBurstTransmitEvent(chrc.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(Gatt gatt, GattCharacteristic charac,
                                          int status) {
            com.issc.util.Log.d("onCharacteristicWrite");
            com.issc.util.Log.d("onCharacteristicWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                reTry = false;
            }
            BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) charac
                    .getImpl();
            if (transmit.isReliableBurstTransmit(ch)) {
                if (status == Gatt.GATT_SUCCESS) {
                    if (status == Gatt.GATT_SUCCESS) {
                        mSuccess += charac.getValue().length;
                    } else {
                        mFail += charac.getValue().length;
                    }
                    total_bytes = total_bytes + charac.getValue().length;
                    com.issc.util.Log.d("onCharacteristicWrite isReliableBurstTransmit success transmit.isBusy()"+
                            " "+ transmit.isBusy());
                    if (!transmit.isBusy() && charac.getUuid().equals(Bluebit.CHR_AIR_PATCH)) {
                        onSetEcho(true);
                        enableNotification();
                    }
                }
            } else {
                synchronized (mQueue) {
                    mQueue.onConsumed();
                }
                if (charac.getUuid().equals(Bluebit.CHR_AIR_PATCH)) {
                    com.issc.util.Log.i("Write AirPatch Characteristic:" + status);
                } else {

                    com.issc.util.Log.d("---------------");

                    if (status == Gatt.GATT_SUCCESS) {
                        mSuccess += charac.getValue().length;
                    } else {
                        mFail += charac.getValue().length;
                    }
                    total_bytes = total_bytes + charac.getValue().length;
                    String s = String.format(
                            "%d bytes , success= %d, fail= %d, pending= %d, TOTAL=%d",
                            charac.getValue().length, mSuccess, mFail,
                            mQueue.size(),total_bytes);
                    didGetData(s);
                }
            }
        }

        @Override
        public void onDescriptorWrite(Gatt gatt, GattDescriptor dsc, int status) {
            BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) dsc
                    .getCharacteristic().getImpl();
            com.issc.util.Log.d("onDescriptorWrite");
            if (status == 5) {
                reTry = true;
                return;
            }
            if (reTry && status == 133) {
                mService.disconnect(mDevice);
                return;
            }
            if (transmit.isReliableBurstTransmit(ch)) {
                if (status == Gatt.GATT_SUCCESS) {
                    if (!transmit.isBusy()) {
                        if (mQueue.size() > 0) {
                            mQueue.process();
                        } else {
                            onSetEcho(true);
                            enableNotification();
                        }
                    }
                }
            } else {
                mQueue.onConsumed();
                if (dsc.getCharacteristic().getUuid()
                        .equals(Bluebit.CHR_AIR_PATCH)) {
                    if (status == Gatt.GATT_SUCCESS) {
                        com.issc.util.Log.i("Write AirPatch Descriptor Success");
                    }
                } else {
                    if (status == Gatt.GATT_SUCCESS) {
                        byte[] value = dsc.getValue();
                        if (Arrays
                                .equals(value,
                                        dsc.getConstantBytes(GattDescriptor.ENABLE_NOTIFICATION_VALUE))) {
							/*
							 * Bundle state = new Bundle();
							 * state.putBoolean(ECHO_ENABLED, true);
							 * updateView(ECHO_STATE, state);
							 */
                        } else if (Arrays
                                .equals(value,
                                        dsc.getConstantBytes(GattDescriptor.DISABLE_NOTIFICATION_VALUE))) {
							/*
							 * Bundle state = new Bundle();
							 * state.putBoolean(ECHO_ENABLED, false);
							 * updateView(ECHO_STATE, state);
							 */
                        }
                    }
                }

            }
        }
    }

    /*class SrvConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mService = ((LeService.LocalBinder) service).getService();
            mService.addListener(mListener);

            int conn = mService.getConnectionState(mDevice);
            if (conn == BluetoothProfile.STATE_DISCONNECTED) {
                onDisconnected();
            } else {
                com.issc.util.Log.d("already connected");
                onConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            com.issc.util.Log.e("Gatt Service disconnected");
        }
    }*/


    class SrvConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mService = ((LeService.LocalBinder) service).getService();
            mService.addListener(mListener);
			/*int conn = mService.getConnectionState(mDevice);
			if (conn == BluetoothProfile.STATE_DISCONNECTED) {
				onDisconnected();
			} else {
				Log.d("already connected");
				onConnected();
			}*/
            // If Adapter is empty, means we never do discovering
            connectToDevice();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            com.issc.util.Log.e("Gatt Service disconnected");


        }
    }

    private void onDiscovered(BluetoothDevice device) {
        com.issc.util.Log.d("on discovered:");
        if (mService != null) {
            List<GattService> srvs = mService.getServices(device);
            com.issc.util.Log.d("discovered result:" + srvs.size());
            Iterator<GattService> it = srvs.iterator();
            while (it.hasNext()) {
                GattService s = it.next();
                //appendService(s);
            }
        }
    }


    private void dismissDiscovery() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mDiscoveringDialog != null && mDiscoveringDialog.isShowing()) {
                    dismissDialog(DISCOVERY_DIALOG);
                }
            }
        });
    }


    private void connectToDevice() {
        com.issc.util.Log.d("connectToDevice connectGatt will be called now");
        mService.connectGatt(this, false, mDevice);
        if (mService.getConnectionState(mDevice) == BluetoothProfile.STATE_CONNECTED) {
            com.issc.util.Log.d("already connected to device");
            List<GattService> list = mService.getServices(mDevice);
            if ((list == null) || (list.size() == 0)) {
                //displayDiscovering();
                com.issc.util.Log.d("start discovering services");
                mService.discoverServices(mDevice);
            } else {

                onDiscovered(mDevice);
            }
        } else {
            com.issc.util.Log.d("Trying to connect to device");
            displayConnecting();
            //boolean init = mService.connect(mDevice, false);
            //Log.d("Try to connec to device, successfully? " + init);
        }
    }

    private void displayConnecting() {
        runOnUiThread(new Runnable() {
            public void run() {
                showDialog(CONNECT_DIALOG);
            }
        });
    }

    private void displayDiscovering() {
        runOnUiThread(new Runnable() {
            public void run() {
                showDialog(DISCOVERY_DIALOG);
            }
        });
    }

    private void dismissConnect() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mConnectionDialog != null && mConnectionDialog.isShowing()) {
                    dismissDialog(CONNECT_DIALOG);
                }
            }
        });
    }




    public void initValues()
    {


        toolbar=(Toolbar)findViewById(R.id.toolbar);
        edtCountTwo=(EditText)findViewById(R.id.edt_count_two);
        edtCountThree=(EditText)findViewById(R.id.edt_count_three);
        edtCountFour=(EditText)findViewById(R.id.edt_count_four);
        edtDays=(EditText)findViewById(R.id.edt_days);

        spinCountry=(Spinner)findViewById(R.id.spin_country);
        spinCity=(Spinner)findViewById(R.id.spin_city);
        edtLatitude=(EditText)findViewById(R.id.edt_latitude);
        edtLongiitude=(EditText)findViewById(R.id.edt_longitude);
        txtCityLatLong=(TextView)findViewById(R.id.txt_city_lat_long);
        radioGroupLanguage=(RadioGroup)findViewById(R.id.rg_language);
        radioButtonFrench=(RadioButton)findViewById(R.id.rb_french);
        radioButtonPersian=(RadioButton)findViewById(R.id.rb_persian);
        radioButtonEnglish=(RadioButton) findViewById(R.id.rb_english);
        radioButtonArabic=(RadioButton)findViewById(R.id.rb_arabic);



        txtChooseLanguage=(TextView)findViewById(R.id.txt_choose_language);
        txtCountFour=(TextView)findViewById(R.id.txt_count_four);
        txtCountThree=(TextView)findViewById(R.id.txt_count_three);
        txtCountTwo=(TextView)findViewById(R.id.txt_count_two);
        txtDays=(TextView)findViewById(R.id.txt_days);
        txtLatitude=(TextView)findViewById(R.id.txt_latitude);
        txtLongitude=(TextView)findViewById(R.id.txt_longitude);
        txtCountry=(TextView)findViewById(R.id.txt_country);
        txtCity=(TextView)findViewById(R.id.txt_city);
        txtSelectedCityLatLong=(TextView)findViewById(R.id.label_city_lat_long);
        txtCityLatLong=(TextView)findViewById(R.id.txt_city_lat_long);
        btnRunGps=(Button)findViewById(R.id.btn_run_gps);
        btnClearCount=(Button)findViewById(R.id.btn_clear_count);
        btnSendLatLong=(Button)findViewById(R.id.btn_send_lat_long);

        editLog=(TextView) findViewById(R.id.edt_log);




        btnRunGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {




                count=0;


                if(mStarted)
                {
                    gpsStop();
                }

                else
                {
                    gpsStart();
                }

                /*if(!mRequestingLocationUpdates)
                {
                    edtLatitude.setText("");
                    edtLongiitude.setText("");
                    selectedLat=0;
                    selectedLong=0;
                    isFirstTime=true;

                    btnRunGps.setText(getString(R.string.stop_gps));
                    startUpdatesButtonHandler(btnRunGps);
                    btnRunGps.setBackgroundResource(R.drawable.ic_btn_pressed);

                    spinCountry.setSelection(0);
                    spinCity.setSelection(0);


                }
                else
                {
                    isFirstTime=true;
                    mLastLocation=null;

                    stopLocationUpdates();
                    btnRunGps.setText(getString(R.string.run_gps));
                    btnRunGps.setBackgroundResource(R.drawable.btn_selector);

                }
*/
                //gpsProgressDialog.show();
               // btnRunGps.setEnabled(false);




               /* try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                btnRunGps.setBackgroundResource(R.drawable.btn_selector);
                btnRunGps.setEnabled(true);*/

            }
        });



        btnSendLatLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if(selectedLat==0||selectedLong==0)
                {
                    Toast.makeText(MainActivity.this,getString(R.string.choose_country_city),Toast.LENGTH_LONG).show();
                }
                else {

                    btnSendLatLong.setBackgroundResource(R.drawable.ic_btn_pressed);
                    btnSendLatLong.setEnabled(false);

                    sendLatLongClicked=true;
                    clearClicked=false;

                    try {

                        CharSequence cs = GpsUtils.getGpsToSend(selectedLat, selectedLong);
                        //msgShow("onClickSend called",cs);
                        //msgShow("send", "\n");
                        //msgShow("", cs);
                        write(cs);

                    }catch (NullPointerException e)
                    {

                        e.printStackTrace();
                    }


                  //  btnSendLatLong.setBackgroundResource(R.drawable.btn_red);


                }



            }
        });


        btnClearCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                btnClearCount.setBackgroundResource(R.drawable.ic_btn_pressed);
                btnClearCount.setEnabled(false);
                clearClicked=true;
                sendLatLongClicked=false;

                try {

                    CharSequence cs = "%RESET%";
                    //msgShow("onClickSend called",cs);
                    // msgShow("send", "\n");
                    // msgShow("", cs);

                    write(cs);

                }
                catch (NullPointerException e)
                {

                    e.printStackTrace();
                }



              //  btnClearCount.setBackgroundResource(R.drawable.btn_red);


            }
        });


    }

    public void setLanguageChecked()
    {
        String lang=PrefUtils.getLanguage(this);
        switch (lang)
        {
            case Constants.LANG_FR:
                radioButtonFrench.setChecked(true);
                break;
            case Constants.LANG_EN:

                radioButtonEnglish.setChecked(true);

                break;
            case Constants.LANG_AR:
                radioButtonArabic.setChecked(true);
                break;

            case Constants.LANG_FA:
                radioButtonPersian.setChecked(true);
                break;

                default:
                    break;
        }
    }



    private synchronized void gpsStart() {
        if (!mStarted) {
            mLocationManager
                    .requestLocationUpdates(mProvider.getName(), 0, 0, this);

            edtLatitude.setText("");
            edtLongiitude.setText("");
            selectedLat=0;
            selectedLong=0;
            isFirstTime=true;

            btnRunGps.setText(getString(R.string.stop_gps));
            //startUpdatesButtonHandler(btnRunGps);
            btnRunGps.setBackgroundResource(R.drawable.ic_btn_pressed);

            spinCountry.setSelection(0);
            spinCity.setSelection(0);
            mStarted = true;


        }

    }

    private synchronized void gpsStop(){
        if (mStarted) {
            mLocationManager.removeUpdates(this);
            mStarted = false;
            isFirstTime=true;
            //mLastLocation=null;
            //selectedLat=0;
           // selectedLong=0;

            //stopLocationUpdates();
            btnRunGps.setText(getString(R.string.run_gps));
            btnRunGps.setBackgroundResource(R.drawable.btn_selector);
            // Stop progress bar

            // Reset the options menu to trigger updates to action bar menu items
          //  invalidateOptionsMenu();
        }

    }


    private void setLanguage(String language){

        SharedData.savePref("lang", language);

        PrefUtils.setLanguage(this,language);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        this.getResources().updateConfiguration(config, this.getResources().getDisplayMetrics());
        setLabels();
        /*Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();*/

    }


    public void setLabels()
    {

        txtChooseLanguage.setText(getString(R.string.choose_language));
        txtCountFour.setText(getString(R.string.count_four));
        txtCountThree.setText(getString(R.string.count_three));
        txtCountTwo.setText(getString(R.string.count_two));
        txtDays.setText(getString(R.string.days));
        btnClearCount.setText(getString(R.string.clear_count));
        txtLatitude.setText(getString(R.string.latitude));
        txtLongitude.setText(getString(R.string.longitude));
        btnRunGps.setText(getString(R.string.run_gps));
        txtCountry.setText(getString(R.string.country));
        txtCity.setText(getString(R.string.city));
        txtSelectedCityLatLong.setText(getString(R.string.city_lat_long));
        btnSendLatLong.setText(getString(R.string.send_lat_long));


        if(countries.size()>0)
        {
            countries.get(0).setCountry(getString(R.string.choose_country));
        }

        if(cities.size()>0)
        {
            cities.get(0).setCity(getString(R.string.choose_city));
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.prayer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.goToPrayerActivity){
            startActivity(new Intent(MainActivity.this,PrayerActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Log.i("Main Activity","Location Result");


                if(mLastLocation==null)
                {
                    mLastLocation = locationResult.getLastLocation();
                    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                    isFirstTime=false;
                }
                else
                {
                    updateLocationUI(locationResult.getLastLocation() );
                }

            }
        };
    }

    /**
     * Uses a {@link LocationSettingsRequest.Builder} to build
     * a {@link LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }



    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        /*if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }*/

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        //Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        //Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                       // updateUI();
                        break;
                }
                break;
        }
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            Log.e("Main Activity","Start Location Updates");
            mRequestingLocationUpdates = true;
            startLocationUpdates();
        }
    }



    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        Log.e("Main Activity","Start Location Updates Inside");



        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i("Main Activity", "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());


                       // updateUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i("Main Activity", "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i("Main Activity", "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                               // Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                       // updateUI();
                    }
                });
    }



    /**
     * Disables both buttons when functionality is disabled due to insuffucient location settings.
     * Otherwise ensures that only one button is enabled at any time. The Start Updates button is
     * enabled if the user is not requesting location updates. The Stop Updates button is enabled
     * if the user is requesting location updates.
     */
    private void setButtonsEnabledState() {
      /*  if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }*/
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude and last update time.
     */


    int count=0;
    private void updateLocationUI(Location location) {

            btnRunGps.setBackgroundResource(R.drawable.btn_selector);
            btnRunGps.setText(getString(R.string.run_gps));

            // btnRunGps.setEnabled(true);
            Log.i("Main Activity", "Location Outside");



                Log.e("Latitude", "LAT+" + location.getLatitude());
                Log.e("Longitude", "LON+" + location.getLongitude());

                if (location.getLatitude() != 0) {
                    selectedLat = location.getLatitude();
                    edtLatitude.setText(GpsUtils.convertLatitudeToGPS(location.getLatitude()) + "");
                }

                if (location.getLongitude() != 0) {
                    selectedLong = location.getLongitude();

                    edtLongiitude.setText(GpsUtils.convertLongitudeToGPS(location.getLongitude()) + "");

                }


                gpsStop();
                count = 0;
                isFirstTime=true;
                mStarted=false;


    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {


        Log.e("Main Activity","Stop Location Updates");

        if (!mRequestingLocationUpdates) {
            //Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                        setButtonsEnabledState();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        /*// Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
          //  startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }*/

       // updateUI();


        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            promptEnableGps();
        }
    }



    /**
     * Ask the user if they want to enable GPS
     */
    private void promptEnableGps() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.enable_gps_message))
                .setPositiveButton(getString(R.string.enable_gps_positive_button),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                        }
                )
                .setNegativeButton(getString(R.string.enable_gps_negative_button),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                )
                .show();
    }


    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        /*stopLocationUpdates();
        btnRunGps.setText(getString(R.string.run_gps));
        btnRunGps.setBackgroundResource(R.drawable.btn_selector);
        mRequestingLocationUpdates=false;
        isFirstTime=true;*/



    }




    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mLastLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);

    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
           // Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            //Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
       // Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                //Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                   // Log.i(TAG, "Permission granted, updates requested, starting location updates");
                   // startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }







    private class LoadCountriesTask extends AsyncTask<Void, Integer, List<City>>
    {

        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {

            progressDialog=new ProgressDialog(MainActivity.this);
            progressDialog.setMessage(getString(R.string.progress_loading));
            progressDialog.show();

            super.onPreExecute();

        }

        @Override
        protected List<City> doInBackground(Void... voids) {


            CityDao cityDao = db.cityDao();
            List<City> scannerResults=cityDao.getCountry();

            return scannerResults;
        }


        @Override
        protected void onPostExecute(List<City> scannerResults) {

            progressDialog.cancel();

            countries=scannerResults;
            setCountryAdapter();

            setLabels();

            super.onPostExecute(scannerResults);
        }
    }



    private class LoadCityTask extends AsyncTask<Void, Integer, List<City>>
    {


        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog=new ProgressDialog(MainActivity.this);
            progressDialog.setMessage(getString(R.string.progress_loading));

            progressDialog.show();
        }

        @Override
        protected List<City> doInBackground(Void... voids) {


            CityDao cityDao = db.cityDao();
            List<City> scannerResults=cityDao.getCityByCountryName(selectedCountryName);

            return scannerResults;
        }


        @Override
        protected void onPostExecute(List<City> scannerResults) {

            progressDialog.cancel();
            cities.clear();
            cities=scannerResults;
            setCityAdapter();
            super.onPostExecute(scannerResults);
        }
    }


    public void setCountryAdapter()
    {
        City city = new City();
        city.setCountry(getString(R.string.choose_country));
        countries.add(0, city);

        CountryAdapter adapter = new CountryAdapter(this,R.layout.item_spinner, countries,true);
        spinCountry.setAdapter(adapter);
        spinCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position > 0) {
                    selectedCountryName=countries.get(position).getCountry();

                    new LoadCityTask().execute();

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    boolean citySelected = false;


    public void setCityAdapter()
    {
        final City city = new City();
        city.setCity(getString(R.string.choose_city));
        cities.add(0, city);




        CountryAdapter adapter = new CountryAdapter(this,R.layout.item_spinner, cities,false);
        spinCity.setAdapter(adapter);


        spinCity.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                citySelected =true;
                return false;
            }
        });
        spinCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position > 0) {


                    if(citySelected) {

                        selectedLat = cities.get(position).getLatitude();
                        selectedLong = cities.get(position).getLongitude();
                        selectedCity = cities.get(position).getCity();

                        Log.e("Latitude",selectedLat+"");
                        Log.e("Longitude",selectedLong+"");


                        //txtCityLatLong.setText(""+selectedLat+" , "+selectedLong+"");
                        edtLatitude.setText(GpsUtils.convertLatitudeToGPS(selectedLat) + "");
                        edtLongiitude.setText(GpsUtils.convertLongitudeToGPS(selectedLong) + "");

                        count = 0;
                        //stopLocationUpdates();
                        gpsStop();
                        mStarted=false;
                        btnRunGps.setText(getString(R.string.run_gps));
                        btnRunGps.setBackgroundResource(R.drawable.btn_selector);


                        citySelected=false;
                    }
                    else
                    {
                        selectedLat = 0;
                        selectedLong = 0;
                    }




                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

                citySelected=false;

            }
        });

    }








    private class InsertTask extends AsyncTask<Void, Integer, Void>

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

                    AssetUtils.insertCity(MainActivity.this, db);
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


            new LoadCountriesTask().execute();


        }
    }





    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //setButtonText("Bluetooth off");
                        Toast.makeText(MainActivity.this,getString(R.string.bluetooth_off),Toast.LENGTH_LONG).show();

                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //setButtonText("Turning Bluetooth off...");
                       // Toast.makeText(MainActivity.this,getString(R.string.disconnected),Toast.LENGTH_LONG).show();

                        break;
                    case BluetoothAdapter.STATE_ON:
                        //setButtonText("Bluetooth on");
                        Toast.makeText(MainActivity.this,getString(R.string.bluetooth_on),Toast.LENGTH_LONG).show();

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        //setButtonText("Turning Bluetooth on...");
                        break;
                }
            }
        }
    };

}
