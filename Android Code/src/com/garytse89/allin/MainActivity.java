package com.garytse89.allin;
 
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

  
public class MainActivity extends Activity {
  private static final String TAG = "bluetooth1";
  private PowerManager.WakeLock wl;
  
  
  private Handler handler;
  
  private SoundMeter mSensor;
  private TextView val;
  
  Button btnOn, btnOff;
    
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private OutputStream outStream = null;
    
  // SPP UUID service 
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  
  // MAC-address of Bluetooth module (you must edit this line)
  private static String address = "00:06:66:49:54:31";
    
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
   
    setContentView(R.layout.activity_main);
  
    // Wakelock
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNjfdhotDimScreen");
    
    
    btnOn = (Button) findViewById(R.id.btnOn);
    btnOff = (Button) findViewById(R.id.btnOff);
      
    btAdapter = BluetoothAdapter.getDefaultAdapter();
    checkBTState();
    
    btnOn.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          sendData("1");
          Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
        }
      });
    
      btnOff.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          sendData("0");
          Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
        }
      });
    
    
    // Sound-based code
    
    TextView val = (TextView) this.findViewById(R.id.val);
   
    mSensor = new SoundMeter();
  
    try {
    	mSensor.start();
	} catch (IllegalStateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}        
    
    // Starts the main running "loop"/thread
    //Thread myThread = new Thread(mPollTask);
    //myThread.start();
    //
    
    
    handler = new Handler();
    final Runnable r = new Runnable() {
	    
		public void run() {
	    	//mSensor.start();
	    	Log.d("Amplify","Sensor initiated.");
	    	
	    	    	
	    	/**
	    	
	    	**/
	    		runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    	
                    		/**
                    		try {
                				Thread.sleep(1000);
                			} catch (InterruptedException e) {
                				// TODO Auto-generated catch block
                				e.printStackTrace();
                			}
                    		**/
                    	Log.d("Amplify","runOnUiThread");
                    	// Get the volume from 0 to 255 in 'int'
                    	double volume = 9 * mSensor.getTheAmplitude() / 32768;
                    	int volumeToSend = (int) volume;
            	        updateTextView(String.valueOf(volumeToSend)); 
            	        Log.d("Amplify",String.valueOf(volumeToSend));
            	        sendData(Integer.toString(volumeToSend));
            	        handler.postDelayed(this, 250); // amount of delay between every cycle of volume level detection + sending the data  out
                    }
                });
	
	    		
	        
	    	
	    	}
		 };
		 
		 handler.postDelayed(r, 1000);
    
  }
   
  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
      if(Build.VERSION.SDK_INT >= 10){
          try {
              final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
              return (BluetoothSocket) m.invoke(device, MY_UUID);
          } catch (Exception e) {
              Log.e(TAG, "Could not create Insecure RFComm Connection",e);
          }
      }
      return  device.createRfcommSocketToServiceRecord(MY_UUID);
  }
    
  @Override
  public void onResume() {
    super.onResume();
    wl.acquire();
    // Sound based code
    try {
		start();
	} catch (IllegalStateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    //
    
    
    Log.d(TAG, "...onResume - try connect...");
    
    // Set up a pointer to the remote node using it's address.
    BluetoothDevice device = btAdapter.getRemoteDevice(address);
    
    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
    
    try {
        btSocket = createBluetoothSocket(device);
    } catch (IOException e1) {
        //errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
    }
        
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
    
    // Establish the connection.  This will block until it connects.
    Log.d(TAG, "...Connecting...");
    try {
      btSocket.connect();
      Log.d(TAG, "...Connection ok...");
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        //errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
      
    // Create a data stream so we can talk to server.
    Log.d(TAG, "...Create Socket...");
  
    try {
      outStream = btSocket.getOutputStream();
    } catch (IOException e) {
      //errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
    }
  }
  
  @Override
  public void onPause() {
    super.onPause();
    wl.release(); // Wakelock
    Log.d(TAG, "...In onPause()...");
  
    if (outStream != null) {
      try {
        outStream.flush();
      } catch (IOException e) {
        //errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
      }
    }
  
    try     {
      btSocket.close();
    } catch (IOException e2) {
      //errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
    }
  }
    
  private void checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on
    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) { 
      //errorExit("Fatal Error", "Bluetooth not support");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth ON...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
      }
    }
  }
  
  private void errorExit(String title, String message){
    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    finish();
  }
  
  private void sendData(String message) {
    byte[] msgBuffer = message.getBytes();
  
    Log.d(TAG, "...Send data: " + message + "...");
  
    try {
      outStream.write(msgBuffer);
    } catch (IOException e) {
      String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
      if (address.equals("00:00:00:00:00:00")) 
        msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
        msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
        
        //errorExit("Fatal Error", msg);       
    }
  }


  	// Sound-related code starts here

	
   

    public void updateTextView(String toThis) {

        TextView val = (TextView) findViewById(R.id.val);
        val.setText(toThis);

        return;
    }

    private void start() throws IllegalStateException, IOException {
        mSensor.start();
    }

	private void stop() {
	    mSensor.stop();
	}

	private void sleep() {
	        mSensor.stop();
	}
    
}