/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bzbluetooth.android.bluetoothlegatt;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bzbluetooth.R;
import com.bzbluetooth.helper.GattUtils;
import com.bzbluetooth.helper.TokenKeeper;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity implements View.OnClickListener,OnItemClickListener{
	private final String TAG = DeviceScanActivity.this.getClass().getSimpleName();
	
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;

    private boolean AUTO_CONN=true ;//如果要禁止自动连接 修改这个值
    private Handler mHandler;
    /*ui*/
	private ListView mScanListView;
    private Button btnHow,btnAbout,btnScan;
    private ToggleButton btnSwitch;
//	private BluetoothAdapter btAdapt;

	private Button btnClear;

	private ImageView imgHowto;

    private static final int REQUEST_ENABLE_BT = 0x001;
    private static final int REQUEST_ENGINESELECTED = 0x002;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    /**
	 * 
	 */
	public void demo() {
		/*delete FIXME*/
	    String myString = "2015/03/30";
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.CHINA);//
	    Date d;
		try {
			d = sdf.parse(myString);
			if(d.before(new java.util.Date())){
				Toast.makeText(this, "This is a demo app!", Toast.LENGTH_SHORT).show();
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		initScreenData();
	}
	private void initScreenData(){
		DisplayMetrics dm = getResources().getDisplayMetrics();
//		Const.SCREEN_WIDTH = dm.widthPixels;
//		Const.SCREEN_HEIGHT= dm.heightPixels;
		Log.i("MyApplication", dm.densityDpi+":dpi+=+desi:"+dm.density);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.layout_prepare);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int width = dm.widthPixels;// 宽度
		int height = dm.heightPixels ;//高度
		Log.i(TAG, width + "++++++++++++++++++++++++" + height);
        
        lastName = TokenKeeper.getValue(DeviceScanActivity.this, ControlActivity.SP_DEVICENAME);
        lastAddr = TokenKeeper.getValue(DeviceScanActivity.this, ControlActivity.SP_DEVICEADDRESS);
        
        mHandler = new Handler(){
        	private ProgressDialog sDialog;
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case 0:// start scan
					mScanning = true;
					mBluetoothAdapter.startLeScan(mLeScanCallback);
		            invalidateOptionsMenu();
		            
		            sDialog = GattUtils.showProgressDialog(DeviceScanActivity.this, getString(R.string.start_search));
		            sDialog.setCancelable(true);
		            sDialog.setOnCancelListener(new OnCancelListener() {
						
						@Override
						public void onCancel(DialogInterface dialog) {
							mHandler.sendEmptyMessage(1);
							
						}
					});
					break;
				case 1:
					mScanning = false;
		            mBluetoothAdapter.stopLeScan(mLeScanCallback);
		            invalidateOptionsMenu();
		            if(null!=sDialog&&sDialog.isShowing()) sDialog.dismiss();
					break;
				case 9://0211 delay
					scanLeDevice(true);
					break;
				default:
					break;
				}
			}
        	
        };
        
        //init ui
        mScanListView = (ListView) this.findViewById(R.id.lvDevices);
        mScanListView.setOnItemClickListener(this);
        btnHow = (Button) this.findViewById(R.id.howtoButton);
        btnAbout = (Button) this.findViewById(R.id.aboutButton);
        btnScan = (Button) this.findViewById(R.id.scanButton);
        btnSwitch = (ToggleButton) this.findViewById(R.id.tbtnSwitch);
        btnClear = (Button) this.findViewById(R.id.clearButton);
        imgHowto = (ImageView) this.findViewById(R.id.img_howto);
        btnHow.setOnClickListener(this);
        btnAbout.setOnClickListener(this);
        btnScan.setOnClickListener(this);
        btnSwitch.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        imgHowto.setOnClickListener(this);
//        btAdapt = BluetoothAdapter.getDefaultAdapter();// 初始化本机蓝牙功能
//        btnSwitch.setChecked(!btAdapt.isEnabled());
        
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not available", Toast.LENGTH_SHORT).show();
            finish();
        }

        if(initBluetooth()){
        	enableBluetooth();
        }else{
        	//
        }
        
        demo();
        
        goControl("asdf", "13gsdgwe54hgweh4");//FIXME run me when testing 
    }

	/**
	 * 
	 */
	public boolean initBluetooth() {
		// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        mBluetoothAdapter = GattUtils.getBluetoothAdapter(this).getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported!", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }else{
        	return true;
        }
	}


   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }*/

    
    /**
	 * 
	 */
	public void enableBluetooth() {
		// Ensures Bluetooth is enabled on the device. If Bluetooth is not
		// currently enabled,
		// fire an intent to display a dialog asking the user to grant
		// permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.howtoButton://abandon 0202
		case R.id.img_howto:
			GattUtils.showHowto(this);	
			break;
		case R.id.aboutButton://abandon 0202
			/*ad = new Builder(this,AlertDialog.THEME_HOLO_DARK);
			ad.setCancelable(false);
			ad.setTitle("About ENDURO?");
			View view = LayoutInflater.from(this).inflate(R.layout.layout_about, null);//
			ad.setView(view);
			ad.setPositiveButton("OK", null);
			ad.create().show();	*/
			break;
		case R.id.scanButton:
			if(null==mBluetoothAdapter){
//				initBluetooth();
				return;
			}
			if(!mBluetoothAdapter.isEnabled()){
//				enableBluetooth();
				return;
			}
			if (!mScanning) {
				if (null != mLeDeviceListAdapter) {
					mLeDeviceListAdapter.clear();
					scanLeDevice(true);
				}
			}else{
				scanLeDevice(false);
			}
			break;
		case R.id.tbtnSwitch:
			if(null==mBluetoothAdapter) return;
			if (!btnSwitch.isChecked()){
				mBluetoothAdapter.enable();
			}
			else {
				mBluetoothAdapter.disable();
			}
			break;
		case R.id.clearButton:
			if(null!=mLeDeviceListAdapter){
				mLeDeviceListAdapter.clear();
				mLeDeviceListAdapter.notifyDataSetChanged();
			}
			break;
		default:
			break;
		}
	}

	

	/*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }*/

    @Override
	protected void onResume() {
		super.onResume();
		mLeDeviceListAdapter = new LeDeviceListAdapter();
		mScanListView.setAdapter(mLeDeviceListAdapter);

		if (null != mBluetoothAdapter&&mBluetoothAdapter.isEnabled()){
			mHandler.sendEmptyMessageDelayed(9, 1000);//0211
		}			

	}

	@Override
	protected void onPause() {
	    super.onPause();
	    if(null==mBluetoothAdapter||!mBluetoothAdapter.isEnabled()) return;//1217
	    scanLeDevice(false);
	    mLeDeviceListAdapter.clear();
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
        }
        else if(requestCode == REQUEST_ENGINESELECTED && resultCode == RESULT_OK){
        	finish();
        }
    }

    @Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
    	final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        goControl(device.getName(), device.getAddress());
	}

	/**
	 * @param dName
	 * @param dAddr
	 */
	public void goControl(String dName, String dAddr) {
		final Intent intent = new Intent(this, ControlActivity.class);
//		intent.putExtra(ControlActivity.EXTRAS_DEVICE_NAME, dName);//FIXME delete us when testing
//		intent.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS, dAddr);
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
//        startActivity(intent);
        startActivityForResult(intent, REQUEST_ENGINESELECTED);//1230
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        boolean needSelectEngine = !TokenKeeper.getSpInstance(DeviceScanActivity.this).contains("showdianji");//1230
        if(!needSelectEngine) finish();//1230
//        finish();//1222，不需要返回该界面	1230注释，选择engine后再finish
	}


    private void scanLeDevice(final boolean enable) {//modified 1217
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.sendEmptyMessageDelayed(1, SCAN_PERIOD);

            mHandler.sendEmptyMessage(0);
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
        	mHandler.sendEmptyMessage(1);
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
//        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("UNKNOWN DEVICE");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {

				@Override
                public void run() {
                	if(null==mLeDeviceListAdapter) return;//1217
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    Log.i(TAG, device.getAddress()+"_"+device.getName());
                    if(AUTO_CONN&&!(lastName.isEmpty()||lastAddr.isEmpty())){//1224 自动连接上次设备
                    	if(device.getName().equals(lastName)&&device.getAddress().equals(lastAddr)){
                    		scanLeDevice(false);
                    		goControl(lastName, lastAddr);
                    	}
                    }
                }
            });
        }
    };

	private String lastName;

	private String lastAddr;

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}