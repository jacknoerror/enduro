package com.bzbluetooth.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.bzbluetooth.R;

/**
 * 
 * @author taotao
 * @Date 2014-11-18
 */
public class MultiToucher implements OnTouchListener, CutOutImpl{
	private final String TAG = getClass().getSimpleName();
	
	private static final int MAX_FINGERS = 2;
	protected static final List<Integer> availableZone = new ArrayList<Integer>();
	protected static final Queue<Integer> waitingZone = new LinkedList<Integer>();
	/**
	 * key:btnId; value:btnValue
	 */
	static final SparseArray<Integer> contrastMap = new SparseArray<Integer>();

	final int[] CP_MASKS = new int[]{0x110000,0x011000,0x000110,0x000011,
									0x101000,0x000101,
									0x100001,0x001100,
									//如果是停止，依然要mask   
									0x100100,0x001001, 0x100010,0x001010,0x010100,0x010001, 
									0x010010,
	};

//	private boolean alive;
	private SparseArray<Boolean> aliveMap = new SparseArray<Boolean>();

	

	public MultiToucher() {
		contrastMap.put(R.id.uprightBtn, 0x100000);
		contrastMap.put(R.id.upBtn, 0x010000);
		contrastMap.put(R.id.upleftBtn, 0x001000);
		contrastMap.put(R.id.downrightBtn, 0x000100);
		contrastMap.put(R.id.downBtn, 0x000010);
		contrastMap.put(R.id.downleftBtn, 0x000001);
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {

		int id = v.getId();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			born(v);
			int order = 0;
			Integer intToAdd = contrastMap.get(id);//得到btnValue
			if(addToAvailable(intToAdd)){//添加有效区成功
				order = getAvailableValue(intToAdd);
			}else{
				addToWaiting(intToAdd);
			}
			if(order>0){
				send(order);
			}
			v.setSelected(true);
//			v.setFocusable(true);
//			v.requestFocus();//useless
			break;
		case MotionEvent.ACTION_UP:
			if(isAlive(v))cut(v);
			break;
		case MotionEvent.ACTION_MOVE:
			float x = event.getX(),y = event.getY();
			if(x<0||x>v.getMeasuredWidth()||y<0||y>v.getMeasuredHeight()) {
				if(isAlive(v))cut(v);
				kill(v);
			}
			break;
		default:
			break;
		}
		return true;
	}


	/**
	 * 终止指令
	 */
	public void stop() {
		Log.i(TAG, "stop--");
		send(0);
	}


	/**
	 * 发送指令
	 * @param order
	 */
	public void send(int order) {
		if(order==0&&availableZone.size()>0) order = availableZone.get(0);//松开第三颗手指而剩下两个有冲突时
		
		logHex(order);
	}


	/**
	 * test
	 * @param order 
	 */
	void logHex(int order) {
		if(0==order) return;
		String output;
		output = Integer.toHexString(order);
		Log.i(TAG, "send:0x"+output);
	}

	/**
	 * 
		 * @param intToAdd
		 * @return 组合键value，单个按键value，0
		 */
		private int getAvailableValue(Integer intToAdd) {
			int order = intToAdd;
			if(availableZone.size()>1){
				int checkCompound = checkCompound();
				if(checkCompound>0){
					order = checkCompound;
				}else{
					removeElement(intToAdd);
					addToWaiting(intToAdd);
					order=0; //如果不能组成组合键，那这个按钮是无效的，加入等待队列中
				}
			}
			return order;
		}


	private int checkCompound() {
		if(availableZone.size()<MAX_FINGERS) return 0;
		int result = 0;
		Integer a0 = availableZone.get(0);
		Integer a1 = availableZone.get(1);
		int com = a0 | a1;
		for(int mask:CP_MASKS){
			if(com==mask) {
				result=mask;
				break;
			}
		}
//		Log.i(TAG , "check:"+result);
		return result;
	}
	
	boolean addToAvailable(Integer intToAdd) {
		if (availableZone.size() >= 2)
			return false;
		availableZone.add(intToAdd);
		return true;
	}

	boolean addToWaiting(Integer intToAdd) {
		waitingZone.add(intToAdd);
		return true;
	}

	/**
	 * 从有效区或等待区删除
	 * 
	 * @param intToRemove
	 * @return
	 */
	boolean removeElement(Integer intToRemove) {
		if (availableZone.contains(intToRemove)) {
			availableZone.remove(intToRemove);
		} else if (waitingZone.contains(intToRemove)) {
			waitingZone.remove(intToRemove);
		} else {
			return false;
		}
//		Log.i(TAG , "remove:"+intToRemove);
		return true;
	}

	


	public int getFingerCount() {//1126
		return availableZone.size()+waitingZone.size();
	}


	@Override
	public void cut(View v) {
		int id = v.getId();
		Integer intToRemove = contrastMap.get(id);
		removeElement(intToRemove);
		if(availableZone.size()<2&&waitingZone.size()>0){
			addToAvailable(waitingZone.poll());
		}
		if (availableZone.size() > 0)
			send(getAvailableValue(availableZone.get(0)));// 松开一个手指后补发按下的
		else
			stop();
		v.setSelected(false);
//		v.setFocusable(false);//useless
	}


	@Override
	public void kill(View v) {
		aliveMap.put(v.getId(), false);// = false;
	}


	@Override
	public void born(View v) {
		aliveMap.put(v.getId(), true);
	}


	@Override
	public boolean isAlive(View v) {
		boolean b = aliveMap.get(v.getId());
		aliveMap.put(v.getId(), b);
		return b;
	}

}