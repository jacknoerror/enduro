package com.bzbluetooth.android.bluetoothlegatt;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bzbluetooth.CompassActivity;
import com.bzbluetooth.CompassDialog;
import com.bzbluetooth.MainActivity;
import com.bzbluetooth.R;
import com.bzbluetooth.RingtoneUtils;
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
	
	private View layout_operate;
	/*btns*/
	private ImageView diya_img,gaoya_img,xinhao_img,guozai_img,djl_img,djr_img;
	private ImageButton songkaiBtn,jiajinBtn,upBtn, downBtn,upleftBtn,
		uprightBtn,downleftBtn,downrightBtn;
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
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            Log.e(TAG, "notice!onServiceDisconnected:LINE_96");
        }
    };


	private String mDeviceName;
	private String mDeviceAddress;

	private String blt_addr_str;

	private CompassHelper cmpsHelper;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);//ȥ��������
		setContentView(R.layout.layout_operate);
		layout_operate = findViewById(R.id.layout_operate);
//		findViewById(R.id.layout_operate).setVisibility(View.VISIBLE);//changeto:��Գɹ�������ʾ
		final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		
        token = TokenKeeper.getValue(this, SP_TOKEN);
        box_token = TokenKeeper.getValue(this, SP_BOX_TOKEN);
        blt_addr_str = TokenKeeper.getValue(this, SP_AUTO_BLT);
        TelephonyManager telephonyManager= (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        imei = telephonyManager.getDeviceId().substring(4, 10);    
        check_str = String.format("%s%s%s%sEE", "550301",box_token.isEmpty()?imei:box_token,"CC",GattUtils.computeCRC8("0301"+(box_token.isEmpty()?imei:box_token), 204));
        
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
        initBtns();
        
        needSelectEngine = !TokenKeeper.getSpInstance(ControlActivity.this).contains("showdianji");//1230
        needSelectEngine = false;//FIXME delete
        if(needSelectEngine)layout_operate.setVisibility(View.INVISIBLE);//1230 ������֮ǰ ͸�� XXX
        else{
        	showDianjiLl(TokenKeeper.getSpInstance(ControlActivity.this).getBoolean("showdianji", true));//1222
        }
	}//12-04 18:54:06.510: I/ControlActivity(9916): sending:550301110270CCB3EE

	@Override
    protected void onDestroy() {
        super.onDestroy();
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
		 // Button ����
		
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
		
		//���ŵ�
		diya_img 	= (ImageView) this.findViewById(R.id.diya_img);
		gaoya_img 	= (ImageView) this.findViewById(R.id.gaoya_img);
		xinhao_img 	= (ImageView) this.findViewById(R.id.xinhao_img);
		guozai_img 	= (ImageView) this.findViewById(R.id.guozai_img);
		djl_img 	= (ImageView) this.findViewById(R.id.img_djb_e);
		djr_img 	= (ImageView) this.findViewById(R.id.img_djb_d);
		
		
		View.OnClickListener l = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AlertDialog.Builder ad;
				switch (v.getId()) {
				case R.id.zhinanzhen_img:
//					startActivity(new Intent(ControlActivity.this,CompassActivity.class));
//					overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
					new CompassDialog(ControlActivity.this).show();
					break;
				case R.id.btn_howto:
					ad = new Builder(ControlActivity.this,AlertDialog.THEME_HOLO_DARK);
					ad.setCancelable(false);
					ad.setTitle("How to use?");
					ad.setMessage(R.string.howto);
					ad.setPositiveButton("OK", null);
					ad.create().show();	
					break;
				case R.id.btn_about:
					ad = new Builder(ControlActivity.this,AlertDialog.THEME_HOLO_DARK);
					ad.setCancelable(false);
					ad.setTitle("About ENDURO?");
//					ad.setMessage("����汾��\nv1.01\n\n��˾��Ϣ��\nTradekar International B.V.\n\n" +
//							"Add: Staalweg 8  4104 AT  Culemborg\n\n��վ���ӣ�\nwww.enduro-europe.eu");
					View view = LayoutInflater.from(ControlActivity.this).inflate(R.layout.layout_about, null);//
					ad.setView(view);
					ad.setPositiveButton("OK", null);
					ad.create().show();	
					break;				
				default:
					break;
				}
				
			}
		};
		this.findViewById(R.id.btn_about).setOnClickListener(l);
		this.findViewById(R.id.btn_howto).setOnClickListener(l);
		ImageView compassImg = (ImageView) this.findViewById(R.id.zhinanzhen_img);
		cmpsHelper = new CompassHelper(this, compassImg);//taotao 1117
		compassImg.setOnClickListener(l);
		
		setOpBtnEnablity(false);//�������ܲ��� 1230
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
				Toast.makeText(ControlActivity.this, "connected",Toast.LENGTH_SHORT).show();
				savDvcInfo();
				mHandler.sendEmptyMessage(4);
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				Toast.makeText(ControlActivity.this, "disconnected",Toast.LENGTH_SHORT).show();
				disableBlinks();
				BLINK_CONNECT = true;//1217
				NO_RECONN = false;//1224
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				Log.e(TAG, "notice��ACTION_GATT_SERVICES_DISCOVERED:LINE_277");
				// ���ַ�����
				startScheduleIfPossible();
				// displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			}
		}
	};
    byte[] WriteBytes = new byte[20];
    /** �̶�ʹ�õĶ�д�ַ�     */
    BluetoothGattCharacteristic fixedCharacteristic = null;

	private ScheduledExecutorService scheduledExecutorService;

//	private String broken_str;

	private boolean needBB;

	private String token;//

	private String imei;

	private String box_token;

	private String check_str = "";// ���ȼ� �жϳɹ�

	private boolean needfengming=true;
	private boolean needSelectEngine=false;//1218 1222
	private boolean needSetNormalSignal;

	private boolean noSignal;//
	
	boolean BLINK_CONNECT,BLINK_OVER,
		BLINK_HIGH,BLINK_LOW,BLINK_DJR,BLINK_DJL;
	int blinkTimerCount,reconnTimerCount;
	private String lastDJ;//��¼�ϴ�ִ�е�С�����ȷ�����յ�92��С��������У�ʱ��������˸

	private SubMultiToucher subMultiToucher;
	void send(){
		if (null == (fixedCharacteristic = getFixedChar())) {return;}// still nil ?  
//		mNotifyCharacteristic = fixedCharacteristic;
        mBluetoothLeService.setCharacteristicNotification(
        		fixedCharacteristic, true);
		byte[] value = new byte[20];
        value[0] = (byte) 0x00;
        
        Log.i(TAG, String.format("sending:%s", check_str));
		WriteBytes = GattUtils.hex2byte(check_str.getBytes());// �̶�ʹ��16���ƴ���
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
			// ��Activity��ʾ������ÿ�������л�һ��ͼƬ��ʾ
			scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					mHandler.sendEmptyMessage(noSignal?5:6);
					send();
					noSignal = true;
//					Log.i(TAG, "��Ӵ~��������~");
					mHandler.sendEmptyMessage(7);
					if(!NO_RECONN) mHandler.sendEmptyMessage(100);//1224 ����
				}
			}, 100, 300, TimeUnit.MILLISECONDS);
		}
    }  
	

	/**
	 * ��������gatt������ffe1��д�ַ�������
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
	public void setCommandStr(String commandStr) {//����ĺü��飿XXX
		check_str = commandStr;
	}

	private void displayData(String rcvStr) {
        if (rcvStr != null) {
        	if(rcvStr.contains("\n")) rcvStr = rcvStr.substring(rcvStr.indexOf("\n")).trim();
        	if(rcvStr.contains(" ")) rcvStr = rcvStr.replace(" ", "");
        	
//            Toast.makeText(this, rcvStr, Toast.LENGTH_SHORT).show();
            Log.i(TAG, String.format("receiving:%s", rcvStr));
            
//            broken_str =  broken_str + rcvStr;//?
            if(rcvStr.length() == 18){//rcvStr.length() == 16 || broken_str.length() == 18){

           	 noSignal = false;
           	 
//           	 if(rcvStr.length() == 16){
//           		 
//           	 }else{
//            }
           		 rcvStr = rcvStr.substring(2, 18);
           	 
           	 Log.d("--", "----#--->"+rcvStr);
           	 
           	 //����ֵ��crc8У��        	
           	 if(comCheckCRC8(rcvStr)){
           		 
           		 String command = rcvStr.substring(10, 12);
           		 Log.d("--","-------- :) ---------------------->command = " +command);
           		 //�ǲ��Ƕ������,���ص�����λ��91 
           		 if(command.equals("91")){
           			 needfengming = true;
           			 needBB = true;
                   	 Log.d("--", "---------111�������״̬----->" + rcvStr);
                   	TokenKeeper.putValue(this, SP_TOKEN, token = rcvStr.substring(4, 10));
						 String duima = "550301"+imei+"BB" + GattUtils.computeCRC8("0301"+imei, 187) + "EE";
						 Log.d("--", "-------���Ͷ�������------->"+duima);		     						 
//                   	 sendDataToPairedDevice(duima);
						 setCommandStr(duima);
                    }

           		 if(command.equals("BB") && needBB){
           			 needBB = false;
           			 Log.d("--", "---------222�������״̬----->" + rcvStr);
						 TokenKeeper.putValue(this, SP_BOX_TOKEN, box_token = rcvStr.substring(4, 10));
						 check_str = "550301"+box_token +"CC"+ GattUtils.computeCRC8("0301"+box_token,204) +"EE";
						 if(needfengming){
//							 needSelectEngine = true;//1222 �״ζ���Ҫѡ��
//							 mHandler.sendEmptyMessage(4);//1230ע�� �������ӳɹ�ʱѡ��
						 }			
						 setOpBtnEnablity(true);//1230 ����ɹ�����Բ���
                   	 Log.d("--","--------------����ɹ�----������ѯ���ִ���---->" + check_str);
                    }   
           		 
               	 if(comCheckCRC8(rcvStr)
							&& token.equals(rcvStr.substring(4, 10))) {
						command = rcvStr.substring(10, 12);
						if (command.equals("90")) {
							if (needSetNormalSignal) {
								mHandler.sendEmptyMessage(3);// setNormalSignal();
							}
							if (needfengming) {
								needfengming=false;//1222 ֻ�ڵ�һ�ζ���ʱ����
								mHandler.sendEmptyMessage(4);// taotao 1204
							}
							Log.d("--", "--����-->");
							disableBlinks();
						} else if (command.equals("96")) {
							Log.d("--", "---------��ѹ-------��ѹ--------------->");
							needSetNormalSignal = true;
							mHandler.sendEmptyMessage(0);// gaodianya();
						} else if (command.equals("95")) {
							Log.d("--", "---------��ѹ-------��ѹ-------------->");
							needSetNormalSignal = true;
							mHandler.sendEmptyMessage(1);// didianya();
						} else if (command.equals("97")) {
							Log.d("--", "---------����-------����------------->");
							needSetNormalSignal = true;
							mHandler.sendEmptyMessage(2);// guozai();
						} else if (command.equals("98")) {
							BLINK_DJR = false;BLINK_DJL = false;
							Log.d("--", "---С���������ʱ--->");
						} else if (command.equals("99")) {
							Log.d("--", "---����������ʱ---->");
						} else if (command.equals("2D") ) {// 1204 //1230 rightside
							BLINK_DJR = true;// ����״̬ʱӦ���ص� 
							lastDJ = "2D";
							resetCommandStr();
						}else if( command.equals("2C")){//1230 leftside
							BLINK_DJL = true;// 
							lastDJ = "2C";
							resetCommandStr();
						} else if (command.equals("9A")) {
							Log.d("--", "---ʧȥ���ӱ���---->");
							disableBlinks();//1230
						} else if(command.equals("92")){
							if(lastDJ.equals("2D")) {
								BLINK_DJR = true;BLINK_DJL = false;
								djl_img.setVisibility(View.INVISIBLE);
							}
							else if(lastDJ.equals("2C")) {
								BLINK_DJL = true;BLINK_DJR = false; 
								djr_img.setVisibility(View.INVISIBLE);
							}
						}

						if (command.equals("93")
								&& subMultiToucher.getFingerCount() == 0) {// ����������
																			// ��ָû����
						// sendDataToPairedDevice(String.format("%s%s%s%sEE",",box_token,"AA",GattUtils.computeCRC8("0301"+box_token,)));
							setCommandStr(String.format("%s%s%s%sEE", "550301",
									box_token, "AA", GattUtils.computeCRC8(
											"0301" + box_token, 170)));
						}
						if(command.equals("AA")){//ֹͣ�ɹ� 1217
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

	//У��
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
				Toast.makeText(this, "�ٰ�һ�η��ؼ��˳�����", Toast.LENGTH_SHORT).show();
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
//            	gaoya_img.setVisibility(View.VISIBLE);
//            	diya_img.setVisibility(View.GONE);
            	BLINK_HIGH = true;
            	xinhao_img.setVisibility(View.VISIBLE);
            	playBeep();
            	break;
            case 1:
            	BLINK_LOW = true;
//            	diya_img.setVisibility(View.VISIBLE);
//            	gaoya_img.setVisibility(View.GONE);
            	xinhao_img.setVisibility(View.VISIBLE);
            	playBeep();            	
            	break;
            case 2:
            	BLINK_OVER = true;
//            	guozai_img.setVisibility(View.VISIBLE);
            	xinhao_img.setVisibility(View.VISIBLE);
            	playBeep();        	
            	break;
            case 3:
            	needfengming = true;
            	needSetNormalSignal = false;
	       		xinhao_img.setVisibility(View.VISIBLE);	       		
	       		guozai_img.setVisibility(View.INVISIBLE);	       		
	       		gaoya_img.setVisibility(View.INVISIBLE);
	       		diya_img.setVisibility(View.GONE);
	       		setOpBtnEnablity(true);
            	break; 
            case 4:
            	playBeep();
            	setOpBtnEnablity(true);
            	if(needSelectEngine)showChooseLlDialog();
            	else {
//            		showDianjiLl(TokenKeeper.getSpInstance(ControlActivity.this).getBoolean("showdianji", true));//1222  1230ע�ӣ��ĵ���ͷ
            	}
            	break;
            case 5:
            	xinhao_img.setVisibility(View.INVISIBLE);
            	break;
            case 6:
            	xinhao_img.setVisibility(View.VISIBLE);
            	break; 
			case 7://blink
//				Log.i(TAG, "blinkCount"+blinkTimerCount);
				blinkTimerCount%=2;
				if(blinkTimerCount++!=0) break;//ʱ�䱶����˸
				if(BLINK_CONNECT) blinkBtn(xinhao_img);
				if(BLINK_HIGH) blinkBtn(gaoya_img);
				if(BLINK_LOW) blinkBtn(diya_img);
				if(BLINK_OVER) blinkBtn(guozai_img);
				if(BLINK_DJR) blinkBtn(djr_img);
				if(BLINK_DJL) blinkBtn(djl_img);
				break;
				
			case 100://����
				reconnTimerCount%=2;
				if(null!=mBluetoothLeService&&!mConnected&&!NO_RECONN&&reconnTimerCount++==0){
					NO_RECONN = mBluetoothLeService.connect(mDeviceAddress);
				}
				break;
			default:
				break;
            }
        }

		private void showChooseLlDialog() {
			AlertDialog.Builder ad = new Builder(ControlActivity.this,AlertDialog.THEME_HOLO_DARK);
			ad.setCancelable(false);
			ad.setTitle("Please select the type of your mover");
			DialogInterface.OnClickListener dListener = new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					boolean show = which == AlertDialog.BUTTON_POSITIVE;
					TokenKeeper.getSpInstance(ControlActivity.this).edit().putBoolean("showdianji", show).commit();//1222
					showDianjiLl(show );
				}
			};
			ad.setPositiveButton("Automatic Engaging Mover", dListener);
			ad.setNegativeButton("Manual Engaging Mover", dListener);
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

		/**
		 * 
		 */
		public void playBeep() {
			if(needfengming){
				RingtoneUtils.playRingtone(ControlActivity.this);
				needfengming = false;
				setOpBtnEnablity(false);//notice here 
			}
		}
    };

    
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
	class ClickEvent implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			AlertDialog.Builder ad = new Builder(ControlActivity.this,
					AlertDialog.THEME_HOLO_DARK);
			ad.setCancelable(false);
			ad.setNegativeButton("No", null);
			switch (v.getId()) {
			case R.id.songkaiBtn:
				if (box_token.equals("")) { // δ���������½�ֹ�û�����
					Toast.makeText(ControlActivity.this, "���Ƚ��ж������",
							Toast.LENGTH_LONG).show();
					return;
				}
				ad.setTitle(R.string.roller_disengage);
				ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {    						//����ɿ�
//						if (btSocket != null){
						if(mConnected){
//							String upString = "550301"+box_token+"2D"+GattUtils.computeCRC8("0301"+box_token, 45)+"EE";								
							String upString =String.format("550301%s%s%sEE", box_token,"2D",GattUtils.computeCRC8("0301"+box_token, 45));
							Log.d("", "------------����ɿ�------>" + upString); 
//							sendDataToPairedDevice(upString);
							setCommandStr(upString);
						}
					}
				});
				ad.create().show();
				break;
			case R.id.jiajinBtn:
				if (box_token.equals("")) { // δ���������½�ֹ�û�����
					Toast.makeText(ControlActivity.this, "���Ƚ��ж������",
							Toast.LENGTH_LONG).show();
					return;
				}
				ad.setTitle(R.string.roller_engage);
				ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {    						//����н�	
						if(mConnected){
							String upString =String.format("550301%s%s%sEE", box_token,"2C",GattUtils.computeCRC8("0301"+box_token, 44));
							setCommandStr(upString);
							Log.d("", "------------����ɿ�------>" + upString); 
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
			if(null==usMap){
				
				usMap = new SparseArray<String>();
				usMap.put(0, makeFormatCmd(box_token, "AA", 170));
				//�Ƿ�����ʱ
				usMap.put(0x100100, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x001001, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x010010, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x100010, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x001010, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x010100, makeFormatCmd(box_token, "AA", 170));
				usMap.put(0x010001, makeFormatCmd(box_token, "AA", 170));
//				usMap.put(0x, makeFormatCmd(box_token, "AA", 170));
				//makeFormatCmd(box_token, "2B", 43)
				//��ϼ�
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
				usMap.put(0x101000, makeFormatCmd(box_token, "20", 32));//����
				usMap.put(0x000101, makeFormatCmd(box_token, "25", 37));
				//ԭ��
				usMap.put(0x100001, makeFormatCmd(box_token, "2A", 42));//����ԭ��
				usMap.put(0x001100, makeFormatCmd(box_token, "2B", 43));//����ԭ��
			}
			return usMap;
		}
		@Override
		public void send(int order) {
			super.send(order);
			if (box_token.equals("")) { // δ���������½�ֹ�û�����
				Toast.makeText(ControlActivity.this, "Please get paired first", Toast.LENGTH_LONG).show();
				return;
			} 
//			if (btSocket == null)	return;
//			sendDataToPairedDevice(getmap().get(order));
			setCommandStr(getmap().get(order));
		}
	}
}
