package com.bzbluetooth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.bzbluetooth.android.bluetoothlegatt.DeviceScanActivity;

/**
 * @author tao
 * didn't make it start from DsActivity. Try change it if necessary
 */
public class StartActivity extends Activity implements Runnable {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);//»•µÙ±ÍÃ‚¿∏
		
		setContentView(R.layout.activity_start);
		
		new Handler().postDelayed(this, 2000);
	}

	@Override
	public void run() {
		startActivity(new Intent(this, DeviceScanActivity.class));
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		finish();
	}
	
}
