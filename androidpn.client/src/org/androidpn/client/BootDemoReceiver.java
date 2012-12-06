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

import org.huofire.common.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 
 * Broadcast receiver that handles push notification messages from the server.
 * This should be registered as receiver in AndroidManifest.xml. 
 * 
 * @author winters_huang
 */
public final class BootDemoReceiver extends BroadcastReceiver {

    private static final String LOGTAG = LogUtil.makeLogTag(BootDemoReceiver.class); 
    private ServiceManager serviceManager;

    public BootDemoReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOGTAG, "BootReceiverr.onReceive()...");
        String action = intent.getAction();
        Log.d(LOGTAG, "action=" + action);
  
        // 启动推送服务
		serviceManager = new ServiceManager(context);
        serviceManager.startService();
    }
}
