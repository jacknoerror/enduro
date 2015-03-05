package com.bzbluetooth.android.bluetoothlegatt;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bzbluetooth.R;
import com.bzbluetooth.helper.CompassHelper;
import com.bzbluetooth.helper.GattUtils;
import com.bzbluetooth.helper.MultiToucher;
import com.bzbluetooth.helper.TokenKeeper;

/**
 * 
 * http://my.oschina.net/tingzi/blog/215008
 * @author taotao
 * @Date 2014-12-4
 */
public class ControlActivity extends Activity {
	public static final String SP_DEVICEADDRESS = "deviceaddress";
	public static final String SP_DEVICENAME = "devicename";
	private static final String SP_AUTO_BLT = "AUTO_BLT";
	private static final String SP_BOX_TOKEN = "BOX_TOKEN";
	private static final String SP_TOKEN = "TOKEN";

	final static String TAG = "ControlActivity";
	
//	private String commandStr = "";

	public static final String EXTRAS_DEVICE_ADDRESS = SP_DEVICEADDRESS;
	public static final String EXTRAS_DEVICE_NAME = SP_DEVICENAME;
	
	private boolean mConnected = false;
	private boolean NO_RECONN = true;
	
	private BluetoothLeService mBluetoothLeService;
	
	ProgressDialog connectingDialog;//0104
	private View layout_operate;
	/*btns*/
	private ImageView diya_img,gaoya_img,xinhao_img,guozai_img,djl_img,djr_img;
	private ImageButton songkaiBtn,jiajinBtn,upBtn, downBtn,upleftBtn,
		uprightBtn,downleftBtn,downrightBtn;
	ImageView hintImg;
	private ToggleButton tbtnSwitch;
		private View dianji_ll;

	
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
            mHandler.sendEmptyMessage(101);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


	private String mDeviceName;
	private String mDeviceAddress;

	private String blt_addr_str;

	private CompassHelper cmpsHelper;
	private Vibrator mVibrator;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
		setContentView(R.layout.layout_operate);
		layout_operate = findViewById(R.id.layout_operate);
//		findViewById(R.id.layout_operate).setVisibility(View.VISIBLE);//changeto:配对成功后再显示
		final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        
        token = TokenKeeper.getValue(this, SP_TOKEN);
        box_token = TokenKeeper.getValue(this, SP_BOX_TOKEN);
        blt_addr_str = TokenKeeper.getValue(this, SP_AUTO_BLT);
        imei = getDeviceId();    
        check_str = String.format("%s%s%s%sEE", "550301",box_token.isEmpty()?imei:box_token,"CC",GattUtils.computeCRC8("0301"+(box_token.isEmpty()?imei:box_token), 204));
        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE); //0201
        
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
        initBtns();
        
        needSelectEngine = !TokenKeeper.getSpInstance(ControlActivity.this).contains("showdianji");//1230
        needSelectEngine = false;//FIXME delete
        if(needSelectEngine)layout_operate.setVisibility(View.INVISIBLE);//1230 在连接之前 透明  
        else{
        	showDianjiLl(TokenKeeper.getSpInstance(ControlActivity.this).getBoolean("showdianji", true));//1222
        }
        // 连接不上 该页面消失
	}//12-04 18:54:06.510: I/ControlActivity(9916): sending:550301110270CCB3EE

	/**
	 * @return imei or 000000
	 * 0304
	 */
	public String getDeviceId() {
		TelephonyManager telephonyManager= (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = telephonyManager.getDeviceId();
        if(null!=deviceId&&deviceId.length()>=10){
        	deviceId= deviceId.substring(4, 10);
        }else{
        	deviceId= "000000";
        }
        return deviceId;
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService.disconnect();//0122
        mBluetoothLeService.close();//0122
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
	@Override
    protected void onResume() {
        super.onResume();
        if(null!=cmpsHelper) cmpsHelper.onResume();
        registerReceiver(mGattUpdateReceiver, GattUtils.makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        startScheduleIfPossible();
    }
	@Override
    protected void onPause() {
        super.onPause();
        if(null!=cmpsHelper) cmpsHelper.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        if(null!=scheduledExecutorService&&!scheduledExecutorService.isShutdown()) scheduledExecutorService.shutdown();
    }
	
	 
    private void initBtns() {
		 // Button 设置
		
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
		ClickEvent clickEvent = new ClickEvent();
		songkaiBtn.setOnClickListener(clickEvent);
		jiajinBtn = (ImageButton) this.findViewById(R.id.jiajinBtn);
		jiajinBtn.setOnClickListener(new ClickEvent());
		
		dianji_ll = this.findViewById(R.id.dianji_ll);
		
		//警号灯
		diya_img 	= (ImageView) this.findViewById(R.id.diya_img);
		gaoya_img 	= (ImageView) this.findViewById(R.id.gaoya_img);
		xinhao_img 	= (ImageView) this.findViewById(R.id.xinhao_img);
		guozai_img 	= (ImageView) this.findViewById(R.id.guozai_img);
		djl_img 	= (ImageView) this.findViewById(R.id.img_djb_e);
		djr_img 	= (ImageView) this.findViewById(R.id.img_djb_d);
		
		
		View.OnClickListener l = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.zhinanzhen_img:
				case R.id.layout_compass_big:
					//0111
					if(null!=cmpsLayout)cmpsLayout.setVisibility(cmpsLayout.getVisibility()==View.VISIBLE?View.INVISIBLE:View.VISIBLE);
					break;
//				case R.id.btn_howto:
				case R.id.img_hint:
					GattUtils.showHowto(ControlActivity.this);
					break;
				/*case R.id.btn_about://abandon 0204
					ad = new Builder(ControlActivity.this,AlertDialog.THEME_HOLO_DARK);
					ad.setCancelable(false);
					ad.setTitle("About ENDURO?");
					View view = LayoutInflater.from(ControlActivity.this).inflate(R.layout.layout_about, null);//
					ad.setView(view);
					ad.setPositiveButton("OK", null);
					ad.create().show();	
					break;	*/			
				default:
					break;
				}
				
			}
		};
		hintImg = (ImageView) this.findViewById(R.id.img_hint);
		hintImg.setOnClickListener(l);
//		this.findViewById(R.id.btn_about).setOnClickListener(l);
//		this.findViewById(R.id.btn_howto).setOnClickListener(l);
		cmpsLayout = findViewById(R.id.layout_compass_big);//0111
		cmpsLayout.setOnClickListener(l);//0119
		ImageView compassImg_b = (ImageView) this.findViewById(R.id.img_compass);
		
		ImageView compassImg = (ImageView) this.findViewById(R.id.zhinanzhen_img);
		cmpsHelper = new CompassHelper(this, compassImg,compassImg_b);//taotao 1117
		compassImg.setOnClickListener(l);
		
		
		setOpBtnEnablity(false);//对码后才能操作 1230
	}

	private void savDvcInfo() {
		TokenKeeper.putValue(this, SP_DEVICENAME, mDeviceName);
		TokenKeeper.putValue(this, SP_DEVICEADDRESS, mDeviceAddress);
		
	}


	/**
     * Handles various events fired by the Service.
     ACTION_GATT_CONNECTED: connected to a GATT server.
     ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     ACTION_DATA_AVAILABLE: received data from the device.  
			This can be a result of read or notification operations.
     */
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				/*if(noSignal>0	){
					displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA)); //重连之后发一次
					noSignal=-20;//0203
				}*/
				Toast.makeText(ControlActivity.this, R.string.paired_succ,Toast.LENGTH_SHORT).show();
				savDvcInfo();
				mHandler.sendEmptyMessage(4);
				if(null!=connectingDialog&&connectingDialog.isShowing()) connectingDialog.dismiss();//0104
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				Log.i("RECON", "DIS");
//				Toast.makeText(ControlActivity.this, "disconnected",Toast.LENGTH_SHORT).show();
				disableBlinks();
//				BLINK_CONNECT = true;//1217 0104zs(to mHandler 5&6)
//				reconnect();//0201 0203jazz
				restartactivity();
//				startScan(context); //0201 jazz
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//				Log.e(TAG, "---notice！ACTION_GATT_SERVICES_DISCOVERED:LINE_277");
				// 发现服务器
				startScheduleIfPossible();
				// displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			}
		}

	};
	
    byte[] WriteBytes = new byte[20];
    /** 固定使用的读写字符     */
    BluetoothGattCharacteristic fixedCharacteristic = null;

	private ScheduledExecutorService scheduledExecutorService;

//	private String broken_str;

	private boolean needBB;

	private String token;//

	private String imei;

	private String box_token;

	private String check_str = "";// 优先级 判断成功

	private boolean needfengming=true;
	private boolean needSelectEngine=false;//1218 1222
	private boolean needSetNormalSignal=true;

	private int noSignal;//0203 改为int
	
	boolean BLINK_CONNECT,BLINK_OVER,
		BLINK_HIGH,BLINK_LOW,BLINK_DJR,BLINK_DJL;
	int blinkTimerCount,reconnTimerCount;
	private String lastDJ;//记录上次执行的小电机，确保在收到92（小电机工作中）时能正常闪烁

	private SubMultiToucher subMultiToucher;
	private String lastCmdRcv="";//0122
	private String lastCmdSnd="";//0203
	void send(){
		if (null == (fixedCharacteristic = getFixedChar())) {return;}// still nil ?  
//		mNotifyCharacteristic = fixedCharacteristic;
        mBluetoothLeService.setCharacteristicNotification(
        		fixedCharacteristic, true);
		byte[] value = new byte[20];
        value[0] = (byte) 0x00;
        
        Log.i(TAG, String.format("sending:%s", check_str));
		WriteBytes = GattUtils.hex2byte(check_str.getBytes());// 固定使用16进制传输
		// WriteBytes = "1234abcd".getBytes();
		fixedCharacteristic.setValue(value[0],BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		fixedCharacteristic.setValue(WriteBytes);

		mBluetoothLeService.writeCharacteristic(fixedCharacteristic);
	}
	
	public void startScheduleIfPossible() {  
		if (mConnected&&(null == scheduledExecutorService
				|| scheduledExecutorService.isShutdown())) {

			scheduledExecutorService = Executors
					.newSingleThreadScheduledExecutor();
			// 当Activity显示出来后，每两秒钟切换一次图片显示
			scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					mHandler.sendEmptyMessage(noSignal>4?5:6);
					send();
					noSignal++;
//					Log.i(TAG, "哎哟~你别恶心我~");
					mHandler.sendEmptyMessage(7);
//					if(!NO_RECONN) mHandler.sendEmptyMessage(100);//1224 重连 0104jazz
					if(noSignal>20&&mConnected) {
//						mBluetoothLeService.disconnect();//0203 //0203
//						Log.i("RECON", "nosignal-disc");
					}
				}
			}, 100, 200, TimeUnit.MILLISECONDS);
		}
    }  
	

	/**
	 * 遍历蓝牙gatt服务获得ffe1读写字符特征码
	 * @return 
	 * taotao 1204
	 */
	public BluetoothGattCharacteristic getFixedChar() {
		if (null == fixedCharacteristic) {
			for (BluetoothGattService bgService : mBluetoothLeService
					.getSupportedGattServices()) {
				for (BluetoothGattCharacteristic bgCharacteristic : bgService
						.getCharacteristics()) {
					String string = bgCharacteristic.getUuid().toString();
//					Log.i(TAG, "uuid::" + string);
					if (string.contains("ffe1"))
						fixedCharacteristic = bgCharacteristic;
				}
			}
		}
		return fixedCharacteristic;
	}
	
	/**
	 * @param commandStr
	 */
	public void setCommandStr(String commandStr) {//如果改好几遍？ 
		check_str = commandStr;
	}

	private void displayData(String rcvStr) {
        if (rcvStr != null) {
        	if(rcvStr.contains("\n")) rcvStr = rcvStr.substring(rcvStr.indexOf("\n")).trim();//unnecessary
//        	if(rcvStr.contains(" ")) rcvStr = rcvStr.replace(" ", "");
        	rcvStr = rcvStr.replaceAll("[^0-9a-zA-Z]","");//0122
        	Log.i(TAG+"r", String.format("receiving:%s", rcvStr));
            
//            broken_str =  broken_str + rcvStr;//?
            if(rcvStr.length() == 18){//rcvStr.length() == 16 || broken_str.length() == 18){

           	 noSignal = 0; 
           	 
//           	 if(rcvStr.length() == 16){
//           		 
//           	 }else{
//            }
           		 rcvStr = rcvStr.substring(2, 18);
           	 
           	 Log.d("--", "----#--->"+rcvStr);
           	 
           	 //返回值的crc8校验        	
           	 if(comCheckCRC8(rcvStr)){
           		 
           		 String command = rcvStr.substring(10, 12);
           		 Log.d("--","-------- :) ---------------------->command = " +command);
           		 //是不是对码操作,返回的命令位是91 
           		 if(command.equals("91")){
           			 needfengming = true;
           			 needBB = true;
                   	 Log.d("--", "---------111进入对码状态----->" + rcvStr);
                   	TokenKeeper.putValue(this, SP_TOKEN, token = rcvStr.substring(4, 10));
						 String duima = "550301"+imei+"BB" + GattUtils.computeCRC8("0301"+imei, 187) + "EE";
						 Log.d("--", "-------发送对码请求------->"+duima);		     						 
//                   	 sendDataToPairedDevice(duima);
						 setCommandStr(duima);
                    }

           		 if(command.equals("BB") && needBB){
           			 needBB = false;
           			 Log.d("--", "---------222进入对码状态----->" + rcvStr);
						 TokenKeeper.putValue(this, SP_BOX_TOKEN, box_token = rcvStr.substring(4, 10));
						 check_str = "550301"+box_token +"CC"+ GattUtils.computeCRC8("0301"+box_token,204) +"EE";
						 if(needfengming){
//							 needSelectEngine = true;//1222 首次对码要选择
							 mHandler.sendEmptyMessage(104);//1230注释 改再链接成功时选择	0211 force
						 }			
						 setOpBtnEnablity(true);//1230 对码成功后可以操作
                   	 Log.d("--","--------------对码成功----重新轮询的字串是---->" + check_str);
                    }   
           		 
               	 if(comCheckCRC8(rcvStr)
							&& token.equals(rcvStr.substring(4, 10))) {
						command = rcvStr.substring(10, 12);
						lastCmdRcv = command;//0122 for pre-send-check
						if (command.equals("90")) {
							if (needSetNormalSignal) {
								mHandler.sendEmptyMessage(3);// setNormalSignal();
							}
							if (needfengming) {
								needfengming=false;//1222 只在第一次对码时蜂鸣
								mHandler.sendEmptyMessage(4);// taotao 1204
							}
							Log.d("--", "--正常-->");
							disableBlinks();
						} else if (command.equals("96")) {
							Log.d("--", "---------高压-------高压--------------->");
							needSetNormalSignal = true;
							mHandler.sendEmptyMessage(0);// gaodianya();
						} else if (command.equals("95")) {
							Log.d("--", "---------低压-------低压-------------->");
							needSetNormalSignal = true;
							mHandler.sendEmptyMessage(1);// didianya();
						} else if (command.equals("97")) {
							Log.d("--", "---------过载-------过载------------->");
							needSetNormalSignal = true;
							mHandler.sendEmptyMessage(2);// guozai();
						} else if (command.equals("98")) {
							BLINK_DJR = false;BLINK_DJL = false;
							Log.d("--", "---小电机工作超时--->");
						} else if (command.equals("99")) {
							Log.d("--", "---大电机工作超时---->");
						} else if (command.equals("2D") ) {// 1204 //1230 rightside
							BLINK_DJR = true;// 其他状态时应当关掉 
							BLINK_DJL = false;//0111
							djl_img.setVisibility(View.GONE);//0111
							lastDJ = "2D";
							resetCommandStr();
						}else if( command.equals("2C")){//1230 leftside
							BLINK_DJL = true;// 
							BLINK_DJR = false;// 其他状态时应当关掉 
							djr_img.setVisibility(View.GONE);
							lastDJ = "2C";
							resetCommandStr();
						} else if (command.equals("9A")) {
							Log.d("--", "---失去连接报错---->");
							disableBlinks();//1230
						} else if(command.equals("92")){
							if(lastDJ.equals("2D")) {
								BLINK_DJR = true;BLINK_DJL = false;
								djl_img.setVisibility(View.GONE);
							}
							else if(lastDJ.equals("2C")) {
								BLINK_DJL = true;BLINK_DJR = false; 
								djr_img.setVisibility(View.GONE);
							}
						} else if (command.equals("21") || command.equals("20")
								|| command.equals("22") || command.equals("26")
								|| command.equals("25") || command.equals("27")
								|| command.equals("23") || command.equals("24")
								|| command.equals("28") || command.equals("29")
								|| command.equals("2A") || command.equals("2B")){
							if(!lastCmdRcv.isEmpty()&&lastCmdRcv.equals(lastCmdSnd))//0203
								resetCommandStr();
							
						}

						if (command.equals("93")
								&& subMultiToucher.getFingerCount() == 0) {// 大电机工作中
																			// 手指没按着
							setCommandStr(String.format("%s%s%s%sEE", "550301",
									box_token, "AA", GattUtils.computeCRC8(
											"0301" + box_token, 170)));
						}
						if(command.equals("AA")){//停止成功 1217
							if(subMultiToucher.getFingerCount() == 0)//0203 没按着才重置 
								resetCommandStr();
						}
						if(command.equals("2A")||command.equals("2B")){//1217 
							resetCommandStr();
						}
					}	                        		 
           	 }	                        	 
            }else{
//           	 	broken_str = rcvStr;	                        	 
            }
        
            
        }
    }

	//校验
	private boolean comCheckCRC8(String rcvStr) {
		return GattUtils.computeCRC8(rcvStr.substring(0, 10),Integer.parseInt(rcvStr.substring(10, 12), 16))
				 .equals(rcvStr.substring(12, 14));
	}
	private void disableBlinks() {
		BLINK_HIGH = false;
		BLINK_LOW = false;
		BLINK_OVER = false;
		BLINK_CONNECT = false;
		//wifi normal
		xinhao_img.setVisibility(View.VISIBLE);
		
		BLINK_DJR = false;
		BLINK_DJL = false;
		djl_img.setVisibility(View.GONE);//0104
		djr_img.setVisibility(View.GONE);
	}

	private void showDianjiLl(boolean show){
		dianji_ll.setVisibility(show ? View.GONE: View.VISIBLE);
		layout_operate.setVisibility(View.VISIBLE);
		setResult(RESULT_OK);
	}

	void resetCommandStr(){
		setCommandStr(String.format("%s%s%s%sEE", "550301",box_token.isEmpty()?imei:box_token,"CC",GattUtils.computeCRC8("0301"+(box_token.isEmpty()?imei:box_token), 204)));
	}
	
	String makeFormatCmd(String tk,String hx,int dx){
		return String.format("550301%s%s%sEE", tk,hx,GattUtils.computeCRC8("0301"+box_token, dx));
	}

	long lastTimePressBack;
	@Override
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(System.currentTimeMillis()-lastTimePressBack>2000){
				Toast.makeText(this, R.string.exit_touch_again, Toast.LENGTH_SHORT).show();
			}else{
				finish();
			}
			lastTimePressBack = System.currentTimeMillis();
		}
		return false;
	};
	Handler mHandler = new Handler(){

		@Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
            case 0:
            	if(BLINK_HIGH) break;//0211
            	BLINK_LOW=false;//0211
            	BLINK_HIGH = true;
            	gaoya_img.setVisibility(View.VISIBLE);//0108
            	diya_img.setVisibility(View.INVISIBLE);
            	xinhao_img.setVisibility(View.VISIBLE);
//            	playBeep();
            	break;
            case 1:
            	if(BLINK_LOW) break;//0211
            	BLINK_HIGH=false;//0211
            	BLINK_LOW = true;
            	diya_img.setVisibility(View.VISIBLE);//0108
            	gaoya_img.setVisibility(View.INVISIBLE);
            	xinhao_img.setVisibility(View.VISIBLE);
//            	playBeep();            	
            	break;
            case 2:
            	if(BLINK_OVER) break;//0211
            	BLINK_OVER = true;
            	guozai_img.setVisibility(View.VISIBLE);//0108
            	xinhao_img.setVisibility(View.VISIBLE);
//            	playBeep();        	
            	break;
            case 3:
            	needfengming = true;
            	needSetNormalSignal = false;
	       		xinhao_img.setVisibility(View.VISIBLE);	       		
	       		guozai_img.setVisibility(View.INVISIBLE);	       		
	       		gaoya_img.setVisibility(View.INVISIBLE);
	       		diya_img.setVisibility(View.INVISIBLE);
	       		setOpBtnEnablity(true);
            	break; 
            case 4://connected
            	setOpBtnEnablity(true);
            	if(needSelectEngine){
            		showChooseLlDialog();
//            		playBeep();//0211jazz
            	}
            	else {
//            		showDianjiLl(TokenKeeper.getSpInstance(ControlActivity.this).getBoolean("showdianji", true));//1222  1230注视，改到开头
            	}
            	break;
            case 5:
            	xinhao_img.setVisibility(View.INVISIBLE);
            	BLINK_CONNECT = true;
//            	if(!box_token.equals(""))startScan(ControlActivity.this);//0122 0201jazz
            	break;
            case 6:
            	xinhao_img.setVisibility(View.VISIBLE);
            	BLINK_CONNECT = false;
//            	if(!box_token.equals(""))stopScanAndConnect(ControlActivity.this);//0122 0201jazz
            	break; 
			case 7://blink
				Log.i(TAG, "blinkCount"+blinkTimerCount);
				blinkTimerCount%=4;
				if(blinkTimerCount++!=0) break;//时间倍数闪烁
				if(BLINK_CONNECT) blinkBtn(xinhao_img);
				if(BLINK_HIGH) blinkBtn(gaoya_img);
				if(BLINK_LOW) blinkBtn(diya_img);
				if(BLINK_OVER) blinkBtn(guozai_img);
				if(BLINK_DJR) blinkBtn(djr_img);
				if(BLINK_DJL) blinkBtn(djl_img);
				break;
				
			case 101://连接中loading框
				connectingDialog = GattUtils.showProgressDialog(ControlActivity.this, getString(R.string.connecting));//not enough
				connectingDialog.setCancelable(false);
				mHandler.sendEmptyMessageDelayed(102, 10*1000);
				break;
			case 102://判断是否连接失败
				if(null!=connectingDialog&&connectingDialog.isShowing()){
					connectingDialog.dismiss();
//					CharSequence text = "connecting failed";
//					GattUtils.showToast(context, text);
					mBluetoothLeService.close();//0211
					/*GattUtils.showDialog(ControlActivity.this, getString(R.string.connectfailreconnect),  new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							restartactivity();
						}
					}).setOnDismissListener(new DialogInterface.OnDismissListener(){

						@Override
						public void onDismiss(DialogInterface dialog) {
							finish();
						}
						
					});//0204 //FIXME delete when testing
*/				}
				break;
			case 103://0201
				if(mConnected) return;
				mBluetoothLeService.connect(mDeviceAddress);
				mHandler.sendEmptyMessageDelayed(103, 5000);
//				Log.i("RECON", "CONN");
				break;
			case 104://0211 force to beep
				needfengming =true;
				playBeep();
				break;
			default:
				break;
            }
        }

		private void showChooseLlDialog() {
			AlertDialog.Builder ad = new Builder(ControlActivity.this,AlertDialog.THEME_HOLO_DARK);
			ad.setCancelable(false);
			ad.setTitle(R.string.select_system);
			DialogInterface.OnClickListener dListener = new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					boolean show = which == AlertDialog.BUTTON_POSITIVE;
					TokenKeeper.getSpInstance(ControlActivity.this).edit().putBoolean("showdianji", show).commit();//1222
					showDianjiLl(show );
				}
			};
			ad.setPositiveButton(R.string.system_Manual, dListener);
			ad.setNegativeButton(R.string.system_auto, dListener);
			ad.create().show();	
			needSelectEngine = false;
		}
		
		
		
		private void blinkBtn(ImageView alert_img) {
			if(alert_img.getVisibility()==View.VISIBLE){
				alert_img.setVisibility(View.INVISIBLE);
			}else{
				alert_img.setVisibility(View.VISIBLE);
			}
			
		}

		long [] vPattern = {0,700};   // 停止 开启 停止 开启   
		/**
		 * 
		 */
		public void playBeep() {
			if(needfengming){
//				RingtoneUtils.playRingtone(ControlActivity.this);
		        mVibrator.vibrate(vPattern,-1);  
				needfengming = false;
			}
			setOpBtnEnablity(false);//notice here 	0201
		}
    };
	private View cmpsLayout;

    
	private void setOpBtnEnablity(boolean enable) {
		upBtn.setEnabled(enable);
		downBtn.setEnabled(enable);
		upleftBtn.setEnabled(enable);
		uprightBtn.setEnabled(enable);
		downleftBtn.setEnabled(enable);
		downrightBtn.setEnabled(enable);
		songkaiBtn.setEnabled(enable);
		jiajinBtn.setEnabled(enable);
		
	}
	public void restartactivity() {
		startActivity(new Intent(ControlActivity.this, DeviceScanActivity.class));
		finish();
	}
	class ClickEvent implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			AlertDialog.Builder ad = new Builder(ControlActivity.this,
					AlertDialog.THEME_HOLO_DARK);
			ad.setCancelable(false);
			ad.setNegativeButton(R.string.answer_no, null);
			switch (v.getId()) {
			case R.id.songkaiBtn:
				if (box_token.equals("")) { // 未对码的情况下禁止用户操作
					Toast.makeText(ControlActivity.this, R.string.hint_pairfirst,Toast.LENGTH_LONG).show();//
					return;
				}
				ad.setTitle(R.string.roller_disengage);
				ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {    						//电机松开
//						if (btSocket != null){
						if(mConnected){
//							String upString = "550301"+box_token+"2D"+GattUtils.computeCRC8("0301"+box_token, 45)+"EE";								
							String upString =String.format("550301%s%s%sEE", box_token,"2D",GattUtils.computeCRC8("0301"+box_token, 45));
							Log.d("", "------------电机松开------>" + upString); 
//							sendDataToPairedDevice(upString);
							setCommandStr(upString);
						}
					}
				});
				ad.create().show();
				break;
			case R.id.jiajinBtn:
				if (box_token.equals("")) { // 未对码的情况下禁止用户操作
					Toast.makeText(ControlActivity.this, R.string.hint_pairfirst,Toast.LENGTH_LONG).show();
					return;
				}
				ad.setTitle(R.string.roller_engage);
				ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {    						//电机夹紧	
						if(mConnected){
							String upString =String.format("550301%s%s%sEE", box_token,"2C",GattUtils.computeCRC8("0301"+box_token, 44));
							setCommandStr(upString);
							Log.d("", "------------电机松开------>" + upString); 
						}
					}
				});
				ad.create().show();
				break;
			default:
				break;
			}
		}
	}
	/**
	 * 
	 * @author taotao
	 * @Date 2014-12-4
	 */
	public class SubMultiToucher extends MultiToucher{
		SparseArray<String> usMap;
		protected SparseArray<String> getmap() {
			if(null==usMap){//添加组合时，要在父类的mask中添加
				
				usMap = new SparseArray<String>();
				usMap.put(0, makeFormatCmd(box_token, "AA", 170));
				//非法操作时
				usMap.put(0x100100, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x001001, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x010010, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x100010, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x001010, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x010100, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x010001, makeFormatCmd(box_token, "AA", 170));
//				usMap.put(0x, makeFormatCmd(box_token, "AA", 170));
				//makeFormatCmd(box_token, "2B", 43)
				//组合键
				usMap.put(0x100000, makeFormatCmd(box_token, "21", 33));
				usMap.put(0x010000, makeFormatCmd(box_token, "20", 32));
				usMap.put(0x001000, makeFormatCmd(box_token, "22", 34));
				usMap.put(0x000100, makeFormatCmd(box_token, "26", 38));
				usMap.put(0x000010, makeFormatCmd(box_token, "25", 37));
				usMap.put(0x000001, makeFormatCmd(box_token, "27", 39));
				usMap.put(0x110000, makeFormatCmd(box_token, "23", 35));
				usMap.put(0x011000, makeFormatCmd(box_token, "24", 36));
				usMap.put(0x000110, makeFormatCmd(box_token, "28", 40));
				usMap.put(0x000011, makeFormatCmd(box_token, "29", 41));
				usMap.put(0x101000, makeFormatCmd(box_token, "20", 32));//左右
				usMap.put(0x000101, makeFormatCmd(box_token, "25", 37));
				//原地
				usMap.put(0x100001, makeFormatCmd(box_token, "2A", 42));//向右原地
				usMap.put(0x001100, makeFormatCmd(box_token, "2B", 43));//向左原地
				
			}
			return usMap;
		}
		@Override
		public void send(int order) {
			super.send(order);
			if (box_token.equals("")) { // 未对码的情况下禁止用户操作
				Toast.makeText(ControlActivity.this, "Please get paired first", Toast.LENGTH_LONG).show();
				return;
			}
			if(BLINK_DJL||BLINK_DJR){
				return;
			}
//			if (btSocket == null)	return;
			String curCmdStr = getmap().get(order);
			setCommandStr(curCmdStr);
			lastCmdSnd = curCmdStr.substring(12,14);
		}
	}
}
