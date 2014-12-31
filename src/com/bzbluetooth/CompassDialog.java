package com.bzbluetooth;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.bzbluetooth.helper.CompassHelper;

public class CompassDialog extends Dialog implements OnShowListener, OnDismissListener {

	private CompassHelper ch;

	public CompassDialog(Context context) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_compass);
		
		getWindow().setGravity(Gravity.BOTTOM);
		
		View img = this.findViewById(R.id.img_compass);
		ch = new CompassHelper(context, (ImageView)img);
		
		img.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		setOnShowListener(this);
		setOnDismissListener(this);
	}

	@Override
	public void onDismiss(DialogInterface arg0) {
		if(null!=ch) ch.onPause();
	}

	@Override
	public void onShow(DialogInterface arg0) {
		if(null!=ch) ch.onResume();
		
	}
	
	
}
