package com.bzbluetooth.helper;

import android.view.MotionEvent;
import android.view.View;

public interface CutOutImpl {

	public abstract void cut(View v);

	public abstract void kill(View v);

	public abstract void born(View v);

	public abstract boolean isAlive(View v);

}