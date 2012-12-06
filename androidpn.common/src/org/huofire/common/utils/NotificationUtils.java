package org.huofire.common.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;


public class NotificationUtils {
	public static int notification_id=0011223344;
	public static int notification4push_id = 1122334455;//保证只使用一个通知id，避免通知栏被撑满用户体验差。
	public static int cacheNotiCount = 0;
	
	/**
	 * 发送通知
	 * 
	 * @param context
	 *            环境对象
	 * @param notificationId
	 *            通知标识
	 * @param notification
	 *            通知对象
	 * @param notificationIntent
	 *            通知意图
	 * @param notificationShowTitle
	 *            通知显示标题
	 * @param notificationShowText
	 *            通知显示文本
	 */
	public static void sendActivityNotification(Context context, int notificationId, Notification notification,
			Intent notificationIntent, String notificationShowTitle, String notificationShowText) {
		if (notification == null)
			return;
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent pendIntent = PendingIntent.getActivity(context, notificationId, notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		// 设置通知显示的参数
		notification.setLatestEventInfo(context, notificationShowTitle, notificationShowText, pendIntent);
		// 执行通知
		notificationManager.notify(notificationId, notification);

	}

	/**
	 * 取消通知
	 * 
	 * @param context
	 *            环境对象
	 * @param notificationId
	 *            通知标识
	 */
	public static void cancelNotification(Context context, int notificationId) {
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.cancel(notificationId);

	}

    /**
     * 这是设置通知是否同时播放声音或振动，声音为Notification.DEFAULT_SOUND
     * @param icon
     * @param tickertext "图标边的文字"
     */
    public static void showNotification(final NotificationManager nm ,Context cx, Intent intent, int icon ,String tickertext){
    	String title = ""; //"标题"
    	String content = ""; //"内容"
        //设置一个唯一的ID，随便设置
  
        //Notification管理器
        Notification notification=new Notification(icon,tickertext,System.currentTimeMillis());
//        notification.defaults=Notification.DEFAULT_VIBRATE; 
        PendingIntent pt=PendingIntent.getActivity(cx, 0, intent, 0);
        //点击通知后的动作，这里是转回main 这个Acticity
        notification.setLatestEventInfo(cx,title,content,pt);
        nm.notify(notification_id, notification);
        new Thread(new Runnable() {
			@Override
			public void run() {
				try {
				  Thread.sleep(2000);
					nm.cancel(notification_id);
				} catch (Exception e) {
				}
			}
		}).start();
    }
}
