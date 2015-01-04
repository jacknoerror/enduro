package com.bzbluetooth.helper;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.Toast;

import com.bzbluetooth.android.bluetoothlegatt.BluetoothLeService;

public class GattUtils {

	public static IntentFilter makeGattUpdateIntentFilter() {
	    final IntentFilter intentFilter = new IntentFilter();
	    intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
	    intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
	    intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
	    intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
	    return intentFilter;
	}
	
	static BluetoothManager bluetoothManager;
	/**
	 * @param context
	 * @return
	 */
	public static BluetoothManager getBluetoothAdapter(Context context){
		if(null==bluetoothManager){
			bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		}
		return bluetoothManager;
	}
/**
 * hexUtils start
 * */
	public static String bin2hex(String bin) {
	    char[] digital = "0123456789ABCDEF".toCharArray();
	    StringBuffer sb = new StringBuffer("");
	    byte[] bs = bin.getBytes();
	    int bit;
	    for (int i = 0; i < bs.length; i++) {
	        bit = (bs[i] & 0x0f0) >> 4;
	        sb.append(digital[bit]);
	        bit = bs[i] & 0x0f;
	        sb.append(digital[bit]);
	    }
	    return sb.toString();
	}

	public static byte[] hex2byte(byte[] b) {
	    if ((b.length % 2) != 0) {
	        throw new IllegalArgumentException("长度不是偶数");
	    }
	    byte[] b2 = new byte[b.length / 2];
	    for (int n = 0; n < b.length; n += 2) {
	        String item = new String(b, n, 2);
	        // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个进制字节
	        b2[n / 2] = (byte) Integer.parseInt(item, 16);
	    }
	    b = null;
	    return b2;
	}

	/**
	 * 动态计算CRC8
	 */
	public static String computeCRC8(String str, int flag) {
	
		byte[] buffer = { (byte) Integer.parseInt(str.substring(0, 2), 16),
				(byte) Integer.parseInt(str.substring(2, 4), 16),
				(byte) Integer.parseInt(str.substring(4, 6), 16),
				(byte) Integer.parseInt(str.substring(6, 8), 16),
				(byte) Integer.parseInt(str.substring(8, 10), 16), (byte) flag };
		int tmp = GattUtils.compute(buffer);
		String c = Integer.toHexString(tmp);
		if (c.length() == 1) {
			c = "0" + c;
		}
		// Log.d("--", "--------------------crc8 结果---------->" + c);
		return c.toUpperCase();
	}

	public static int compute(byte dataToCrc[]) {
		return compute(dataToCrc, 0, dataToCrc.length, 0);
	}

	public static int compute(byte dataToCrc[], int off, int len, int seed) {
		int CRC8 = seed;
		for (int i = 0; i < len; i++)
			CRC8 = GattUtils.dscrc_table[(CRC8 ^ dataToCrc[i + off]) & 0x0FF];
		return (CRC8 & 0x0FF);
	}

	public static byte dscrc_table[];
	/* * Create the lookup table */
	static {
		GattUtils.dscrc_table = new byte[256];
		int acc;
		int crc;
		for (int i = 0; i < 256; i++) {
			acc = i;
			crc = 0;
			for (int j = 0; j < 8; j++) {
				if (((acc ^ crc) & 0x01) == 0x01) {
					crc = ((crc ^ 0x18) >> 1) | 0x80;
				} else
					crc = crc >> 1;
				acc = acc >> 1;
			}
			GattUtils.dscrc_table[i] = (byte) crc;
		}
	}
	/**
	 * hexUtils end
	 * */
	
	/**
	 * uiUtils start
	 * */
	public static ProgressDialog showProgressDialog(Context context,String text){
		return ProgressDialog.show(context, "",text);
	}
	public static void showToast(Context context, CharSequence text) {
		Toast.makeText(context, text , Toast.LENGTH_LONG);
	}
}
