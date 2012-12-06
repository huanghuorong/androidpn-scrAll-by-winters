package org.huofire.common.utils;

import java.util.UUID;

import android.provider.Settings;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

public class DeviceTokenUtils {

	/**
	 * DESC 先获取ANDROID_ID，获取不到时，再获取UUID，注意ANDROID_ID在每次恢复出厂设置后，都会改变。</p>
	 * @see http://blog.csdn.net/huanghr_1/article/details/7721707
	 * @param context
	 * @return
	 */
	public static String getDeviceToken(Context context){
		String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		
		if(!StringUtils.hasText(androidId)){
			androidId = newRandomUUID();
		}else{
			androidId = "an"+androidId;//设备android_id
		}
		if(StringUtils.hasText(androidId)){
			androidId.replaceAll("-", "");
		}
		return androidId;
	}
	
    private static String newRandomUUID() {
        String uuidRaw = UUID.randomUUID().toString();
        uuidRaw = "uu" + uuidRaw; //标志UIID
        return uuidRaw.replaceAll("-", "");
    }
}
