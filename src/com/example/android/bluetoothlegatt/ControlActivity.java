package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bzbluetooth.R;
import com.example.bzbluetooth.R.layout;

public class ControlActivity extends Activity {
	final static String TAG = "ControlActivity";
	

	public static final String EXTRAS_DEVICE_ADDRESS = "deviceaddress";
	public static final String EXTRAS_DEVICE_NAME = "devicename";
	
	private boolean mConnected = false;
	
	private BluetoothLeService mBluetoothLeService;
	
	// Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


	private String mDeviceName;
	private String mDeviceAddress;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_operate);
		
		final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		
//		mGattServicesList.setOnChildClickListener(servicesListClickListner);
//        mConnectionState = (TextView) findViewById(R.id.connection_state);
		
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }
	@Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }
	
	// Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
//                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();
                Toast.makeText(ControlActivity.this, "connected", Toast.LENGTH_SHORT).show();
                send();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
//                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
                Toast.makeText(ControlActivity.this, "disconnected", Toast.LENGTH_SHORT).show();
//                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
            	//TODO
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
	
    byte[] WriteBytes = new byte[20];
	void send(){
		
		final BluetoothGattCharacteristic characteristic =
                mGattCharacteristics.get(groupPosition).get(childPosition);
		
		byte[] value = new byte[20];
        value[0] = (byte) 0x00;
//        if(editTextName.getText().length() > 0){
//            //write string
//            WriteBytes= editTextName.getText().toString().getBytes();
//        }else if(editTextNumEditText.getText().length() > 0){
//            WriteBytes= hex2byte(editTextNumEditText.getText().toString().getBytes());
//        }
        WriteBytes = "1234abcd".getBytes();
        characteristic.setValue(value[0],
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        characteristic.setValue(WriteBytes);

        mBluetoothLeService.writeCharacteristic(characteristic);
	}
	
	private void displayData(String data) {
        if (data != null) {
        	Log.i(TAG, "data:"+data);
            Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
        }
    }
}
