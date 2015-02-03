package com.bzbluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bzbluetooth.R;
import com.bzbluetooth.helper.CompassHelper;
import com.bzbluetooth.helper.GattUtils;
import com.bzbluetooth.helper.MultiToucher;

@Deprecated
public class MainActivity extends Activity {
//	private Button discoverButton = null;
//	private Button scanButton = null;
//	private Button checkButton = null;
//	private BluetoothAdapter adapter = null;
	private LinearLayout oprate_ll,prepare_ll,dianji_ll;
	private String result;
	private String command;
	private String imei;
//	private String ctrl_id;
	private String check_str;  	//动态计算crc8码
	private Thread mThread;
	private String token;		//控制盒的码
	public String box_token; 	//遥控器的码
	private String broken_str;	//截断的返回码
	private String blt_addr_str;//自动连接的蓝牙地址
	private boolean needBB = true;
	private TextView lvTitle;
	private boolean needSetNormalSignal = false;
	private boolean needfengming = true;  // 对码成功是否需要蜂鸣
	private boolean hasSignal = false; 		  //是否显示信号图标
	
	private ImageView diya_img,gaoya_img,xinhao_img,guozai_img;
	private ImageView campass_img;//taotao 1117
	
	private SharedPreferences sharedPrefrences;
//	private Editor editor;
	
	OutputStream mmOutputStream;
	InputStream mmInputStream;
	Thread workerThread;
	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker;
	
  	static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
	Button btnSearch, btnDis, btnExit, clearButton, howtoButton, aboutButton;
	
	ImageButton songkaiBtn,jiajinBtn,upBtn, downBtn,upleftBtn,
	uprightBtn,downleftBtn,downrightBtn;
	ToggleButton tbtnSwitch;
	
	
	//---------------------------
	BluetoothDevice remoteDevice;
	//---------------------------
	
	ListView lvBTDevices;
	ArrayAdapter<String> adtDevices;
	List<String> lstDevices = new ArrayList<String>();
	BluetoothAdapter btAdapt;
	public static BluetoothSocket btSocket;
	
	private HomeKeyEventBroadCastReceiver receiver;
	private Editor editorToken;
	private Editor editorBox;
	private Editor editorAuto;

	
	 /**
	  * 动态计算CRC8
	  */
	 public String computeCRC8(String str,int flag){
		
		 byte[] buffer = {
				 (byte)Integer.parseInt(str.substring(0, 2),16),
				 (byte)Integer.parseInt(str.substring(2, 4),16),
				 (byte)Integer.parseInt(str.substring(4, 6),16),
				 (byte)Integer.parseInt(str.substring(6, 8),16),
				 (byte)Integer.parseInt(str.substring(8, 10),16),
				 (byte)flag};
		 int tmp =  compute(buffer);
		 String c = Integer.toHexString(tmp);
		 if(c.length() == 1){
			c = "0"+c;
		 }
//		 Log.d("--", "--------------------crc8 结果---------->" + c);
		 return c.toUpperCase();
	 }
	 
    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.activity_main);
        
        //注册home键监听
		receiver = new HomeKeyEventBroadCastReceiver();
		registerReceiver(receiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
		//注册电源监听
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);  
	    registerReceiver(mBatInfoReceiver, filter); 
	    
        sharedPrefrences = this.getSharedPreferences("TOKEN", MODE_WORLD_READABLE);	
        editorToken = sharedPrefrences.edit();      
        token = sharedPrefrences.getString("TOKEN", "");
        
        sharedPrefrences = this.getSharedPreferences("BOX_TOKEN", MODE_WORLD_READABLE);	
        editorBox = sharedPrefrences.edit();      
        box_token = sharedPrefrences.getString("BOX_TOKEN", "");     
    
        sharedPrefrences = this.getSharedPreferences("AUTO_BLT", MODE_WORLD_READABLE);	
        editorAuto = sharedPrefrences.edit();      
        blt_addr_str = sharedPrefrences.getString("AUTO_BLT", "");  
        
        TelephonyManager telephonyManager= (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        imei = telephonyManager.getDeviceId().substring(4, 10);     

        if(!box_token.equals("")){
        	check_str ="550301"+box_token +"CC"+ computeCRC8("0301"+box_token,204) +"EE";
        }else{
        	check_str ="550301"+imei +"CC"+ computeCRC8("0301"+imei,204) +"EE";
        }
        
        // Button 设置
		btnSearch = (Button) this.findViewById(R.id.scanButton);
		btnSearch.setOnClickListener(new ClickEvent());
		btnExit = (Button) this.findViewById(R.id.exitButton);
		btnExit.setOnClickListener(new ClickEvent());
//		btnDis = (Button) this.findViewById(R.id.btnDis);
		btnDis.setOnClickListener(new ClickEvent());
		
		subMultiToucher = new SubMultiToucher();
		
		upBtn = (ImageButton) this.findViewById(R.id.upBtn);
		upBtn.setOnTouchListener(subMultiToucher);	
		downBtn = (ImageButton) this.findViewById(R.id.downBtn);
		downBtn.setOnTouchListener(subMultiToucher);	
		
		upleftBtn = (ImageButton) this.findViewById(R.id.upleftBtn);
		upleftBtn.setOnTouchListener(subMultiToucher);	
		uprightBtn = (ImageButton) this.findViewById(R.id.uprightBtn);
		uprightBtn.setOnTouchListener(subMultiToucher);	
		downleftBtn = (ImageButton) this.findViewById(R.id.downleftBtn);
		downleftBtn.setOnTouchListener(subMultiToucher);	
		downrightBtn = (ImageButton) this.findViewById(R.id.downrightBtn);
		downrightBtn.setOnTouchListener(subMultiToucher);
		
		songkaiBtn = (ImageButton) this.findViewById(R.id.songkaiBtn);
		songkaiBtn.setOnClickListener(new ClickEvent());
		jiajinBtn = (ImageButton) this.findViewById(R.id.jiajinBtn);
		jiajinBtn.setOnClickListener(new ClickEvent());
		clearButton = (Button)this.findViewById(R.id.clearButton);
		clearButton.setOnClickListener(new ClickEvent());
		aboutButton = (Button)this.findViewById(R.id.aboutButton);
		aboutButton.setOnClickListener(new ClickEvent());
		howtoButton = (Button)this.findViewById(R.id.howtoButton);
		howtoButton.setOnClickListener(new ClickEvent());
		
		
		lvTitle = (TextView) this.findViewById(R.id.lvTitle);
		oprate_ll = (LinearLayout) this.findViewById(R.id.oprate_ll);
		prepare_ll = (LinearLayout) this.findViewById(R.id.prepare_ll);
		dianji_ll = (LinearLayout) this.findViewById(R.id.dianji_ll);
		
		//警号灯
		diya_img 	= (ImageView) this.findViewById(R.id.diya_img);
		gaoya_img 	= (ImageView) this.findViewById(R.id.gaoya_img);
		xinhao_img 	= (ImageView) this.findViewById(R.id.xinhao_img);
		guozai_img 	= (ImageView) this.findViewById(R.id.guozai_img);
		
		campass_img = (ImageView) this.findViewById(R.id.zhinanzhen_img);//taotao 1117
		cmpsHelper = new CompassHelper(this, campass_img);
		
		// ToogleButton设置
		tbtnSwitch = (ToggleButton) this.findViewById(R.id.tbtnSwitch);
		tbtnSwitch.setOnClickListener(new ClickEvent());

		
		// ListView及其数据源 适配器
		lvBTDevices = (ListView) this.findViewById(R.id.lvDevices);
		adtDevices = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, lstDevices);
		lvBTDevices.setAdapter(adtDevices);
		lvBTDevices.setOnItemClickListener(new ItemClickEvent());

		btAdapt = BluetoothAdapter.getDefaultAdapter();// 初始化本机蓝牙功能

		// ========================================================
		// modified by jason0539 搜索jason0539进入我的博客
		/*
		 * if (btAdapt.getState() == BluetoothAdapter.STATE_OFF)// 读取蓝牙状态并显示
		 * tbtnSwitch.setChecked(false); else if (btAdapt.getState() ==
		 * BluetoothAdapter.STATE_ON) tbtnSwitch.setChecked(true);
		 */
		if (btAdapt.isEnabled()) {
			tbtnSwitch.setChecked(false);
		} else {
			tbtnSwitch.setChecked(true);
		}
		// ============================================================
		// 注册Receiver来获取蓝牙设备相关的结果
		IntentFilter intent = new IntentFilter();
		intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver来取得搜索结果
		intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(searchDevices, intent);
		
		
		if (btAdapt.getState() == BluetoothAdapter.STATE_OFF) {// 如果蓝牙还没开启
			Toast.makeText(MainActivity.this, "Please switch Bluetooth on first", 1000).show();
			return;
		}
		if (btAdapt.isDiscovering()){
			btAdapt.cancelDiscovery();
		}
			
		lstDevices.clear();
		Object[] lstDevice = btAdapt.getBondedDevices().toArray();
		
		BluetoothDevice device = null;
		
		for (int i = 0; i < lstDevice.length; i++) {
			device = (BluetoothDevice) lstDevice[i]; 
			
			if(blt_addr_str.equals(device.getAddress())){
				BluetoothDevice mDevice = btAdapt.getRemoteDevice(blt_addr_str);
				if(mDevice.getBondState() != BluetoothDevice.BOND_BONDED){//判断给定地址下的device是否已经配对  
			         try{  
			              ClsUtils.setPin(mDevice.getClass(), mDevice, "1234"); // 手机和蓝牙采集器配对
			              ClsUtils.createBond(mDevice.getClass(), mDevice);
			              ClsUtils.cancelPairingUserInput(mDevice.getClass(), mDevice);
			              remoteDevice = mDevice; 
			              connect(remoteDevice);
			          }  
			          catch (Exception e) {
			          }    
				}  
				else {  
					remoteDevice = mDevice; 
					connect(remoteDevice);
				} 	
			}
			
			String str = "Paired successfully|" + device.getName() + "|"
					+ device.getAddress();
			lstDevices.add(str); // 获取设备名称和mac地址
			adtDevices.notifyDataSetChanged();
		}
		if(lstDevices.size() > 0){
			lvTitle.setVisibility(View.VISIBLE);
		}
//		setTitle("本机蓝牙地址：" + btAdapt.getAddress());
		btAdapt.startDiscovery();		
		
		
//		afterConnectUIChange();//TODO
	}
    
	class HomeKeyEventBroadCastReceiver extends BroadcastReceiver {

		static final String SYSTEM_REASON = "reason";
		static final String SYSTEM_HOME_KEY = "homekey";//home key
		static final String SYSTEM_RECENT_APPS = "recentapps";//long home key
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
				String reason = intent.getStringExtra(SYSTEM_REASON);
				if (reason != null) {
					if (reason.equals(SYSTEM_HOME_KEY)) {
						// home key处理点
						Toast.makeText(MainActivity.this, "按home键", Toast.LENGTH_SHORT).show();
					} else if (reason.equals(SYSTEM_RECENT_APPS)) {
						// long home key处理点
						Toast.makeText(MainActivity.this, "长按home键", Toast.LENGTH_SHORT).show();
					}
				}
			}
		}
	}
	
	private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {  
        @Override  
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();  
            if(Intent.ACTION_SCREEN_OFF.equals(action)) {
            	Log.d("--", "-------------屏幕关闭了----------------->");
            	if (btSocket != null){
					Log.d("", "---------socket  closed--------->");
					try {
						btSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
            	finish();
            } 
        }  
    }; 
    
    
	private BroadcastReceiver searchDevices = new BroadcastReceiver() {

//		String strPsw = "1234";
		
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Bundle b = intent.getExtras();
			Object[] lstName = b.keySet().toArray();
			
			// 显示所有收到的消息及其细节
			for (int i = 0; i < lstName.length; i++) {
				String keyName = lstName[i].toString();
				Log.e(keyName, String.valueOf(b.get(keyName)));
			}
			BluetoothDevice device = null;
			// 搜索设备时，取得设备的MAC地址
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBondState() == BluetoothDevice.BOND_NONE) {
					String str = "Failed to pair|" + device.getName() + "|" + device.getAddress();
					if (lstDevices.indexOf(str) == -1)// 防止重复添加
						lstDevices.add(str); // 获取设备名称和mac地址
					adtDevices.notifyDataSetChanged();
				}
			}
			else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				switch (device.getBondState()) {
				case BluetoothDevice.BOND_BONDING:
					Log.d("BlueToothTestActivity", "正在配对......");
					break;
				case BluetoothDevice.BOND_BONDED:
					Log.d("BlueToothTestActivity", "完成配对");
					connect(device);//连接设备
					break;
				case BluetoothDevice.BOND_NONE:
					Log.d("BlueToothTestActivity", "取消配对");
				default:
					break;
				}
			}
//			else if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")){
//				Log.d("-----------", "自动配对");	
//	            BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);		 
//	            // byte[] pinBytes = BluetoothDevice.convertPinToBytes("1234");
//	            // device.setPin(pinBytes);
//	            Log.d("-------------------", "ddd");
//	            try{
//	                ClsUtils.setPin(btDevice.getClass(), btDevice, strPsw); // 手机和蓝牙采集器配对
//	                ClsUtils.createBond(btDevice.getClass(), btDevice);
//	                ClsUtils.cancelPairingUserInput(btDevice.getClass(), btDevice);
//	            }catch (Exception e){
//	                // TODO Auto-generated catch block
//	                e.printStackTrace();
//	            }
//	        }
		}
	};

	@Override
	protected void onDestroy() {
		this.unregisterReceiver(searchDevices);
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	class ItemClickEvent implements AdapterView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
			
			if(btAdapt.isDiscovering())	btAdapt.cancelDiscovery();
			String str = lstDevices.get(arg2);
			String[] values = str.split("\\|");
			
			String address = values[2];
			Log.e("address", values[2]);
			BluetoothDevice btDev = btAdapt.getRemoteDevice(address);

			try {
				Boolean returnValue = false;
				if (btDev.getBondState() == BluetoothDevice.BOND_NONE) {
					//利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
					Method createBondMethod = BluetoothDevice.class
							.getMethod("createBond");
					Log.d("BlueToothTestActivity", "开始配对");
					returnValue = (Boolean) createBondMethod.invoke(btDev);
					
				}else if(btDev.getBondState() == BluetoothDevice.BOND_BONDED){
					connect(btDev);				
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}
	
	/**
	 * 蓝牙连接成功之后UI切换
	 */
	private void afterConnectUIChange(){
		AlertDialog.Builder ad = new Builder(MainActivity.this,AlertDialog.THEME_HOLO_DARK);
		ad.setCancelable(false);
		ad.setTitle("Please select the type of your mover");
		ad.setPositiveButton("Automatic Engaging Mover", ui_change_0_listener);
		ad.setNegativeButton("Manual Engaging Mover", ui_change_1_listener);
		ad.create().show();	
		
		RingtoneUtils.playRingtone(MainActivity.this);
	}
	
	//带电机
	private DialogInterface.OnClickListener ui_change_0_listener = new DialogInterface.OnClickListener() {
		
		public void onClick(DialogInterface dialog, int which) {    						//电机松开	
			oprate_ll.setVisibility(View.VISIBLE);
			prepare_ll.setVisibility(View.GONE);
		}
	};
	//不带电机
	private DialogInterface.OnClickListener ui_change_1_listener = new DialogInterface.OnClickListener() {
		
		public void onClick(DialogInterface dialog, int which) {    						//电机松开	
			oprate_ll.setVisibility(View.VISIBLE);
			prepare_ll.setVisibility(View.GONE);
			dianji_ll.setVisibility(View.GONE);
		}
	};
	
	private void connect(BluetoothDevice btDev) {
		UUID uuid = UUID.fromString(SPP_UUID);
		try {
			btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
			Log.d("BlueToothTestActivity", "开始连接socket...");	
			Log.d("--", "----------------0-------------->");
			btSocket.connect();
			Log.d("--", "----------------1-------------->");
			mThread = new Thread(new MyThread());
			mThread.start();
			Log.d("--", "----------------2-------------->");
			mmOutputStream = btSocket.getOutputStream();
			mmInputStream = btSocket.getInputStream();
			Log.d("--", "----------------3-------------->");
			editorAuto.putString("AUTO_BLT", btDev.getAddress());
			editorAuto.commit();//提交
			Log.d("--", "----------------4-------------->");
			afterConnectUIChange();
		    beginListenForData();
		    
		} catch (IOException e) {
			Toast.makeText(MainActivity.this, "connect failed...", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	Handler handler1 = new Handler() {
	    public void handleMessage(Message msg) {
	    	if (btSocket != null){
//				Log.d("", "------------------> send <------" + check_str);
	    		if(hasSignal){
	    			 Message mMsg = new Message();	    			 
			         mMsg.what = 5;
			         handler2.sendMessage(mMsg);
	    		}else{
	    			 Message mMsg = new Message();	    			 
			         mMsg.what = 6;
			         handler2.sendMessage(mMsg);
	    		}
	    		sendDataToPairedDevice(check_str);
	    		hasSignal = true;
			}
	        super.handleMessage(msg);
	    }
	};
	
	public class MyThread implements Runnable {
	    @Override
	    public void run() {
	        while (true) {
	            try {
	                Thread.sleep(300);// 每隔n毫秒发送一次
	                Message message = new Message();
	                message.what = 1;
	                handler1.sendMessage(message);// 发送消息
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	}
	
	/*@Deprecated
	class TouchEvent implements View.OnTouchListener{

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(box_token.equals("")){ 	//未对码的情况下禁止用户操作
				Toast.makeText(MainActivity.this, "请先进行对码操作", Toast.LENGTH_LONG).show();
				return false;
			}
			if(event.getAction() == MotionEvent.ACTION_DOWN)
			{				
				if( v == upBtn ){
					if (btSocket != null){
						String upString = "550301"+box_token+"20"+computeCRC8("0301"+box_token, 32)+"EE";
						Log.d("", "------------前进------>" + upString);  
						sendDataToPairedDevice(upString);
					}
				} else if( v == downBtn ){
					if (btSocket != null){		
						String upString = "550301"+box_token+"25"+computeCRC8("0301"+box_token, 37)+"EE";
						Log.d("", "------------后退------>" + upString);  
						sendDataToPairedDevice(upString);
					}
				} 
				else if(v == upleftBtn ){
					if (btSocket != null){						
						String upString = "550301"+box_token+"22"+computeCRC8("0301"+box_token, 34)+"EE";	
						Log.d("", "------------前右转------>" + upString); 
						sendDataToPairedDevice(upString);
					}
				} else if( v == uprightBtn){
					if (btSocket != null){
						String upString = "550301"+box_token+"21"+computeCRC8("0301"+box_token, 33)+"EE";						
						Log.d("", "------------前左转------>" + upString);  
						sendDataToPairedDevice(upString);
					}
				}else if( v == downleftBtn){
					if (btSocket != null){
						String upString = "550301"+box_token+"27"+computeCRC8("0301"+box_token, 39)+"EE";								
						Log.d("", "------------后右转------>" + upString); 
						sendDataToPairedDevice(upString);
					}
				} else if( v == downrightBtn){
					if (btSocket != null){
						String upString = "550301"+box_token+"26"+computeCRC8("0301"+box_token, 38)+"EE";						
						Log.d("", "------------后左转------>" + upString); 
						sendDataToPairedDevice(upString);
					}
				} 
			}else if(event.getAction() == MotionEvent.ACTION_UP){
				if (btSocket != null){
					String upString = "550301"+box_token+"AA"+computeCRC8("0301"+box_token, 170)+"EE";	
					Log.d("", "------------！！！停止当前工作！！！------>" + upString); 
					sendDataToPairedDevice(upString);
					sendDataToPairedDevice(upString);
				}
			}
			return false;
		}

	}*/
	/**
	 * 
	 * @author taotao
	 * @Date 2014-11-20
	 */
	public class SubMultiToucher extends MultiToucher{

		@Override
		public void send(int order) {
			super.send(order);
			if (box_token.equals("")) { // 未对码的情况下禁止用户操作
				Toast.makeText(MainActivity.this, "请先进行对码操作", Toast.LENGTH_LONG).show();
				return;
			}//TODO testtest
			if (btSocket == null)	return;
//			Log.i("send", getmap().get(order));
			sendDataToPairedDevice(getmap().get(order));
		}
		SparseArray<String> usMap;
		protected SparseArray<String> getmap() {
			if(null==usMap){
				
				usMap = new SparseArray<String>();
//				usMap.put(0, makeFormatCmd(box_token, "AA", 170));//notice this
				usMap.put(0x100000, String.format("%s%s%s%sEE", "550301",box_token,"21",GattUtils.computeCRC8("0301"+box_token, 33)));
				usMap.put(0x010000, String.format("%s%s%s%sEE", "550301",box_token,"20",GattUtils.computeCRC8("0301"+box_token, 32)));
				usMap.put(0x001000, String.format("%s%s%s%sEE", "550301",box_token,"22",GattUtils.computeCRC8("0301"+box_token, 34)));
				usMap.put(0x000100, String.format("%s%s%s%sEE", "550301",box_token,"26",GattUtils.computeCRC8("0301"+box_token, 38)));
				usMap.put(0x000010, String.format("%s%s%s%sEE", "550301",box_token,"25",GattUtils.computeCRC8("0301"+box_token, 37)));
				usMap.put(0x000001, String.format("%s%s%s%sEE", "550301",box_token,"27",GattUtils.computeCRC8("0301"+box_token, 39)));
				usMap.put(0x110000, String.format("%s%s%s%sEE", "550301",box_token,"23",GattUtils.computeCRC8("0301"+box_token, 35)));
				usMap.put(0x011000, String.format("%s%s%s%sEE", "550301",box_token,"24",GattUtils.computeCRC8("0301"+box_token, 36)));
				usMap.put(0x000110, String.format("%s%s%s%sEE", "550301",box_token,"28",GattUtils.computeCRC8("0301"+box_token, 40)));
				usMap.put(0x000011, String.format("%s%s%s%sEE", "550301",box_token,"29",GattUtils.computeCRC8("0301"+box_token, 41)));
				
				usMap.put(0x101000, String.format("%s%s%s%sEE", "550301",box_token,"20",GattUtils.computeCRC8("0301"+box_token, 32)));//左右
				usMap.put(0x000101, String.format("%s%s%s%sEE", "550301",box_token,"25",GattUtils.computeCRC8("0301"+box_token, 37)));
				
				usMap.put(0x100001, String.format("%s%s%s%sEE", "550301",box_token,"2A",GattUtils.computeCRC8("0301"+box_token, 42)));//向右原地
				usMap.put(0x001100, String.format("%s%s%s%sEE", "550301",box_token,"2B",GattUtils.computeCRC8("0301"+box_token, 43)));//向左原地
			}
			return usMap;
		}
	}
	
	private DialogInterface.OnClickListener songkai_listener = new DialogInterface.OnClickListener() {
		
		public void onClick(DialogInterface dialog, int which) {    						//电机松开
			if (btSocket != null){
				String upString = "550301"+box_token+"2D"+computeCRC8("0301"+box_token, 45)+"EE";								
				Log.d("", "------------电机松开------>" + upString); 
				sendDataToPairedDevice(upString);
			}
		}
	};
	
	private DialogInterface.OnClickListener jiajin_listener = new DialogInterface.OnClickListener() {
		
		public void onClick(DialogInterface dialog, int which) {    						//电机夹紧	
			if (btSocket != null){
				String upString = "550301"+box_token+"2C"+computeCRC8("0301"+box_token, 44)+"EE";								
				Log.d("", "-------------电机夹紧------>" + upString); 
				sendDataToPairedDevice(upString);
			}
		}
	};
	
	class ClickEvent implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if(v == howtoButton){
				AlertDialog.Builder ad = new Builder(MainActivity.this,AlertDialog.THEME_HOLO_DARK);
				ad.setCancelable(false);
				ad.setTitle("How to use?");
				ad.setMessage(R.string.howto);
				ad.setPositiveButton("OK", null);
				ad.create().show();	
			}else if(v == aboutButton){
				AlertDialog.Builder ad = new Builder(MainActivity.this,AlertDialog.THEME_HOLO_DARK);
				ad.setCancelable(false);
				ad.setTitle("About ENDURO?");
				ad.setMessage("软件版本：\nv1.01\n\n公司信息：\nTradekar International B.V.\n\n" +
						"Add: Staalweg 8  4104 AT  Culemborg\n\n网站链接：\nwww.enduro-europe.eu");
				ad.setPositiveButton("OK", null);
				ad.create().show();	
			}
//			else if(v == clearButton){				
//				adtDevices.clear();
//				adtDevices.notifyDataSetChanged();				
//			}
			else if (v == btnSearch)// 搜索蓝牙设备，在BroadcastReceiver显示结果
			{
//				afterConnectUIChange();
				
				if (btAdapt.getState() == BluetoothAdapter.STATE_OFF) {// 如果蓝牙还没开启
					Toast.makeText(MainActivity.this, "Please switch Bluetooth on first", 1000).show();
					return;
				}
				if (btAdapt.isDiscovering()){
					btAdapt.cancelDiscovery();
				}
				
				lstDevices.clear();
				Object[] lstDevice = btAdapt.getBondedDevices().toArray();
				for (int i = 0; i < lstDevice.length; i++) {
					BluetoothDevice device = (BluetoothDevice) lstDevice[i];
					String str = "Paired successfully|" + device.getName() + "|"
							+ device.getAddress();
					lstDevices.add(str); // 获取设备名称和mac地址
					adtDevices.notifyDataSetChanged();
				}
				if(lstDevices.size() > 0){
					lvTitle.setVisibility(View.VISIBLE);
				}
//				setTitle("本机蓝牙地址：" + btAdapt.getAddress());
				btAdapt.startDiscovery();
			} else if (v == tbtnSwitch) {// 本机蓝牙启动/关闭
				if (tbtnSwitch.isChecked() == false)
					btAdapt.enable();

				else if (tbtnSwitch.isChecked() == true)
					btAdapt.disable();
			} else if (v == btnDis)// 本机可以被搜索
			{
				Intent discoverableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(
						BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
				startActivity(discoverableIntent);
			} else if (v == btnExit) {
				try {
					if (btSocket != null){
						Log.d("", "---------socket  closed--------->");
						btSocket.close();
						Toast.makeText(MainActivity.this, "安全退出", Toast.LENGTH_LONG).show();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				finish();
			} else if( v == songkaiBtn){
				if(box_token.equals("")){ 	//未对码的情况下禁止用户操作
					Toast.makeText(MainActivity.this, "请先进行对码操作", Toast.LENGTH_LONG).show();
					return;
				}
				AlertDialog.Builder ad = new Builder(MainActivity.this,AlertDialog.THEME_HOLO_DARK);
				ad.setCancelable(false);
				ad.setTitle("Disengage the rollers?");
				ad.setPositiveButton("Yes", songkai_listener);
				ad.setNegativeButton("No", null);
				ad.create().show();	
			} else if( v == jiajinBtn){
				if(box_token.equals("")){ 	//未对码的情况下禁止用户操作
					Toast.makeText(MainActivity.this, "请先进行对码操作", Toast.LENGTH_LONG).show();
					return;
				}
				AlertDialog.Builder ad = new Builder(MainActivity.this,AlertDialog.THEME_HOLO_DARK);
				ad.setCancelable(false);
				ad.setTitle("Engage the rollers?");
				ad.setPositiveButton("Yes", jiajin_listener);
				ad.setNegativeButton("No", null);
				ad.create().show();	
			}
		}
	}
    
	private void sendDataToPairedDevice(String message){       
          try {              
        	  System.out.println(message);
        	  OutputStream mmout=btSocket.getOutputStream();
              mmout.write(getHexBytes(message));
              mmout.flush();
//              mmout.close();
//              btSocket.close();
          } catch (IOException e) {
        	  Message mMsg = new Message();
	          mMsg.what =5;
	          handler2.sendMessage(mMsg);
              Log.e("-------", "Exception during write", e);
          }//testtest TODO
      }
    
	 /**
	  * 监听返回数据
	  */
	 void beginListenForData()
	 {
//	     final Handler handler = new Handler(); 
//	     final byte delimiter = 10; //This is the ASCII code for a newline character

	     stopWorker = false;
	     readBufferPosition = 0;
	     readBuffer = new byte[1024];
	     workerThread = new Thread(new Runnable()
	     {
	         public void run()
	         {             	        	
	            while(!Thread.currentThread().isInterrupted() && !stopWorker){
	                 try 
	                 {
	                     int bytesAvailable = mmInputStream.available();  

	                     if(bytesAvailable > 0)
	                     {

	                         byte[] packetBytes = new byte[bytesAvailable];
	                         mmInputStream.read(packetBytes);
	                         result = bytesToHexString(packetBytes);

	                         Log.d("--", "-----------------result------------->"+ result);
	                         broken_str = broken_str + result;
	                         if(result.length() == 16 || broken_str.length() == 18){

	                        	 hasSignal = false;
	                        	 
	                        	 if(result.length() == 16){
	                        		 
	                        	 }else{
	                        		 result = broken_str.substring(2, 18);
	                        	 }
	                        	 
	                        	 Log.d("--", "----#--->"+result);
	                        	 
	                        	 //返回值的crc8校验             	
	                        	 if(computeCRC8(result.substring(0, 10),Integer.parseInt(result.substring(10, 12), 16))
	                        			 .equals(result.substring(12, 14))){
	                        		 
	                        		 command = result.substring(10, 12);
	                        		 Log.d("--","-------- :) ---------------------->command = " +command);
	                        		 //是不是对码操作,返回的命令位是91 
	                        		 if(command.equals("91")){
	                        			 needfengming = true;
	                        			 needBB = true;
			                        	 Log.d("--", "---------111进入对码状态----->" + result);
			                        	 token = result.substring(4, 10);  	
			                        	 editorToken.putString("TOKEN", token);
										 editorToken.commit();//提交
			     						 String duima = "550301"+imei+"BB" + computeCRC8("0301"+imei, 187) + "EE";
			     						 Log.d("--", "-------发送对码请求------->"+duima);		     						 
			                        	 sendDataToPairedDevice(duima);
			                         }

	                        		 if(command.equals("BB") && needBB){
	                        			 needBB = false;
	                        			 Log.d("--", "---------222进入对码状态----->" + result);
	                        			 box_token = result.substring(4, 10);  	
			                        	 editorBox.putString("BOX_TOKEN", box_token);
										 editorBox.commit();//提交
										 check_str = "550301"+box_token +"CC"+ computeCRC8("0301"+box_token,204) +"EE";
										 if(needfengming){
											 needfengming = false;
											 Message mMsg = new Message();
									         mMsg.what=4;
									         handler2.sendMessage(mMsg); 
										 }										 
			                        	 Log.d("--","--------------对码成功----重新轮询的字串是---->" + check_str);
			                         }   
	                        		 
//	                        		 Log.d("--","-----判断token---->"+  result.substring(4, 10) + "    " +token);
//		                        	 if(result.substring(4, 10).equals(token)){
		                        	 if(computeCRC8(result.substring(0, 10),Integer.parseInt(result.substring(10, 12), 16))
		                        			 .equals(result.substring(12, 14))){
		                        		 command = result.substring(10, 12);
			                        	 if(command.equals("90")){
			                        		 if(needSetNormalSignal){
			                        			 setNormalSignal();
			                        		 }
				                        	 Log.d("--", "--正常-->");
				                         }else if(command.equals("96")){
				                        	 Log.d("--","---------高压-------高压--------------->");
				                        	 needSetNormalSignal = true;
				                        	 gaodianya();
				                         }else if(command.equals("95")){
				                        	 Log.d("--","---------低压-------低压-------------->");
				                        	 needSetNormalSignal = true;
				                        	 didianya(); 
				                         }else if(command.equals("97")){
				                        	 Log.d("--","---------过载-------过载------------->");
				                        	 needSetNormalSignal = true;
				                        	 guozai();
				                         }else if(command.equals("98")){
				                        	 Log.d("--", "---小电机工作超时--->");
				                         }else if(command.equals("99")){
				                        	 Log.d("--", "---大电机工作超时---->");
				                         }
			                        	 
			                        	 if(command.equals("93")&&subMultiToucher.getFingerCount()==0){//大电机工作中 手指没按着
				                        	 //TODO to be tested
			                        		 sendDataToPairedDevice(String.format("%s%s%s%sEE", "550301",box_token,"AA",computeCRC8("0301"+box_token, 170)));
				                         }
		                        	 }	                        		 
	                        	 }	                        	 
	                         }else{
	                        	 broken_str = result;	                        	 
	                         }
	                     }
	                 } 
	                 catch (IOException ex) 
	                 {
	                     stopWorker = true;
	                 }
	            }
	         }
	     });

	     workerThread.start();
	 }
	 
	Handler handler2 = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
            case 0:
            	gaoya_img.setVisibility(View.VISIBLE);
            	diya_img.setVisibility(View.GONE);
            	xinhao_img.setVisibility(View.VISIBLE);
            	if(needfengming){
            		RingtoneUtils.playRingtone(MainActivity.this);
            		needfengming = false;
            		setOpBtnDisable();
            	}          	
            	break;
            case 1:
            	diya_img.setVisibility(View.VISIBLE);
            	gaoya_img.setVisibility(View.GONE);
            	xinhao_img.setVisibility(View.VISIBLE);
            	if(needfengming){
            		RingtoneUtils.playRingtone(MainActivity.this);
            		needfengming = false;
            		setOpBtnDisable();
            	}            	
            	break;
            case 2:
            	guozai_img.setVisibility(View.VISIBLE);
            	xinhao_img.setVisibility(View.VISIBLE);
            	if(needfengming){
            		RingtoneUtils.playRingtone(MainActivity.this);
            		needfengming = false;
            		setOpBtnDisable();
            	}           	
            	break;
            case 3:
            	needfengming = true;
            	needSetNormalSignal = false;
	       		xinhao_img.setVisibility(View.VISIBLE);	       		
	       		guozai_img.setVisibility(View.INVISIBLE);	       		
	       		gaoya_img.setVisibility(View.INVISIBLE);
	       		diya_img.setVisibility(View.GONE);
	       		setOpBtnEnable();
            	break; 
            case 4:
            	RingtoneUtils.playRingtone(MainActivity.this);
            	break;
            case 5:
            	xinhao_img.setVisibility(View.INVISIBLE);
            	break;
            case 6:
            	xinhao_img.setVisibility(View.VISIBLE);
            	break;           	
            }
        }
    };
	    
    private void setOpBtnDisable(){
    	upBtn.setEnabled(false);
    	downBtn.setEnabled(false);
    	upleftBtn.setEnabled(false);
    	uprightBtn.setEnabled(false);
    	downleftBtn.setEnabled(false);
    	downrightBtn.setEnabled(false);
    	songkaiBtn.setEnabled(false);
    	jiajinBtn.setEnabled(false);
    }
    
    private void setOpBtnEnable(){
    	upBtn.setEnabled(true);
    	downBtn.setEnabled(true);
    	upleftBtn.setEnabled(true);
    	uprightBtn.setEnabled(true);
    	downleftBtn.setEnabled(true);
    	downrightBtn.setEnabled(true);
    	songkaiBtn.setEnabled(true);
    	jiajinBtn.setEnabled(true);
    }
    
	 private void setNormalSignal(){
		 Message mMsg = new Message();
         mMsg.what=3;
         handler2.sendMessage(mMsg);
	 }
	 
	 private void gaodianya(){
		 Message mMsg = new Message();
         mMsg.what=0;
         handler2.sendMessage(mMsg);		 
	 }
	 
	 private void didianya(){
		 Message mMsg = new Message();
         mMsg.what=1;
         handler2.sendMessage(mMsg);		
	 }
	 
	 private void guozai(){
		 Message mMsg = new Message();
         mMsg.what=2;
         handler2.sendMessage(mMsg);
	 }
	 
	/**
	 * 转化字符串为十六进制编码  
	 * @param s
	 * @return
	 */
	 public static String toHexString(String s) {  
	    String str = "";  
	    for (int i = 0; i < s.length(); i++) {  
	     int ch = (int) s.charAt(i);  
	     String s4 = Integer.toHexString(ch);  
	     str = str + s4;  
	    }  
	    return str;  
	 }  
	 
	 /**
	  * 字节数组转化为16进制字符串
	  * @param bytes
	  * @return
	  */
	 public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        return result;
	 }
	 
	 /**
	  * 字符串转16进制
	  * @param message
	  * @return
	  */
	 private byte[] getHexBytes(String message) {
        int len = message.length() / 2;
        char[] chars = message.toCharArray();
        String[] hexStr = new String[len];
        byte[] bytes = new byte[len];
        for (int i = 0, j = 0; j < len; i += 2, j++) {
            hexStr[j] = "" + chars[i] + chars[i + 1];
            bytes[j] = (byte) Integer.parseInt(hexStr[j], 16);
        }
        return bytes;
	 }
	 
	 /**
	  * CRC8 算法---查表法
	  */
	 private static byte dscrc_table [];
	  /*    * Create the lookup table    */   
	 static   {          
		 dscrc_table = new byte [256];      
		 int acc;      
		 int crc;      
		 for (int i = 0; i < 256; i++)      
		 {         
			 acc = i;         
			 crc = 0;        
			 for (int j = 0; j < 8; j++)         
			 {            
				 if (((acc ^ crc) & 0x01) == 0x01)            
				 {               
					 crc = ((crc ^ 0x18) >> 1) | 0x80;            
					 }            
				 else               
					 crc = crc >> 1;            
					 acc = acc >> 1;         
					 }         
			 dscrc_table [i] = ( byte ) crc;      }  
	 }
	 
	   public static int compute (byte dataToCrc [])   
	   {      
		   return compute(dataToCrc, 0, dataToCrc.length,0);   
	   }
	   
	   public static int compute (byte dataToCrc [], int off, int len, int seed)   
	   {      
		   int CRC8 = seed;      
		   for (int i = 0; i < len; i++)         
			   CRC8 = dscrc_table [(CRC8 ^ dataToCrc [i + off]) & 0x0FF];      
		   return (CRC8 & 0x0FF);   
		}
	   
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			if(keyCode == KeyEvent.KEYCODE_BACK){
				AlertDialog.Builder ad = new Builder(MainActivity.this,AlertDialog.THEME_HOLO_DARK);
				ad.setTitle("Are you sure to exit?");
				ad.setPositiveButton("Yes", exit_listener);
				ad.setNegativeButton("No", null);
				ad.create().show();
				return true;
			}

			return super.onKeyDown(keyCode, event);
		}
		
		DialogInterface.OnClickListener exit_listener = new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				//退出系统	
				if (btSocket != null){
					Log.d("", "---------socket  closed--------->");
					try {
						btSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					Toast.makeText(MainActivity.this, "安全退出", Toast.LENGTH_LONG).show();
				}
				finish();
			}
		};
		private CompassHelper cmpsHelper;
		private MultiToucher subMultiToucher;
		@Override
		protected void onResume() {
			if(null!=cmpsHelper) cmpsHelper.onResume();
			super.onResume();
		}
		@Override
		protected void onPause() {
			if(null!=cmpsHelper) cmpsHelper.onPause();
			super.onPause();
		}
}