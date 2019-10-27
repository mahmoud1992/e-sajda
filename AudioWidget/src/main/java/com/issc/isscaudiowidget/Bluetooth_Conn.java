package com.issc.isscaudiowidget;

import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;


public class Bluetooth_Conn extends Application {
	
	private static final String TAG = "BT_Conn";
	private static final boolean D = true;

	private static BluetoothAdapter mAdapter = null;
	private static BluetoothSocket  mSocket  = null;
	private static BluetoothDevice  mDevice = null;
	
	/* RD version */
	/*private BluetoothDevice  headset_Device = null;
	private BluetoothDevice  a2dp_Device = null;*/
	/* RD version */
	
	private boolean spp_status = false;
	private boolean headset_conn = false;  // record there's headset connect or not
	
	// closeSocketThread and flag to indicate closing or not
	private boolean reset = false;
	closeSocketThread t1;
	
	protected boolean service_running = false;
	
	// flags for recording SPP cmd/ack
	private byte cur_cmd  = 0x00;
	private byte cur_cmd_para = 0x00;
	private byte next_cmd = 0x00;
	private byte next_cmd_para = 0x00;
    private boolean reply_screen = false;
    
   // SendVoicePromptThead variables
 	private boolean terminateFromStop = false;
 	private boolean ready = false;
 	private boolean sending = false;
 	private int file_lines = 0;

	//Equalizer Variables
	private boolean EqDataready = false;

 	// Synthesizing variable
 	private boolean synthesizing = false;
 	private boolean has_sdcard = true;
    
	private ConnectedThread mConnectedThread; 
	private sendVoicePromptThread mSendVoicePromptThread;
	private EqDataThread mEqDataThread;
	
	// for SPP, use this UUID
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	@Override
	public void onCreate()
	{
		/* only execute when application run at first time */
		super.onCreate();
		if (D) Log.d(TAG, "[Global Variable] onCreate");
		
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mAdapter == null) {
			if (D) Log.w(TAG, "Device does not support Bluetooth");
        }
		
	}
	
	public void SetSpp(BluetoothDevice dev) {	
		//mDevice = dev;
		
		Intent i = new Intent();
		i.setAction("Headset_Connect");
		sendBroadcast(i);
		
		if (D) Log.w(TAG,"[Blue] Set Spp Link for: " + dev.getName());
		
		if (mSocket == null) {
			if (D) Log.v(TAG,"mSocket = NULL");
			SocketThread mSocketThread = new SocketThread(dev);
	     	mSocketThread.start();
		} else { 
			if (D) Log.v(TAG,"mSocket is not NULL, socket already exist!");
		}
	}
	
	public synchronized void connected(BluetoothSocket socket){
    	if (D) Log.d(TAG, "[connected] at sync");    	
    	mConnectedThread = new ConnectedThread(socket);		
    	mConnectedThread.start();
    }

	
	private class SocketThread implements Runnable {
        private final BluetoothDevice mmDevice;
        private Thread thread = null;
    	public SocketThread(BluetoothDevice device){
    		this.thread = new Thread(this);    	    		 
    	    if (D) Log.i(TAG, "[SocketThread] Enter these server sockets");
    	    mmDevice = device;
    	    BluetoothSocket tmp = null;
    	    
    	    // Get a BluetoothSocket for a connection with the given BluetoothDevice
    	    try {
    	    	this.thread.sleep(5000);
    	    	if (D) Log.d(TAG,"Device address: "+device.getAddress()+" "+device.getName());
    	    	tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
    	    	if (D) Log.i(TAG, "[SocketThread] Constructure: Get a BluetoothSocket for a connection, create Rfcomm");
    	    	if (D) Log.d(TAG,"Socket connected: "+tmp.isConnected()+"address"+tmp.getRemoteDevice().getAddress());
    	    } catch (Exception e) {
    	    	if (mmDevice == null) Log.e(TAG, "Device Null", e);
    	    	Log.e(TAG, "create() failed", e);
    	    }
    	    mSocket = tmp;    	     
    	}
    	
    	public void start(){
    		this.thread.start();
    	}
    	
		@Override
		public void run() {
			if (D) Log.d(TAG, "BEGIN SocketThread: " + this);
			if (mAdapter.isDiscovering())
				mAdapter.cancelDiscovery();
            try {
                // Blocking call. It will only return on a successful connection or an exception
            	mSocket.connect();
            	mDevice = mmDevice;   // store the device which setup SPP link
                if (D) Log.i(TAG, "[SocketThread] Return a successful connection");
            } catch (Exception e) {
            	if (D) Log.w(TAG, "[SocketThread] Connection failed");
            	e.printStackTrace();
            	
                try {
                	mSocket.close();
                    if (D) Log.i(TAG, "[SocketThread] Connect fail, close the socket");
                } catch (IOException e2) {
                	Log.e(TAG, "[IOException] Unable to close() socket during connection failure");
                } catch (Exception e1) {
                	Log.e(TAG, "[Exception] Unable to close() socket during connection failure");
                }
                
                mSocket = null;
				try {
					thread.sleep(5000);
					if (isHeadset()) {
						SetSpp(mmDevice);
					}
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

                this.thread = null;
                
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (Bluetooth_Conn.this) {
    		    connected(mSocket);
                if (D) Log.i(TAG, "[SocketThread] "+mmDevice+" is connected.");
            }
            this.thread = null;
            if (D) Log.i(TAG, "END mConnectThread");
		}
        
    }
	
	public class ConnectedThread implements Runnable {
    	protected BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
    	private Thread thread = null;
    	private boolean alive = true;

        
    	private ConnectedThread(BluetoothSocket socket) {
    		if (D) Log.v(TAG,"ConnectedThread create: "+socket.getRemoteDevice().getName());
    		this.thread = new Thread(this, socket.getRemoteDevice().toString());
    		mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                if (D) Log.i(TAG, "[ConnectedThread] Constructure: Set up bluetooth socket i/o stream");
            } catch (IOException e) {
                Log.e(TAG, "[ConnectedThread] temp sockets not created");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;    
    	}
    	    	
    	public void start(){
            this.thread.start();
    	}
    	
    	public void stop(){
    		alive = false;
    		while (true) {
    			try {
    				Thread.sleep(200);
					if (mConnectedThread == null) 
						break;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    	}
    	
		@Override
		public void run() {
			if (D) Log.d(TAG, "[ConnectedThread] BEGIN: " + this);
			
			/* Notify Activity to update device info */
            if (D) Log.i(TAG,"Send SPP_setup broadcast, spp_status = true");
    		Intent intent1 = new Intent();
            intent1.setAction("SPP_setup");
            sendBroadcast(intent1);
            spp_status = true;
			
			byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	        int count = 0;
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (alive) {
	            try {
	            	count = mmInStream.available();
	            } catch (IOException e) {
	            	Log.e(TAG,"Exception while listing");
	                break;
	            }
	            
	            try {
					if (count != 0) {
		                // Read from the InputStream
	                    bytes = mmInStream.read(buffer, 0, 1024);
	                    if (D) Log.i(TAG,"[ConnectedThread] read start, count: "+count+", read bytes: "+bytes);
	                    
	                    int acks = 0;
	                    
	                    // Case of more than one ack at a time. 4 = 3(header) + 1(checksum)
	                    if ( ((int)buffer[2] + 4) != bytes ) {
	                    	int index = 0; 
	                    	while (index != bytes) {
	                    		if (index > bytes) break;
	                    		index = index + 3 + (int)buffer[index+2] + 1;
	                    		acks += 1;
	                    	}	                    	
	                    	if (D) Log.w(TAG,"[WARN] More than one acks, number of acks: "+acks);
	                    } else 
	                    	acks = 1;
	                    
	                    int i=0;
	                    StringBuilder sb = new StringBuilder();
						for (byte b : buffer) {
					        sb.append(String.format("%02X ", b));
					        i++;
					        if (i == bytes) break;
					    }
						if (D) Log.i(TAG,"Receive: " + sb.toString());
						
						int index = 0; i=0; 
						for (i=0;i<acks;i++)
						{
							Intent intent = new Intent();
							if (D) Log.d(TAG,"i= "+i+", Command id: "+buffer[index+3]);
							
							/* send event ack except for event "command ack" */
							if (buffer[index+3] != 0x00) {
								byte [] event_ack = new byte [6];
								event_ack[0] = (byte) 0xaa; event_ack[1] = (byte) 0x00;
								event_ack[2] = (byte) 0x02; event_ack[3] = (byte) 0x14;
								event_ack[4] = buffer[index+3]; 
								event_ack[5] = (byte) (0x100-0x02-0x14-event_ack[4]);
								Bluetooth_Conn.this.write(event_ack); 
							}
							
							if (buffer[index+3] == 0x18) {
								StringBuilder sb_fw = new StringBuilder();
								sb_fw.append("v");
								sb_fw.append(String.format("%d", buffer[index+5]));
								sb_fw.append(".");
								sb_fw.append(String.format("%02X", buffer[index+6]));
								
								intent.setAction("FW_VER");
			                    intent.putExtra("version", sb_fw.toString());
			                    sendBroadcast(intent);
							} else if (buffer[index+3] == 0x00) {
								StringBuilder sb_cmd_ack = new StringBuilder();
								sb_cmd_ack.append(String.format("%02X", buffer[index+4]));
								sb_cmd_ack.append(String.format("%02X", buffer[index+5]));
								
								/* voice prompt data ack */
								if (buffer[index+4] == 0x20 && buffer[index+5] == 0x00) {
									sending = false;
								} else if (buffer[index+4] == 0x20 && buffer[index+5] == 0x01) {
									/* ack: disallow, use when SCO interrupt voice prompt */
									if (mSendVoicePromptThread != null) {
										mSendVoicePromptThread.stop();
										sending = false;
									}
								}
								
								intent.setAction("CMD_ACK");
			                    intent.putExtra("ack", sb_cmd_ack.toString());
			                    sendBroadcast(intent);
							} else if (buffer[index+3] == 0x27) {
								// GPIO STATUS, response event
								StringBuilder sb_gpio_status = new StringBuilder();
								sb_gpio_status.append(String.format("%02X", buffer[index+4]));
								sb_gpio_status.append(String.format("%02X", buffer[index+5]));
								sb_gpio_status.append(String.format("%02X", buffer[index+6]));
								sb_gpio_status.append(String.format("%02X", buffer[index+7]));
								sb_gpio_status.append(String.format("%02X", buffer[index+8]));
								sb_gpio_status.append(String.format("%02X", buffer[index+9]));
								sb_gpio_status.append(String.format("%02X", buffer[index+10]));
								sb_gpio_status.append(String.format("%02X", buffer[index+11]));
								intent.setAction("GPIO_EVENT");
			                    intent.putExtra("status", sb_gpio_status.toString());
			                    sendBroadcast(intent);
							} else if (buffer[index+3] == 0x2a) {
								if (buffer[index+4] == 0x01)
									ready = true;
							} else if (buffer[index+3] == 0x70) {
								// Key Event
								if (buffer[index+4] == 0x0a && buffer[index+5] == 0x05) {
									intent.setAction("TAKE_PICTURE");
				                    sendBroadcast(intent);
								}
							}else if(buffer[index+3] == 0x10) { //EQ Event mode handling
								if (buffer[index+4] == 0xA) {
									startSendEqData();
									if(isEqDataready() == false)
										SetEqDataready(true);
								}
								else if((buffer[index+4] > 0) && (buffer[index+4] < 0xA))
									//CustomToast.showToast(getBaseContext(), "Default Equalizer is not Custom Equalizer", 2000);
								if (D) Log.i(TAG, "Default Equalizer is not Custom Equalizer");
								//else if(buffer[index+4] == 0) // disable bt_switch in this case or not need to discussed.
								else if(buffer[index+4] == 0)
									//CustomToast.showToast(getBaseContext(), "Equalizer is disabled in the BTM", 2000);
								if (D) Log.i(TAG, "Equalizer is disabled in the BTM");

							}

							index = index + 3 + (int)buffer[index+2] + 1;
						} // end for                 
					}

                } catch (IOException e) {
            	    Log.e(TAG, "[ConnectedThread] connection lost");
                    break;
                }
	        }
	        if (D) Log.i(TAG, "[ConnectedThread] break from while");
	        mConnectedThread = null;
	        if (mDevice != null) resetConnection(mSocket.getRemoteDevice().getAddress());
		}
		
		/* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	    	
	    	StringBuilder sb = new StringBuilder();
			for (byte b : bytes) {
			    sb.append(String.format("%02X ", b));
			}
			if (D) Log.w(TAG,"Send: "+sb.toString());
			
	        try {
	            mmOutStream.write(bytes);
	            if (D) Log.v(TAG, "Write Success");
	        } catch (IOException e) {
	        	Log.e(TAG, "Exception during write");
	        }
	    }	
    }
	
	/**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
    	mConnectedThread.write(out);
    }
	
	
	/* Reset input and output streams and make sure socket is closed. */
    public void resetConnection(String address) {
    	if (mSocket != null && mSocket.getRemoteDevice().getAddress().equals(address)) {
    		if (D) Log.w(TAG,"Reset Connection, address: "+address+", "+mSocket.getRemoteDevice().getAddress());		
			if (reset == false) {
				reset = true;
				t1 = new closeSocketThread();
				t1.start();
			} else {
				if (D) Log.v(TAG,"resetConnection is working");
			}
    	} else {
    		if (mSocket != null) {
    			if (D) Log.d(TAG,"Address not match: "+address+", "+mSocket.getRemoteDevice().getAddress());
    		} else {
    			if (D) Log.d(TAG,"No socket created");
    		}
    	}
    }
    
    public class closeSocketThread implements Runnable {
		private Thread thread = null;
		
		public closeSocketThread() {
			this.thread = new Thread(this);
		}
		
		public void start() {
    		this.thread.start();
    	}
		
		@Override
		public void run() {
			try {
				if (mConnectedThread != null) {
	    			if (D) Log.v(TAG,"mConnectedThread not null, wait until it stops");
	    			mConnectedThread.stop();
	    		}
				
				mDevice = null;
		        try {	
		        	if (D) Log.v(TAG, "[disconnectSocket] Close bluetooth socket: "+mSocket.getRemoteDevice().getAddress());
		        	mSocket.close();
		        	mSocket = null;
		        } catch (IOException e) {
		            Log.e(TAG, "[disconnectSocket] close() of connect socket failed");
		        }
		        
		        if (D) Log.i(TAG,"SPP_disconnect Broadcast");
				Intent intent1 = new Intent();
		        intent1.setAction("SPP_disconnect");
		        sendBroadcast(intent1);
		        
		        spp_status = false;
		        reset = false;
				
			} catch (Exception e) {
				if (D) Log.v(TAG,"closeSocketThread exception");
				e.printStackTrace();
			}
			this.thread = null;
			t1 = null;
		}
		
	}
    
    public class sendVoicePromptThread implements Runnable {
    	private Thread thread = null;
		
		public sendVoicePromptThread() {
			this.thread = new Thread(this);
			byte [] buffer = new byte [7];
			buffer [0] = (byte) 0xaa; buffer[1] = (byte) 0x00;
			buffer [2] = (byte) 0x03; buffer[3] = (byte) 0x20;
			buffer [4] = (byte) 0x00; buffer[5] = (byte) 0x02;
			buffer [6] = (byte) 0xdb;
			Bluetooth_Conn.this.write(buffer);
		}
		
		public void start() {
    		this.thread.start();
    	}
		
		public void stop() {
			terminateFromStop = true;
			ready = true;
		}
		
		@Override
		public void run() {
			while (true) {
				if (ready) break;
			}
			if (D) Log.d(TAG,"Ready at sendVoicePromptThread");
			String s = CallerNameService.getSdcardPath() + "TTSTest/output.h";
			
			File fp = new File(s);
			FileInputStream fis = null;
			
			if (fp.exists() && terminateFromStop == false) {
				try {
					fis = new FileInputStream(fp);
					file_lines = countStreamLines(fis);
					fis.close();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				
			} else {
				if (D) Log.e(TAG,"File does not exist or thread stop");
			}
			
			if (fp.exists() && terminateFromStop == false) {
				try {
					fis = new FileInputStream(fp);
					
					BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
					StringBuilder sb = new StringBuilder();
			        String line = null;
			        
			        int i;
			        for (i=0;i<file_lines;i++) {
			        	line = reader.readLine();
			        	if (line == null) break;
			        	while (true) {
			        		if (sending == false) break;
			        	}
			        	if (terminateFromStop) {
			        		Intent intent1 = new Intent();
					        intent1.setAction("ShowContact");
					        sendBroadcast(intent1);
					        
			        		/* terminate thread when incoming call interrupts, send end packet */
			        		byte buffer [] = new byte[7];
			        		buffer[0]  = (byte) 0xaa; buffer[1]  = (byte) 0x00;
			        		buffer[2]  = (byte) 0x03; buffer[3]  = (byte) 0x20;
			        		buffer[4]  = (byte) 0x01; buffer[5]  = (byte) 0x03;
			        		buffer[6]  = (byte) 0xd9;
			        		Bluetooth_Conn.this.write(buffer);
			        		
			        		try {
			    				Thread.sleep(30);
							} catch (InterruptedException e) {
								Log.e(TAG,"Add dalay between two commands, exception", e);
							}
			        		
			        		/* stop media buffer packet */
			        		buffer[4] = (byte) 0x00; buffer[5] = (byte) 0x00; buffer[6] = (byte) 0xdd;
			        		Bluetooth_Conn.this.write(buffer);
			        		break;
			        	}
			        	sending = true;
			        	
			        	sb.append(line);
				        byte data [] = hexStringToByteArray(sb.toString());
				        byte buffer [] = new byte[data.length+7];			        
				        buffer[0] = (byte) 0xaa;
				        buffer[1] = (byte) ((data.length+3)/256); buffer[2] = (byte) ((data.length+3)%256);
				        buffer[3] = (byte) 0x20;  buffer[4] = (byte) 0x01;
				        if (i==0) {
				        	if (file_lines == 1) {
				        		/* single packet */
				        		buffer[5] = (byte) 0x00;
				        	} else {
				        		/* 1st packet of multiple packets*/
				        		buffer[5] = (byte) 0x01;
				        	}
				        }
				        else if (i==file_lines-1) {
				        	/* end packet */
				        	buffer[5] = (byte) 0x03;
				        } else {
				        	/* continue packet */
				        	buffer[5] = (byte) 0x02;
				        }
				        
				        /* checksum calculation and allocate data to buffer */
				        long checksum = buffer[1] + buffer[2] + buffer[3] + buffer[4] + buffer[5]; 
				        int index = 0;
				        for (byte b : data) {
				        	buffer[index+6] = b;
				        	checksum += b;
				        	index += 1;
				        }
				        
				        buffer[buffer.length-1] = (byte) (256 - (checksum%256));
				        Bluetooth_Conn.this.write(buffer);
				        
				        //if (D) Log.d(TAG,"Checksum check: "+ (checksum+buffer[buffer.length-1])%256 );
						sb.delete(0, sb.length());
			        }
			        reader.close();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			} else {
				if (D) Log.e(TAG,"File does not exist or thread stop");
				/* avoid that FW is in receiving state but no data */
				if (terminateFromStop == true) {
					byte buffer [] = new byte[31];			        
					buffer[0]  = (byte) 0xaa;  buffer[1]  = (byte) 0x00;
	        		buffer[2]  = (byte) 0x1b;  buffer[3]  = (byte) 0x20;
	        		buffer[4]  = (byte) 0x01;  buffer[5]  = (byte) 0x00;
	        		buffer[6]  = (byte) 0x00;  buffer[7]  = (byte) 0x00;
	        		buffer[8]  = (byte) 0x00;  buffer[9]  = (byte) 0x00;
	        		buffer[10] = (byte) 0x00;  buffer[11] = (byte) 0x00;
	        		buffer[12] = (byte) 0x00;  buffer[13] = (byte) 0x00;
	        		buffer[14] = (byte) 0x00;  buffer[15] = (byte) 0x00;
	        		buffer[16] = (byte) 0x00;  buffer[17] = (byte) 0x00;
	        		buffer[18] = (byte) 0x00;  buffer[19] = (byte) 0x00;
	        		buffer[20] = (byte) 0x00;  buffer[21] = (byte) 0x00;
	        		buffer[22] = (byte) 0x00;  buffer[23] = (byte) 0x00;
	        		buffer[24] = (byte) 0x00;  buffer[25] = (byte) 0x00;
	        		buffer[26] = (byte) 0x00;  buffer[27] = (byte) 0x00;
	        		buffer[28] = (byte) 0x00;  buffer[29] = (byte) 0x00;
	        		buffer[30] = (byte) 0xc4;
	        		Bluetooth_Conn.this.write(buffer);
				}
			}
			
			/* reset global variables (flags) */
			terminateFromStop = false;
			sending = false;
			ready = false;
			file_lines = 0;
			mSendVoicePromptThread = null;
			if (D) Log.d(TAG,"End sendVoicePromptThread");
		}
    }

	public class EqDataThread implements Runnable {
		private Thread thread = null;

		public EqDataThread() {
			this.thread = new Thread(this);
		}

		public void start() {
			this.thread.start();
		}

		public void stop() {
			EqDataready = true;
		}

		@Override
		public void run() {
			while (true) {
				if (EqDataready) break;
			}
			if (D) Log.d(TAG, "Ready at EqDataThread");
			int eqcoefLength = 84; // Equalizer data size
			int totalBBLength = eqcoefLength + 6;//(6 = 1 byte header + 2 bytes size + 2 byte command + 1 byte checksum)
			byte buffer[] = new byte[totalBBLength];
			SharedPreferences Eq_dsp_data = getSharedPreferences("com.issc.isscaudiowidget", 0);

			buffer[0] = (byte) 0xaa;
			buffer[1] = 0; buffer[2] = (byte)(eqcoefLength+2);
			buffer[3] = (byte) 0x30;//for dsp online control command ID
			buffer[4] = (byte) 0x13; //for Equalizer Data command(0x2d -> for sound effect command)
			int index = 5;
			long checksum = buffer[1] + buffer[2] + buffer[3]+buffer[4];
			byte bEven,bOdd;
			int eqcoefLengthtmp = eqcoefLength/2;
			for(int i=0;i<eqcoefLengthtmp;i++){
				int coefval = Eq_dsp_data.getInt("EqData_"+ Integer.toString(i),0);

				bEven = (byte)(coefval & 0xFF);
				bOdd = (byte)((coefval>>8) & 0xFF);
				//buffer[2*i + index] = bEven;
				//buffer[2*i+1+index] = bOdd;
				//Write EQ data in Big Endian format
				buffer[2*i + index] = bOdd;
				buffer[2*i+1+index] = bEven;
				checksum += bEven+bOdd;
				if (D) Log.d(TAG,"i :"+Integer.toString(i)+ " \t Eqcoef :" + Integer.toString(coefval));
			}
			buffer[totalBBLength-1] = (byte) (256 - (checksum%256));
			setReplyScreen(true);
			Bluetooth_Conn.this.write(buffer);

			/* reset global Equalizer thread control variables */
			EqDataready = false;
			mEqDataThread = null;
			//EqterminateFromStop = false;
			if (D) Log.d(TAG,"End EqDataThread");
		}
	}
    
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    public static int countStreamLines(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        int line = 0;
        String tmp;
        int data_length = 0;
        while ((tmp = reader.readLine()) != null) {
        	line ++;
        	data_length += tmp.length();
        }
        if (D) Log.d(TAG,"Raw data "+data_length/2+" bytes");
        return line;
    }
    
    public void startSendVoicePrompt() {
    	if (D) Log.d(TAG, "startSendVoicePrompt");
    	mSendVoicePromptThread = new sendVoicePromptThread();
    	mSendVoicePromptThread.start();
    }
    
    public void stopSendVoicePrompt() {
    	if (D) Log.d(TAG, "stopSendVoicePrompt");
    	mSendVoicePromptThread.stop();
    }
    
    public boolean isSendVoicePromptThread() {
    	if (mSendVoicePromptThread == null)
    		return false;
    	else
    		return true;
    }

	public void startSendEqData() {
		if (D) Log.d(TAG, "startSendEqData");
		mEqDataThread = new EqDataThread();
		mEqDataThread.start();
	}

	public void stopSendEqData() {
		if (D) Log.d(TAG, "stopSendEqData");
		mEqDataThread.stop();
	}

	public boolean isSendEqDataThread() {
		if (mEqDataThread == null)
			return false;
		else
			return true;
	}
    
    /* RD version */
    /*public ServerSocketThread mServerSocketThread = null;
    
    public synchronized void startSession(){
    	if (D) Log.d(TAG, "[startSession] ServerSocketThread start...");
    		
    	if (mServerSocketThread == null) {
    		Log.i(TAG, "[startSession] mServerSocketThread is dead");
    	    mServerSocketThread = new ServerSocketThread();
    	    mServerSocketThread.start();
    	} else {
    		Log.i(TAG, "[startSession] mServerSocketThread is alive : "+this);
    	}
    }
    
    public void disconnectServerSocket() {
    	Log.d(TAG, "[disconnectServerSocket] ----------------");
    	if (mServerSocketThread != null) {
    		mServerSocketThread.disconnect();
    	    mServerSocketThread = null; 
    	    Log.w(TAG, "[disconnectServerSocket] NULL mServerSocketThread"); 
    	}
    }
    
    
    private class ServerSocketThread implements Runnable {
    	private BluetoothServerSocket mmServerSocket = null;
    	private Thread thread = null;
    	private boolean isServerSocketValid = false;
    	public ServerSocketThread(){
    		this.thread = new Thread(this);
    		
    		BluetoothServerSocket serverSocket = null;    		
    		try {
    			 Log.i(TAG, "[ServerSocketThread] Enter the listen server socket");
    			 serverSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord("Bluetooth_Conn", MY_UUID);
    			 //Log.i(TAG, "[ServerSocketThread] serverSocket hash code = "+serverSocket.hashCode());
    			 isServerSocketValid = true;

            } catch (IOException e) {
            	 Log.e(TAG, "[ServerSocketThread] Constructure: listen() failed", e);
                 e.printStackTrace();           
                 isServerSocketValid = false;
                 mServerSocketThread = null;
            }             
            mmServerSocket = serverSocket; 

            //String serverSocketName = mmServerSocket.toString();
            //Log.i(TAG, "[ServerSocketThread] serverSocket name = "+serverSocketName);
    	}    	
    	public void start(){
            this.thread.start();
    	}
    	
		@Override
		public void run() {
			if (D) Log.d(TAG, "BEGIN ServerSocketThread " + this);
			//BluetoothSocket socket = null;
			
			while( isServerSocketValid ) {
				try {
					Log.i(TAG, "[ServerSocketThread] Enter while loop");
					//Log.i(TAG, "[ServerSocketThread] serverSocket hash code = "+mmServerSocket.hashCode());				
                    //socket = mmServerSocket.accept();
					mSocket = mmServerSocket.accept();
                   
                    Log.i(TAG, "[ServerSocketThread] Got client socket");                    
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                
                //if (socket!=null) {
				if (mSocket!=null) {
                	 synchronized (Bluetooth_Conn.this) {                		 
                		 //Log.i(TAG, "[ServerSocketThread] "+socket.getRemoteDevice()+" is connected.");
                		 Log.i(TAG, "[ServerSocketThread] "+mSocket.getRemoteDevice()+" is connected.");
                		 //mDevice = socket.getRemoteDevice();
                		 mDevice = mSocket.getRemoteDevice();
                		 //connected(socket);
                		 connected(mSocket);
                		 Bluetooth_Conn.this.disconnectServerSocket();              		 
                		 break;                		 
                	 }
                } else {
                	Log.e(TAG, "[ServerSocketThread] mSocket = NULL");
                }
			}	
			Log.i(TAG, "[ServerSocketThread] break from while");
			Bluetooth_Conn.this.startSession();
		}
		
        public void disconnect() {
            //if (D) Log.d(TAG, "[ServerSocketThread] disconnect " + this);
            try {
            	//Log.i(TAG, "[ServerSocketThread] disconnect serverSocket name = "+mmServerSocket.toString());
                mmServerSocket.close();
                Log.i(TAG, "[ServerSocketThread] mmServerSocket is closed.");
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }    	
        
    }*/

    /* RD version */
    
    /* Manage TTS synthesizing status  */
    public void setSynthesizing (boolean flag) {
    	synthesizing = flag;
    }    
    public boolean isSynthesizing() {
    	return synthesizing;
    }

	/* Manage EqData ready status  */
	public void SetEqDataready (boolean flag) {
		EqDataready = flag;
	}
	public boolean isEqDataready() {
		return EqDataready;
	}
    
    /* Manage device has external storage status  */
    public void setHasSD (boolean flag) {
    	has_sdcard = flag;
    }
    public Boolean isHasSD () {
    	return has_sdcard;
    }

	public Boolean isHeadset() {
		return headset_conn;
	}
	
	public void setHeadset(boolean value) {
		headset_conn = value;
	}
	
	public BluetoothDevice getDevice() {
		return mDevice; 
	}

	public BluetoothAdapter getAdapter() {
		return mAdapter;
	}

	public boolean getSppStatus() {
		return spp_status;
	}
	
	public void setCurCmd(byte s) {
		cur_cmd = s;
	}
	
	public byte getCurCmd() {
		return cur_cmd;
	}
	
	public void setCurCmdPara(byte cur) {
		cur_cmd_para = cur;
	}
	
	public byte getCurCmdPara() {
		return cur_cmd_para;
	}
	
	public void setNextCmd(byte next) {
		next_cmd = next;
	}
	
	public byte getNextCmd() {
		return next_cmd;
	}
	
	public void setNextCmdPara(byte next) {
		next_cmd_para = next;
	}
	
	public byte getNextCmdPara() {
		return next_cmd_para;
	}
	
	public void setReplyScreen(boolean b) {
		reply_screen = b;
	}
	
	public boolean getReplyScreen() {
		return reply_screen;
	}
	
	/* RD version */
	/*public void set_HSP_device(BluetoothDevice dev) {
		headset_Device = dev;
	}
	public void set_A2DP_device(BluetoothDevice dev) {
		a2dp_Device = dev;
	}
	public BluetoothDevice get_HSP_device() {
		return headset_Device;
	}
	public BluetoothDevice get_A2DP_Device() {
		return a2dp_Device;
	}*/
	/* RD version */
}