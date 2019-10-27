package com.issc.isscaudiowidget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class CameraActivity extends Activity implements SurfaceHolder.Callback {

	Camera camera;
	SurfaceView surfaceView;
	SurfaceHolder surfaceHolder;
	
	int orientation;
	
	private class MyOrientationDetector extends OrientationEventListener {
		
		public MyOrientationDetector(Context context, int rate) {
			super(context, rate);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			//Log.i("Camera", "orientation = "+orientation);
			if (orientation == ORIENTATION_UNKNOWN) {
				CameraActivity.this.orientation = Surface.ROTATION_0;
			}
			if (orientation >= 315 || orientation < 45) {
				CameraActivity.this.orientation = Surface.ROTATION_0;
			}
			else if (orientation >= 45 && orientation < 135) {
				CameraActivity.this.orientation = Surface.ROTATION_90;
			}
			else if (orientation >= 135 && orientation < 225) {
				CameraActivity.this.orientation = Surface.ROTATION_180;
			}
			else if (orientation >= 225 && orientation < 315) {
				CameraActivity.this.orientation = Surface.ROTATION_270;
			}
			else {
				CameraActivity.this.orientation = Surface.ROTATION_0;
			}
		}
		
	}
	MyOrientationDetector orientationDetector; 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		surfaceView = (SurfaceView) findViewById(R.id.cameraView);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("TAKE_PICTURE");
		registerReceiver(mBroadcast, intentFilter);
		orientationDetector = new MyOrientationDetector(this,SensorManager.SENSOR_DELAY_NORMAL);
		orientationDetector.enable();
	}

	
	@Override
	protected void onDestroy() {
		unregisterReceiver(mBroadcast);
		orientationDetector.disable();
		super.onDestroy();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.camera, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		 if (camera != null){
			  try {
			   camera.setPreviewDisplay(surfaceHolder);
			   camera.startPreview();
			   camera.setDisplayOrientation(90);
			  } catch (IOException e) {
			   e.printStackTrace();
			  }	
		 }
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		camera.stopPreview();
		camera.release();
		camera = null;		
	}
	
	private BroadcastReceiver mBroadcast =  new BroadcastReceiver() {

		@Override
	    public void onReceive(Context mContext, Intent mIntent) {
        	if (mIntent.getAction().equals("TAKE_PICTURE")) {
        		Log.i("Camera", "TakePickure");
        		camera.takePicture(myShutterCallback,myPictureCallback_RAW, myPictureCallback_JPG);
        	}
		}
	};
	
	ShutterCallback myShutterCallback = new ShutterCallback(){

		 @Override
		 public void onShutter() {
		 
		 }
	};

		PictureCallback myPictureCallback_RAW = new PictureCallback() {

		 @Override
		 public void onPictureTaken(byte[] arg0, Camera arg1) {
		 
		 }
	};

		PictureCallback myPictureCallback_JPG = new PictureCallback(){

		 @Override
		 public void onPictureTaken(byte[] arg0, Camera arg1) {
			 Uri uriTarget = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
			 OutputStream imageFileOS;
			  try {
			   imageFileOS = getContentResolver().openOutputStream(uriTarget);
			   imageFileOS.write(arg0);
			   imageFileOS.flush();
			   imageFileOS.close();
			    try {
			        ExifInterface exifi = new ExifInterface(uriTarget.getPath());
			        int degrees = 0;
			        switch (orientation)
			        {
			        case Surface.ROTATION_0:
			            degrees = ExifInterface.ORIENTATION_ROTATE_90;
			            break;
			        case Surface.ROTATION_90:
			            degrees = ExifInterface.ORIENTATION_ROTATE_180;
			            break;
			        case Surface.ROTATION_180:
			            degrees = ExifInterface.ORIENTATION_ROTATE_270;
			            break;
			        case Surface.ROTATION_270:
			            degrees = ExifInterface.ORIENTATION_NORMAL;
			            break;
			        }
	                Log.i("ISSCAudioWidget", "rotation " +orientation +" degrees " + degrees);
			        exifi.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(degrees));
			        exifi.saveAttributes();
			    } catch (IOException e) {
			        Log.e("ISSCAudioWidget", "Exif error");
			    }
			   Toast.makeText(CameraActivity.this, "Image saved: " + uriTarget.toString(), Toast.LENGTH_LONG).show();
			   MediaScannerConnection.scanFile(CameraActivity.this,
		                new String[] { uriTarget.getPath() }, null,
		                new MediaScannerConnection.OnScanCompletedListener() {
		            public void onScanCompleted(String path, Uri uri) {
		                Log.i("ExternalStorage", "Scanned " + path + ":");
		                Log.i("ExternalStorage", "-> uri=" + uri);
		            }
		        });
			  } catch (FileNotFoundException e) {
			   e.printStackTrace();
			  } catch (IOException e) {
			   e.printStackTrace();
			  }
			  camera.startPreview();
		 }
	};
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "ISSCAudioWidget");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("ISSCAudioWidget", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
}
