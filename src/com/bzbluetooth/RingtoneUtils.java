package com.bzbluetooth;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

public class RingtoneUtils {

    public static void playRingtone(Context context){
//    	Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        if(alert == null){
//            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//        }
//    	Ringtone r = RingtoneManager.getRingtone(context, alert);
//    	r.play();
    	
    	
		 Uri path = Uri.parse("android.resource://"+context.getPackageName()+"/raw/waterdrop4");
//		 RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE,path);
		 RingtoneManager.getRingtone(context, path).play();
    }
}
