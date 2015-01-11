package com.bzbluetooth.helper;

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
public class CompassHelper {

	Context context;
	
	static private SensorManager manager;
	private SensorListener listener ;
	ImageView[] campassImgs;

	public CompassHelper(Context context,ImageView... campassImgs) {
		this.context = context;
		if(null==manager)manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		listener = new SensorListener();
		this.campassImgs = campassImgs;
	}
	
	public void onResume(){
		Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		// Ӧ����ǰ̨ʱ��ע�������
		manager.registerListener(listener, sensor,				SensorManager.SENSOR_DELAY_GAME);
		
		if(null == sensor){//taotao 1117
			Toast.makeText(context, "Sorry, compass is not available on your device!", Toast.LENGTH_LONG).show();
		}
	}
	public void onPause(){
		clearAllAnim();
		manager.unregisterListener(listener);
	}

	public void clearAllAnim() {
		if(null==campassImgs)return;
		for(ImageView v : campassImgs){
			v.clearAnimation();
		}
	}
	
	private final class SensorListener implements SensorEventListener {

		private float predegree = 0;

		@Override
		public void onSensorChanged(SensorEvent event) {
			/**
			 * values[0]: x-axis ������ٶ� ���� values[1]: y-axis ������ٶ� ���� values[2]:
			 * z-axis ������ٶ�
			 */
			float degree = event.values[0];// ����˷���ֵ
			/** ����Ч�� */
			clearAllAnim();
			for (ImageView v : campassImgs) {//
				RotateAnimation animation = new RotateAnimation(predegree,
						degree, Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f);
				animation.setDuration(200);
				// campassImg.startAnimation(animation);
				v.startAnimation(animation);
			}
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
