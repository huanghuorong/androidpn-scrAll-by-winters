/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.androidpn.client;

import java.util.Random;

import org.androidpn.huofire.R;
import org.huofire.common.utils.AndroidpnLogger;
import org.huofire.common.utils.NotificationUtils;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager; 

/** 
 * This class is to notify the user of messages with NotificationManager.
 *
 * @author winters (huanghr.1@gmail.com)
 */
public class HuofireNotifier {

    private static final String LOGTAG = LogUtil.makeLogTag(HuofireNotifier.class);
    private static final Random random = new Random(System.currentTimeMillis());
    private Context context;
    private SharedPreferences sharedPrefs;
    private AudioManager audioManager = null;
    
	// 通知栏跳转Intent
	private Intent jumpIntent = null;
	// 通知栏
	private Notification levelNotification = null;
    
    public HuofireNotifier(Context context) {
        this.context = context;
        this.sharedPrefs = context.getSharedPreferences(
                Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    //先弹出，在通知栏显示，以便获得消息数；
    public void notify(String notificationId, String apiKey, String title,String message, String uri) {
    	push2NotificationBar(title, message, 1, notificationId, apiKey, uri);
//    	push2NotificationDialog(title, message, 1, notificationId, apiKey, uri);
    }
    
    private int getSysRingerMode(){
    	audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    	int ringerMode = audioManager.getRingerMode();
//		if(AudioManager.RINGER_MODE_NORMAL == ringerMode){
//			content = "RINGER_MODE_NORMAL";
//		}else if(AudioManager.RINGER_MODE_SILENT == ringerMode){
//			content = "RINGER_MODE_SILENT";
//		}else if(AudioManager.RINGER_MODE_VIBRATE == ringerMode){
//			content = "RINGER_MODE_VIBRATE";
//		}else{
//			content = "UNKNOWN!";
//		}
    	return ringerMode;
    }
    
    private int getNotificationIcon() {
        return R.drawable.notif_push_icon;//sharedPrefs.getInt(Constants.NOTIFICATION_ICON, 0);
    }

    private boolean isNotificationEnabled() {
        return sharedPrefs.getBoolean(Constants.SETTINGS_NOTIFICATION_ENABLED,
                true);
    }

    private boolean isNotificationSoundEnabled() {
        return getSysRingerMode()==AudioManager.RINGER_MODE_NORMAL?true:false;
    }

    private boolean isNotificationVibrateEnabled() {
    	return getSysRingerMode()==AudioManager.RINGER_MODE_VIBRATE?true:false;
    }

    private boolean isNotificationToastEnabled() {
        return sharedPrefs.getBoolean(Constants.SETTINGS_TOAST_ENABLED, false);
    }
    
    public String getCurrentPackageName(){
        //FIXME
    	return "";
    }
    
	//推送消息
	private void push2NotificationBar(String title, String message, int num,String notificationId, String apiKey, String uri){
		String packageName = getCurrentPackageName();
		
		int msgCount = NotificationUtils.cacheNotiCount +1;
		levelNotification = new Notification(getNotificationIcon(), message, System.currentTimeMillis());
		levelNotification.defaults = Notification.DEFAULT_LIGHTS;
        if (isNotificationSoundEnabled()) {
        	//levelNotification.defaults |= Notification.DEFAULT_SOUND; //
        }
        if (isNotificationVibrateEnabled()) {
        	levelNotification.defaults |= Notification.DEFAULT_VIBRATE;
        }
		levelNotification.flags = Notification.FLAG_AUTO_CANCEL;
		levelNotification.number =msgCount;
		title = "云推送（共"+levelNotification.number+"条信息）";
		jumpIntent = new Intent(context, NotificationDetailsActivity.class);
		jumpIntent.putExtra(Constants.NOTIFICATION_ID, notificationId);
		jumpIntent.putExtra(Constants.NOTIFICATION_API_KEY, apiKey);
		jumpIntent.putExtra(Constants.NOTIFICATION_TITLE, title);//暂时不用服务器传来的title
		jumpIntent.putExtra(Constants.NOTIFICATION_MESSAGE, message);
		jumpIntent.putExtra(Constants.NOTIFICATION_URI, uri);
//		jumpIntent.setComponent(new ComponentName(packageName, packageName+".activities.ACT_Frame_other"));
		
		int notification4push_id = NotificationUtils.notification4push_id;
		NotificationUtils.cancelNotification(this.context, notification4push_id);
		NotificationUtils.sendActivityNotification(this.context, NotificationUtils.notification4push_id,  levelNotification, jumpIntent, title, message);
	}
	
	//桌面弹出对话框，仿QQ通讯录短信
    private void push2NotificationDialog(String title, String message, int num,String notificationId, String apiKey, String uri){
    	String packageName = getCurrentPackageName();
    	
    	jumpIntent = new Intent();
		jumpIntent.putExtra(Constants.NOTIFICATION_ID, notificationId);
		jumpIntent.putExtra(Constants.NOTIFICATION_API_KEY, apiKey);
		jumpIntent.putExtra(Constants.NOTIFICATION_TITLE, title);
		jumpIntent.putExtra(Constants.NOTIFICATION_MESSAGE, message);
		jumpIntent.putExtra(Constants.NOTIFICATION_URI, uri);
		jumpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  //上下文开启Activity必加这个Flags
		jumpIntent.setComponent(new ComponentName(packageName, packageName+".activities.ACT_PushOnDesktopDialog"));
		context.startActivity(jumpIntent);
		
		//若果已经打开Activity则广播收到的内容
        Intent intentBC = new Intent();
        intentBC.putExtra(Constants.NOTIFICATION_MESSAGE, message);
        intentBC.setAction("android.intent.action.HuofirePushMsg");
		context.sendBroadcast(intentBC);
    }
}
