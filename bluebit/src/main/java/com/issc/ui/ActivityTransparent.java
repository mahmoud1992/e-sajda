// vim: et sw=4 sts=4 tabstop=4
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.issc.ui;

import com.issc.Bluebit;
import com.issc.gatt.Gatt;
import com.issc.gatt.GattCharacteristic;
import com.issc.gatt.GattDescriptor;
import com.issc.gatt.GattService;
import com.issc.impl.LeService;
import com.issc.impl.GattTransaction;
import com.issc.R;
import com.issc.reliableburst.ReliableBurstData;
import com.issc.reliableburst.ReliableBurstData.ReliableBurstDataListener;
import com.issc.util.Log;
import com.issc.util.Util;
import com.issc.util.TransactionQueue;

import java.io.FileOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ActivityTransparent extends Activity implements
		TransactionQueue.Consumer<GattTransaction> {

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
	private final static int CONNECT_DIALOG   = 2;

	private final static int TIMER_DIALOG = 3;
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
	private TextView mMsg;
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

	private ReliableBurstDataListener transmitListener;
	private boolean reTry = false;
	private ProgressDialog mDiscoveringDialog;
	private final static int DISCOVERY_DIALOG = 4;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trans);

		mQueue = new TransactionQueue(this);

		mMsg = (TextView) findViewById(R.id.trans_msg);
		mInput = (EditText) findViewById(R.id.trans_input);
		mBtnSend = (Button) findViewById(R.id.trans_btn_send);
		mToggleEcho = (ToggleButton) findViewById(R.id.echo_toggle);
		mToggleResponse = (ToggleButton) findViewById(R.id.trans_type);
		mEchoIndicator = (CompoundButton) findViewById(R.id.echo_indicator);

		mViewHandler = new ViewHandler();

		mTabHost = (TabHost) findViewById(R.id.tabhost);
		mTabHost.setup();
		addTab(mTabHost, "Tab1", "Raw", R.id.tab_raw);
		addTab(mTabHost, "Tab2", "Timer", R.id.tab_timer);
		addTab(mTabHost, "Tab3", "Echo", R.id.tab_echo);

		mMsg.setMovementMethod(ScrollingMovementMethod.getInstance());
		registerForContextMenu(mMsg);

		mDevice = getIntent().getParcelableExtra(Bluebit.CHOSEN_DEVICE);

		mListener = new GattListener();
		initSpinners();

		mLogBuf = new ArrayList<CharSequence>();

		/* Transparent is not a leaf activity. connect service in onCreate */
		mConn = new SrvConnection();
		bindService(new Intent(this, LeService.class), mConn, Context.BIND_AUTO_CREATE);

        //Log.d("MADHU LOG");

		transmit = new ReliableBurstData();
		transmitListener = new ReliableBurstDataListener() {
			@Override
			public void onSendDataWithCharacteristic(
					ReliableBurstData reliableBurstData,
					final BluetoothGattCharacteristic transparentDataWriteChar) {
				runOnUiThread(new Runnable() {
					public void run() {
						Log.d("onSendDataWithCharacteristic runOnUiThread");
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
	}

	private void didGetData(String s) {
		Log.d("didGetData");
		synchronized (mQueue) {
			mQueue.onConsumed();
            msgShow("", "\n");
			msgShow("wrote ", s);
            msgShow("", "\n");
			if (mQueue.size() == 0 && mStartTime != null) {
				final long elapse = Calendar.getInstance()
						.getTimeInMillis()
						- mStartTime.getTimeInMillis();
				//Handler handler = new Handler();
				Runnable runnable = new Runnable() {
					
					@Override
					public void run() {

						msgShow("time", "spent " + (elapse / 1000)
								+ " seconds" + "  Throughput: " + (total_bytes/(elapse / 1000))
                                + " bytes/sec");
                        total_bytes = 0;
						mSuccess = 0;
						mFail = 0;
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
		//mQueue.clear();
		mQueue.destroy();
		//disableNotification();
		closeStream();
		mViewHandler.removeCallbacksAndMessages(null);

		/*
		 * Transparent is not a leaf activity. disconnect/unregister-listener in
		 * onDestroy
		 */
		mService.rmListener(mListener);
		mService = null;
		unbindService(mConn);
		super.onDestroy();
	}

	private void initSpinners() {
		Resources res = getResources();

		mSpinnerDelta = (Spinner) findViewById(R.id.timer_delta);
		mSpinnerSize = (Spinner) findViewById(R.id.timer_size);
		mSpinnerRepeat = (Spinner) findViewById(R.id.timer_repeat);

		mValueDelta = res.getIntArray(R.array.delta_value);
		mValueSize = res.getIntArray(R.array.size_value);
		mValueRepeat = res.getIntArray(R.array.repeat_value);

		initSpinner(R.array.delta_text, mSpinnerDelta);
		initSpinner(R.array.size_text, mSpinnerSize);
		initSpinner(R.array.repeat_text, mSpinnerRepeat);

		mSpinnerDelta.setSelection(3); // supposed to select 1000ms
		mSpinnerSize.setSelection(19); // supposed to select 20bytes
		mSpinnerRepeat.setSelection(0); // supposed to select Unlimited
	}

	private void initSpinner(int textArrayId, Spinner spinner) {
		ArrayAdapter<CharSequence> adapter;
		adapter = ArrayAdapter.createFromResource(this, textArrayId,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}

	private void addTab(TabHost host, String tag, CharSequence text,
			int viewResource) {
		View indicator = getLayoutInflater().inflate(R.layout.tab_indicator,
				null);
		TextView tv = (TextView) indicator.findViewById(R.id.indicator_text);
		tv.setText(text);

		TabHost.TabSpec spec = host.newTabSpec(tag);
		spec.setIndicator(indicator);
		spec.setContent(viewResource);
		host.addTab(spec);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);
		if (v == mMsg) {
			menu.setHeaderTitle("Message Area");
			menu.add(0, MENU_CLEAR, Menu.NONE, "Clear");
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == MENU_CLEAR) {
			mLogBuf.clear();
			mMsg.setText("");
			mMsg.scrollTo(0, 0);
		}
		return true;
	}

	public void onClickSend(View v) {
        Log.d("onClickSend called");

		CharSequence cs = mInput.getText();
        //msgShow("onClickSend called",cs);
        msgShow("send", "\n");
		msgShow("", cs);
		write(cs);
		mInput.setText("");
	}

	public void onClickStartTimer(View v) {
		showDialog(TIMER_DIALOG);
		startTimer();
	}

	public void onClickCompare(View v) {
		Intent i = new Intent(this, ActivityFileChooser.class);
		i.putExtra(Bluebit.CHOOSE_PATH, Bluebit.DATA_DIR);
		startActivityForResult(i, COMPARE_FILE);
	}

	public void onClickChoose(View v) {
		Intent i = new Intent(this, ActivityFileChooser.class);
		i.putExtra(Bluebit.CHOOSE_PATH, Bluebit.DATA_DIR);
		startActivityForResult(i, CHOOSE_FILE);
	}

	public void onClickType(View v) {
		onSetType(mToggleResponse.isChecked());
	}

	public void onClickEcho(View v) {
		onSetEcho(mToggleEcho.isChecked());
	}

	private void onSetType(boolean withResponse) {
		Log.d("set write with response:" + withResponse);
	}

	private void onSetEcho(boolean enable) {
		if (enable) {
			// enableNotification();
			openStream(Bluebit.DEFAULT_LOG);
			Bundle state = new Bundle();
			state.putBoolean(ECHO_ENABLED, true);
			updateView(ECHO_STATE, state);

		} else {
			// disableNotification();
			closeStream();
			Bundle state = new Bundle();
			state.putBoolean(ECHO_ENABLED, false);
			updateView(ECHO_STATE, state);
		}
	}

	private void enableNotification() {
		Log.d("calling mService.setCharacteristicNotification:Activity Transperent");
		boolean set = mService.setCharacteristicNotification(mTransTx, true);
		Log.d("set notification:" + set);
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

	private void disableNotification() {
		boolean set = mService.setCharacteristicNotification(mTransTx, false);
		Log.d("set notification:" + set);
		GattDescriptor dsc = mTransTx
				.getDescriptor(Bluebit.DES_CLIENT_CHR_CONFIG);
		dsc.setValue(dsc
                .getConstantBytes(GattDescriptor.DISABLE_NOTIFICATION_VALUE));
		GattTransaction transaction = new GattTransaction(dsc,
				dsc.getConstantBytes(GattDescriptor.DISABLE_NOTIFICATION_VALUE));
		// mQueue.add(transaction);
		// mQueue.process();

		boolean success = mService.writeDescriptor(dsc);
		Log.d("writing disable descriptor:" + success);

	}

	private void openStream(String path) {
		try {
			File file = new File(path);
			if (!file.exists()) {
				Log.w("Target file does not exist, create: " + path);
				File parent = file.getParentFile();
				Log.w("make dirs:" + parent.getPath());
				parent.mkdirs();
				file.createNewFile();
			}

			mStream = new FileOutputStream(file, false);
		} catch (IOException e) {
			msgShow("open stream fail", e.toString());
			e.printStackTrace();
		}
	}

	private void closeStream() {
		Log.d("closeStream");
		try {
			if (mStream != null) {
				mStream.flush();
				mStream.close();
			}
		} catch (IOException e) {
			msgShow("close stream fail", e.toString());
			e.printStackTrace();
		}

		mStream = null;
	}

	private void writeToStream(byte[] data) {
		Log.d("inside writeToStream mStream:" + mStream );
		//msgShow("recv", data);
		if (mStream != null) {
			try {
				mStream.write(data, 0, data.length);
				mStream.flush();
			} catch (IOException e) {
				msgShow("write fail", e.toString());
				e.printStackTrace();
			}
		}
	}

	private void compareFile(String pathA, String pathB) {
		try {
			if (mTempStartTime == null)
				Log.d("mTempStartTime is null");
			if (mTempStartTime != null) {
				final long elapse = Calendar.getInstance()
						.getTimeInMillis()
						- mTempStartTime.getTimeInMillis();
				Handler handler = new Handler();
				Runnable runnable = new Runnable() {

					@Override
					public void run() {

						msgShow("time", "spent " + (elapse / 1000)
								+ " seconds");

					}
				};
				handler.postDelayed(runnable, 2000);
			}
			mStartTime = null;
			mEndTime = null;
            mTempStartTime = null;
			String md5A = Util.getMD5FromBytes(Util.readBytesFromFile(pathA));
			String md5B = Util.getMD5FromBytes(Util.readBytesFromFile(pathB));
			msgShow(pathA, md5A);
            msgShow("", "\n");
			msgShow(pathB, md5B);
            msgShow("", "\n");
			if (md5A.equals(md5B)) {
				msgShow("compare :", "Match");
                msgShow("", "\n");
			} else {
				msgShow("compare :", "Not Match");
                msgShow("", "\n");
			}
		} catch (IOException e) {
			msgShow("comapre fail", e.toString());
			e.printStackTrace();
		}
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
			msgShow("recv", recv);
			write(data);
			//writeToStream(data);
			msgShow("echo", recv);
		}
		Bundle msg = new Bundle();
		msg.putCharSequence(INFO_CONTENT, sb);
		updateView(APPEND_MESSAGE, msg);
	}

	private void onReciveData(byte[] data) {
        //Log.d("[R}");
		StringBuffer sb = new StringBuffer();
		if (data == null) {
			sb.append("Received empty data");
			Bundle msg = new Bundle();
		    Log.d("going for msg.putCharSequence(INFO_CONTENT, sb)");
		    msg.putCharSequence(INFO_CONTENT, sb);
		    updateView(APPEND_MESSAGE, msg);
		} else {
			String recv = new String(data);
			msgShow("", recv);
			writeToStream(data);
		}

	}

	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		Log.d("onActivityResult called with request " + request);
		Log.d("onActivityResult called with result" + result);
		if (request == CHOOSE_FILE) {
			if (result == Activity.RESULT_OK) {
				Uri uri = data.getData();
				final String filePath = uri.getPath();
				Log.d("chosen file:" + filePath);
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							mStartTime = Calendar.getInstance();
							write(Util.readBytesFromFile(filePath));
							msgShow("send", filePath);
						} catch (IOException e) {
							e.printStackTrace();
							Log.d("IO Exception");
						}

					}
				});
				thread.start();
			}
		} else if (request == COMPARE_FILE) {
			if (data != null) {
				openStream(Bluebit.DEFAULT_LOG);
				Uri uri = data.getData();
				final String filePath = uri.getPath();
				mRunnable = new Runnable() {
					@Override
					public void run() {
						closeStream();
                        Log.d("inside run");
                        compareFile(filePath, Bluebit.DEFAULT_LOG);
						mRunnable = null;
					}
				};
			}
		}
	}

	private void msgShow(CharSequence prefix, CharSequence cs) {
		StringBuffer sb = new StringBuffer();

        Log.d("count:" + countx);
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
		Log.d("write(CharSequence cs)");
		write(bytes);
	}

	/**
	 * Write data to remote device.
	 */
	private void write(final byte[] bytes) {
		Log.d(" write before writeThread.post");
		writeThread.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mQueue) {
                    Log.d(" write inside thread run");
                    ByteBuffer buf = ByteBuffer.allocate(bytes.length);
                    buf.put(bytes);
                    buf.position(0);
                    while (buf.remaining() != 0) {
                        int size = (buf.remaining() > transmit.transmitSize()) ? transmit
                                .transmitSize() : buf.remaining();
                        byte[] dst = new byte[size];
                        buf.get(dst, 0, size);
                        GattTransaction t = new GattTransaction(mTransRx, dst);
                        mQueue.add(t);
                        Log.i("size" + mQueue.size());
                        if (mQueue.size() == 1) {
                            mQueue.process();
                        }
                    }
                }
            }
        });
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
		}else if (id == TIMER_DIALOG) {
			mTimerDialog = new ProgressDialog(this);
			mTimerDialog.setMessage("Timer is running");
			mTimerDialog.setOnCancelListener(new Dialog.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Log.d("some one canceled me");
					stopTimer();
				}
			});
			return mTimerDialog;
		}
		return null;
	}

	private void onTimerSend(int count, int size) {
		/* max is 20 */
		//String out = String.format("%020d", count);

		String out = "";
        int tempcount = count;

		if (out.length() > size) {
			// if too long
			out = out.substring(out.length() - size);
		}



		Log.d("count = "+count +"size = "+ size);
        count++;
		while(out.length() < (size-1)) {

            //if (count == 0) count = 1;

			if(count > 9) count = count % 10;
			String count1 = String.format("%d",count);
			out = out + count;

		}
        out = out + "\n";

		Log.d("String"+out);
		//Log.d(out);
		//out = out + "\n";
		//Log.d("After newline"+out);
        FileString = FileString + out;

        Log.d("tempcount :"+tempcount);

        /*if (tempcount == 99) {

           File path = getApplicationContext().getFilesDir();
            Log.d("Path: "+path);
            //File file = new File(path, "my-file-name.txt");
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext().openFileOutput("config.txt", Context.MODE_PRIVATE));
                outputStreamWriter.write(FileString);
                outputStreamWriter.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }*/

		msgShow("send", out);
		write(out);
	}

	private boolean mRunning;

	private void startTimer() {

		final int delta = mValueDelta[mSpinnerDelta.getSelectedItemPosition()];
		final int size = mValueSize[mSpinnerSize.getSelectedItemPosition()];
		final int repeat = mValueRepeat[mSpinnerRepeat
				.getSelectedItemPosition()];
		mRunning = true;
		Log.d("startTimer");
		Thread runner = new Thread() {
			public void run() {
				int counter = 0;
				try {
					while (mRunning) {
						if (repeat != 0 && repeat == counter) {
							stopTimer();
						} else {
							onTimerSend(counter, size);
							sleep(delta);
							counter++;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				updateView(DISMISS_TIMER_DIALOG, null);
			}
		};
		runner.start();
	}

	private void stopTimer() {
		mRunning = false;
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
		Log.d("appendMsg");
		if (mLogBuf.size() > MAX_LINES) {
			mLogBuf.remove(0);
		}

		StringBuffer text = new StringBuffer();
		for (int i = 0; i < mLogBuf.size(); i++) {
			text.append(mLogBuf.get(i));
		}
        Log.d("appendMsg text"+text);
		mMsg.setText(text);



	}

	private void onConnected() {
		List<GattService> list = mService.getServices(mDevice);
		Log.d("onConnected");
		if ((list == null) || (list.size() == 0)) {
			Log.d("no services, do discovery");
			mService.discoverServices(mDevice);
		} else {
			onDiscovered();
		}
	}

	private void onDisconnected() {
		Log.d("transparent activity disconnected, closing");
		stopTimer();
		mStartTime = null;
        mTempStartTime =null;
		mQueue.clear();
		this.setResult(Bluebit.RESULT_REMOTE_DISCONNECT);
		this.finish();
	}

	private void onDiscovered() {
		updateView(DISMISS_CONNECTION_DIALOG, null);
        Log.d(" onDiscovered ActivityTransparent Bluebit.CHR_ISSC_TRANS_TX" + Bluebit.CHR_ISSC_TRANS_TX);
		GattService proprietary = mService.getService(mDevice,
                Bluebit.SERVICE_ISSC_PROPRIETARY);

		mTransTx = proprietary.getCharacteristic(Bluebit.CHR_ISSC_TRANS_TX);
		mTransRx = proprietary.getCharacteristic(Bluebit.CHR_ISSC_TRANS_RX);
		mAirPatch = proprietary.getCharacteristic(Bluebit.CHR_AIR_PATCH);
		proprietary = mService.getService(mDevice, Bluebit.SERVICE_ISSC_AIR_PATCH_SERVICE);
		if (proprietary.getImpl() != null) {
			mAirPatch = proprietary.getCharacteristic(Bluebit.CHR_AIR_PATCH);
		}
		Log.d(String.format("found Tx:%b, Rx:%b, AirPatch:%b",
				mTransTx != null, mTransRx != null, mAirPatch != null));
		onSetEcho(mToggleEcho.isChecked());
		 enableNotification();
		// sendVendorMPEnable();
		BluetoothGatt gatt = (BluetoothGatt) mService.getGatt().getImpl();
		if (gatt != null) {
			BluetoothGattCharacteristic air_ch = (BluetoothGattCharacteristic) mAirPatch
					.getImpl();
			transmit.enableReliableBurstTransmit(gatt, air_ch);
		}
	}

	@Override
	public void onTransact(GattTransaction t) {
		Log.d("onTransact Activity transperant ");
		if (t.isForDescriptor()) {
			GattDescriptor dsc = t.desc;
			boolean success = mService.writeDescriptor(dsc);
			Log.d("writing " + dsc.getCharacteristic().getUuid().toString()
					+ " descriptor:" + success);
		} else {
            Log.d("onTransact t.isForDescriptor() false");
			t.chr.setValue(t.value);
            Log.d("Value : " + t.value);
            String str1 = new String(t.value);
            Log.d("Value (string) : "+ str1);
			if (t.isWrite) {
                Log.d("GattCharacteristic.WRITE_TYPE_DEFAULT"+ GattCharacteristic.WRITE_TYPE_DEFAULT);
                Log.d("GattCharacteristic.WRITE_TYPE_NO_RESPONSE"+ GattCharacteristic.WRITE_TYPE_NO_RESPONSE);
				int type = mToggleResponse.isChecked() ? GattCharacteristic.WRITE_TYPE_DEFAULT
						: GattCharacteristic.WRITE_TYPE_NO_RESPONSE;
				t.chr.setWriteType(type);
                Log.d("mToggleResponse.isChecked()  ;  type : "+mToggleResponse.isChecked() +type);
                Log.d("!t.chr.getUuid().equals(Bluebit.CHR_AIR_PATCH)"+ !t.chr.getUuid().equals(Bluebit.CHR_AIR_PATCH));
				if (type == GattCharacteristic.WRITE_TYPE_NO_RESPONSE
						&& !t.chr.getUuid().equals(Bluebit.CHR_AIR_PATCH)) {
					synchronized (mQueue) {
                        Log.d("calling canSendReliableBurstTransmit");
						if (transmit.canSendReliableBurstTransmit()) {
							BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) t.chr
									.getImpl();
                            Log.d("calling reliableBurstTransmit");
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

	class ViewHandler extends Handler {
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			if (bundle == null) {
				Log.d("ViewHandler handled a message without information");
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
					final int amount = mMsg.getLayout().getLineTop(
							mMsg.getLineCount())
							- mMsg.getHeight();
					if (amount > 0) {
						mMsg.scrollTo(0, amount);
					}
				}
			} else if (tag == ECHO_STATE) {
				mEchoIndicator.setChecked(bundle
						.getBoolean(ECHO_ENABLED, false));
			}
		}
	}

	class GattListener extends Gatt.ListenerHelper {

		GattListener() {
			super("ActivityTransparent");
		}

		@Override
		public void onConnectionStateChange(Gatt gatt, int status, int newState) {

            Log.d("onConnectionStateChange: DATA TRANSFER " );
			if (!mDevice.getAddress().equals(gatt.getDevice().getAddress())) {
				// not the device I care about
				return;
			}
			
			if (reTry) {
				if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					mService.connectGatt(ActivityTransparent.this, false, mDevice);
				}
				else {
					transmit = null;
					Log.d("ReliableBurstData :DATA TRANSFER" );
					transmit = new ReliableBurstData();
					transmit.setListener(transmitListener);
					onConnected();
                    Log.d("setting board id for trnasmitdata" + Bluebit.board_id );
					transmit.setBoardId(Bluebit.board_id);
					reTry = false;
				}
				return;
			}
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				dismissConnect();
				onConnected();

				Log.d("connected to device, start discovery");
				//displayDiscovering();
				//mService.discoverServices(mDevice);

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				onDisconnected();
			}	
		}

		@Override
		public void onServicesDiscovered(Gatt gatt, int status) {
			//onDiscovered();
			dismissDiscovery();
			onDiscovered();
		}

		@Override
		public void onCharacteristicRead(Gatt gatt, GattCharacteristic charac,
				int status) {
            Log.d("onCharacteristicRead");
			Log.d("read char, uuid=" + charac.getUuid().toString());
			byte[] value = charac.getValue();
			Log.d("get value, byte length:" + value.length);
			for (int i = 0; i < value.length; i++) {
				Log.d("[" + i + "]" + Byte.toString(value[i]));
			}
			synchronized (mQueue) {
				mQueue.onConsumed();
			}
		}

		@Override
		public void onCharacteristicChanged(Gatt gatt, GattCharacteristic chrc) {
			Log.d("on chr changed");
			byte arr[] = new byte[] {0x55, 0x44, 0x55};
			Log.d(" onCharacteristicChanged Bluebit.CHR_ISSC_TRANS_TX" +Bluebit.CHR_ISSC_TRANS_TX);
			if (chrc.getUuid().equals(Bluebit.CHR_ISSC_TRANS_TX)) {
				Log.d("onCharacteristicChanged:  getUuid successful ");
                Log.d("mRunnable" + mRunnable);
				Log.d("onCharacteristicChanged mToggleEcho.isChecked " + mToggleEcho.isChecked());
                Log.d("read char, uuid=" + chrc.getUuid().toString());
                byte[] value = chrc.getValue();
                Log.d("get value, byte length:" + value.length);
                String buffer = "";
                for (int i = 0; i < value.length; i++) {
                    buffer = buffer+String.format("%02X ", value[i]);
                }
                Log.d("[" + buffer + "]");
                //msgShow("send", cs);
                onReciveData(chrc.getValue());
				//onEcho(arr);
				if (mToggleEcho.isChecked()) {
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
					Log.d("read char, uuid=" + chrc.getUuid().toString());
					//byte[] value = chrc.getValue();
					Log.d("get value, byte length:" + value.length);
					//String buffer = "";
					for (int i = 0; i < value.length; i++) {
    					buffer = buffer+String.format("%02X ", value[i]);
					}
					Log.d("[" + buffer + "]");
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
            Log.d("onCharacteristicWrite");
            Log.d("onCharacteristicWrite");
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
					Log.d("onCharacteristicWrite isReliableBurstTransmit success transmit.isBusy()"+
                            " "+ transmit.isBusy());
					if (!transmit.isBusy() && charac.getUuid().equals(Bluebit.CHR_AIR_PATCH)) {
						onSetEcho(mToggleEcho.isChecked());
						enableNotification();
					}
				}
			} else {
				synchronized (mQueue) {
					mQueue.onConsumed();	
				}
				if (charac.getUuid().equals(Bluebit.CHR_AIR_PATCH)) {
					Log.i("Write AirPatch Characteristic:" + status);
				} else {

					Log.d("---------------");

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
			Log.d("onDescriptorWrite");
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
							onSetEcho(mToggleEcho.isChecked());
							enableNotification();
						}
					}
				}
			} else {
				mQueue.onConsumed();
				if (dsc.getCharacteristic().getUuid()
						.equals(Bluebit.CHR_AIR_PATCH)) {
					if (status == Gatt.GATT_SUCCESS) {
						Log.i("Write AirPatch Descriptor Success");
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
			Log.e("Gatt Service disconnected");
		}
	}

	private void onDiscovered(BluetoothDevice device) {
		Log.d("on discovered:");
		if (mService != null) {
			List<GattService> srvs = mService.getServices(device);
			Log.d("discovered result:" + srvs.size());
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
		Log.d("connectToDevice connectGatt will be called now");
		mService.connectGatt(this, false, mDevice);
		if (mService.getConnectionState(mDevice) == BluetoothProfile.STATE_CONNECTED) {
			Log.d("already connected to device");
			List<GattService> list = mService.getServices(mDevice);
			if ((list == null) || (list.size() == 0)) {
				//displayDiscovering();
				Log.d("start discovering services");
				mService.discoverServices(mDevice);
			} else {

				onDiscovered(mDevice);
			}
		} else {
			Log.d("Trying to connect to device");
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


}
