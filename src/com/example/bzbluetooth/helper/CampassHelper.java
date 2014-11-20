package com.example.bzbluetooth.helper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * 
 * @author taotao
 * @Date 2014-11-17
 */
public class CampassHelper {

	Context context;
	
	private SensorManager manager;
	private SensorListener listener ;
	View campassImg;

	public CampassHelper(Context context,ImageView campassImg) {
		this.context = context;
		manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		listener = new SensorListener();
		this.campassImg = campassImg;
	}
	
	public void onResume(){
		Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		// 应用在前台时候注册监听器
		manager.registerListener(listener, sensor,
				SensorManager.SENSOR_DELAY_GAME);
		
		if(null == sensor){//taotao 1117
			Toast.makeText(context, "对不起，您的手机不支持指南针功能", Toast.LENGTH_LONG).show();
		}
	}
	public void onPause(){
		manager.unregisterListener(listener);
	}
	
	private final class SensorListener implements SensorEventListener {

		private float predegree = 0;

		@Override
		public void onSensorChanged(SensorEvent event) {
			/**
			 * values[0]: x-axis 方向加速度 　　 values[1]: y-axis 方向加速度 　　 values[2]:
			 * z-axis 方向加速度
			 */
			float degree = event.values[0];// 存放了方向值
			/** 动画效果 */
			RotateAnimation animation = new RotateAnimation(predegree, degree,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			animation.setDuration(200);
			campassImg.startAnimation(animation);
			predegree = -degree;

			/**
			 * float x=event.values[SensorManager.DATA_X]; float
			 * y=event.values[SensorManager.DATA_Y]; float
			 * z=event.values[SensorManager.DATA_Z]; Log.i("XYZ",
			 * "x="+(int)x+",y="+(int)y+",z="+(int)z);
			 */
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

	}
}
