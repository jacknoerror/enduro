package com.bzbluetooth;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.bzbluetooth.helper.CompassHelper;

public class CompassActivity extends Activity {
	
	
	private CompassHelper ch;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);//»•µÙ±ÍÃ‚¿∏
		setContentView(R.layout.activity_compass);
		
		View img = this.findViewById(R.id.img_compass);
		ch = new CompassHelper(this, (ImageView)img);
		img.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
//		getWindow().setBackgroundDrawableResource(R.color.green);//work
	}
	
	@Override
	protected void onResume() {
		if(null!=ch) ch.onResume();
		super.onResume();
	}
	@Override
	protected void onPause() {
		if(null!=ch) ch.onPause();
		super.onPause();
	}
	
}
