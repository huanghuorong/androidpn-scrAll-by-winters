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
package org.androidpn.demoapp;

import org.androidpn.client.ServiceManager;
import org.androidpn.huofire.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog; 
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

/**
 * This is an androidpn client demo application.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class DemoAppActivity extends Activity {
	ServiceManager serviceManager = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("DemoAppActivity", "onCreate()...");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Settings
        Button okButton = (Button) findViewById(R.id.btn_settings);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ServiceManager.viewNotificationSettings(DemoAppActivity.this);
            }
        });

        // Start the service
        serviceManager = new ServiceManager(this);
        serviceManager.setNotificationIcon(R.drawable.notif_push_icon);
        serviceManager.startService();
    }

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getRepeatCount() == 0) {
			exitApplication();
			return true;
		} else {
			return super.dispatchKeyEvent(event);
		}
	}
	
	/**
	 *  
	 * 
	 * @author winters_huang
	 */
	private void exitApplication() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(
				DemoAppActivity.this);
		alertDialog.setTitle("提示")
				.setMessage("退出").setPositiveButton("是",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								serviceManager.stopService();
								if (android.os.Build.VERSION.SDK_INT > 7) {
									System.exit(0);
								} else {
									ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
									am.restartPackage(getPackageName());
								}
							}
						}).setNegativeButton("否",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								return;
							}
						}).create();
		alertDialog.show();
	}
}