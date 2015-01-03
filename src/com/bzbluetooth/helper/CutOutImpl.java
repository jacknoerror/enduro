package com.bzbluetooth.helper;

import android.view.MotionEvent;
import android.view.View;

public interface CutOutImpl {

	public abstract void cut(int id);

	public abstract void kill();

	public abstract void born();

	public abstract boolean isAlive();

}