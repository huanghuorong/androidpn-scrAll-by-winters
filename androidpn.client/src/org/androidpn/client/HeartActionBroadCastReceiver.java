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

import org.huofire.common.utils.AndroidpnLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent; 

/** 
 * A broadcast receiver to handle the changes in network connectiion states.
 *
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class HeartActionBroadCastReceiver extends BroadcastReceiver {

    private static final String LOGTAG = LogUtil
            .makeLogTag(HeartActionBroadCastReceiver.class);

    private PushDemoService notificationService;

    public HeartActionBroadCastReceiver(PushDemoService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReceive(Context context, Intent intent) { 
        //String action = intent.getAction(); 
        XmppManager xmppManager = notificationService.getXmppManager(); 
        if (xmppManager !=null) {
        	xmppManager.startKeepAliveTask();
        }else{
        	AndroidpnLogger.e(LOGTAG, "xmppManager is null,skip run... ");
        }
    }

}
