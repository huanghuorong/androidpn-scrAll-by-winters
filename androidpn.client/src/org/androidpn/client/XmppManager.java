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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import org.huofire.common.config.AndroidpnConfiguration;
import org.huofire.common.utils.AndroidpnLogger;
import org.huofire.common.utils.DeviceTokenUtils;
import org.huofire.common.utils.Log;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Authentication;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.packet.Time;
import org.jivesoftware.smackx.packet.VCard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler; 

/**
 * This class is to manage the XMPP connection between client and server.
 * 修改说明：</p>
 * androidpn.client 修改自androidpn-client-src-0.5</p>
 * androidpn.smack  修改自smack-src-3.2.2，不用jar的原因：方便调试</p>
 *    
 * @author huanghr.1@gmail.com
 * @see 2012.11
 * @see 
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class XmppManager {

    private static final String LOGTAG = LogUtil.makeLogTag(XmppManager.class);
    private static final String XMPP_RESOURCE_NAME = "AndroidpnClient";//TODO 服务端写死了这里只能用 AndroidpnClient，否则单播时无法下发信息
    private Context context;
    private PushDemoService.TaskSubmitter taskSubmitter;
    private PushDemoService.TaskTracker taskTracker;
    private SharedPreferences sharedPrefs;
    private String xmppHost;
    private int xmppPort;
    private XMPPConnection connection;
    private String username;
    private String password;
    private ConnectionListener connectionListener;
    private PacketListener notificationPacketListener;
    private Handler handler;
    private List<Runnable> taskList;
    private boolean running = false;
    private Future<?> futureTask;
    private Thread reconnection;
    private long lastActive = System.currentTimeMillis();
    private long delay = 5*60*1000; //5分钟检查网络是否正常

    /**
     * @param notificationService
     */
    public XmppManager(PushDemoService notificationService) {
        context = notificationService;
        taskSubmitter = notificationService.getTaskSubmitter();
        taskTracker = notificationService.getTaskTracker();
        sharedPrefs = notificationService.getSharedPreferences();

        xmppHost = sharedPrefs.getString(Constants.XMPP_HOST, AndroidpnConfiguration.xmppHost);
        xmppPort = sharedPrefs.getInt(Constants.XMPP_PORT, AndroidpnConfiguration.xmppPort);
        username = sharedPrefs.getString(Constants.XMPP_USERNAME, "");
        password = sharedPrefs.getString(Constants.XMPP_PASSWORD, "");
        //侦听网络异常：端口关闭，连接断开 FIXME 但在移动网络/wifi模式下不可靠,会出现空连接，建议重连策略自定义，@see KeepAliveSingleTask
        connectionListener = new PersistentConnectionListener(this);
        //侦听XMPP消息包
        notificationPacketListener = new NotificationPacketListener(this);

        handler = new Handler();
        taskList = new ArrayList<Runnable>();
        reconnection = new ReconnectionThread(this);
    }

    public Context getContext() {
        return context;
    }

    public void connect() {
    	// 一旦进入重新链接，取消现有队列的所有任务，避免出现相同功能的任务以及已经无效的任务；@winters
    	// if (isRegistered()) { //如果不在这里注册，可以先判断是否已经注册，避免无效的重连。
	    	cancelAllTask();
	    	if(checkNetworkConnected()){
	    		AndroidpnLogger.d(LOGTAG, "connect ...");
		        submitLoginTask();
		        runTask();
	    	}
//    	}else{
//    		AndroidpnLogger.e(LOGTAG, "DeviceId is null,waiting to store...");
//    	}
    }

    public void disconnect() {
    	AndroidpnLogger.d(LOGTAG, "disconnect...");
        cancelAllTask();
        terminatePersistentConnection();
        runTask();
    }

    /**
     * 检查网络物理连接是否可用，避免无网络情况下频频连接消耗电量
     * @author huanghuorong
     * @return boolean
     */
    public boolean checkNetworkConnected(){
    	try{
			ConnectivityManager connectivity = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivity == null) {
				return false;
			} else {
				NetworkInfo netWrokInfo = connectivity.getActiveNetworkInfo();
				if (netWrokInfo == null || !netWrokInfo.isAvailable()) {
					return false;
				}
			}
    	}catch(Exception ex){
    		AndroidpnLogger.e(LOGTAG, "checkNetworkConnected catch an error", ex);
    	}
    	return true;
    }
    
    public void terminatePersistentConnection() {
        Runnable runnable = new Runnable() {
            final XmppManager xmppManager = XmppManager.this;
            public void run() {
                if (xmppManager.isConnected()) {
                    //Log.d(LOGTAG, "terminatePersistentConnection()... run()");
                    xmppManager.getConnection().removePacketListener(xmppManager.getNotificationPacketListener());
                    xmppManager.getConnection().disconnect();
                }
            }
        };
        addTask(runnable);
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public PacketListener getNotificationPacketListener() {
        return notificationPacketListener;
    }

    public void startReconnectionThread() {
//    	KdweiboLogger.i(LOGTAG, "i will use winters's keep alive task instead~~~~~~~~~~~~~~~~~");
//        synchronized (reconnection) {
//            if (!reconnection.isAlive()) {
//                reconnection.setName("Xmpp Reconnection Thread");
//                reconnection.start();
//            }
//        }
    }

    public Handler getHandler() {
        return handler;
    }

//    public void reregisterAccount() {
//        removeAccount();
//        submitLoginTask();
//        runTask();
//    }

    public List<Runnable> getTaskList() {
        return taskList;
    }

    public Future<?> getFutureTask() {
        return futureTask;
    }

    public void runTask() {
        //Log.d(LOGTAG, "runTask()...");
        synchronized (taskList) {
            running = false;
            futureTask = null;
            if (!taskList.isEmpty()) {
                Runnable runnable = (Runnable) taskList.get(0);
                taskList.remove(0);
                running = true;
                //Log.d(LOGTAG, "running task ["+runnable.getClass().getSimpleName()+"]...");
                futureTask = taskSubmitter.submit(runnable);
                if (futureTask == null) {
                    taskTracker.decrease();
                }
            }
        }
        taskTracker.decrease();
    }
    
    /**
     * by winters_huang
     */
    public void startKeepAliveTask(){
    	new Thread(new KeepAliveSingleTask()).start();
    }
    private String newRandomUUID() {
        String uuidRaw = UUID.randomUUID().toString();
        return uuidRaw.replaceAll("-", "");
    }

    private boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    private boolean isAuthenticated() {
        return connection != null && connection.isConnected()
                && connection.isAuthenticated();
    }

    public boolean isRegistered() {
        return sharedPrefs.contains(Constants.XMPP_USERNAME)
                && sharedPrefs.contains(Constants.XMPP_PASSWORD);
    }

    private void submitConnectTask() {
        addTask(new ConnectTask());
    }

    //TODO 可在你的app中先注册
    private void submitRegisterTask() {
//        Log.d(LOGTAG, "submitRegisterTask()...");
        submitConnectTask();
        addTask(new RegisterTask());
    }

    private void submitLoginTask() {
//        Log.d(LOGTAG, "submitLoginTask()...");
        submitRegisterTask(); 
        addTask(new LoginTask());
    }

    private void addTask(Runnable runnable) {
//        Log.d(LOGTAG, "addTask(runnable)...");
        taskTracker.increase();
        synchronized (taskList) {
            if (taskList.isEmpty() && !running) {
                running = true;
                futureTask = taskSubmitter.submit(runnable);
                if (futureTask == null) {
                    taskTracker.decrease();
                }
            } else {
            	//解决服务器端重启后,客户端不能成功连接androidpn服务器
                //runTask(); by winters 没搞懂，重连得了
                taskList.add(runnable);
            }
        } 
    }
    
    //取消所有任务，如当异常重连接时。
    public void cancelAllTask(){
    	synchronized (taskList) {
    		if (!taskList.isEmpty()) {
    			taskTracker.reset();
    			taskList.clear();
    		}
    	} 
    }

    private void removeAccount() {
        Editor editor = sharedPrefs.edit();
        editor.remove(Constants.XMPP_USERNAME);
        editor.remove(Constants.XMPP_PASSWORD);
        editor.commit();
    }

    /**
     * A runnable task to connect the server. 
     */
    private class ConnectTask implements Runnable {
        final XmppManager xmppManager;

        private ConnectTask() {
            this.xmppManager = XmppManager.this;
        }
        public void run() {
            if (!xmppManager.isConnected()) {
                // Create the configuration for this new connection
                ConnectionConfiguration connConfig = new ConnectionConfiguration(
                        xmppHost, xmppPort);
                 connConfig.setSecurityMode(SecurityMode.disabled); // by winters_huang
                // connConfig.setSecurityMode(SecurityMode.required);
                connConfig.setSASLAuthenticationEnabled(false);
                connConfig.setCompressionEnabled(false);
                // 解决 java.security.KeyStoreException: KeyStore jks implementation not found
                // @seeto http://blog.csdn.net/joans123/article/details/7566063
                connConfig.setTruststorePath("/system/etc/security/cacerts.bks");
                connConfig.setTruststoreType("bks");
                

                XMPPConnection connection = new XMPPConnection(connConfig);
                xmppManager.setConnection(connection);

                try {
                    // Connect to the server
                	AndroidpnLogger.i(LOGTAG, "XMPP connecting...");
                    connection.connect();
                    AndroidpnLogger.i(LOGTAG, "XMPP connected successfully");

                    // packet provider
                    ProviderManager.getInstance().addIQProvider("notification","androidpn:iq:notification", new NotificationIQProvider());
                } catch (Exception e) {//捕捉所有异常，避免空指针
                	AndroidpnLogger.e(LOGTAG, "XMPP connection failed,Cause by:"+e.getMessage());
                }
            } else {
            	AndroidpnLogger.i(LOGTAG, "XMPP connected already,skip it.");
            }
            runTask(); //先同步登录，在执行其它操作；
        }
    }

    /**
     * A runnable task to register a new user onto the server. 
     */
    private class RegisterTask implements Runnable {

        final XmppManager xmppManager;

        private RegisterTask() {
            xmppManager = XmppManager.this;
        }

        public void run() {
            Log.i(LOGTAG, "RegisterTask.run()...");

            if (!xmppManager.isRegistered()) {
            	//FIXME 根据自己的安全策略
                final String newUsername = DeviceTokenUtils.getDeviceToken(context);//newRandomUUID();
                final String newPassword = DeviceTokenUtils.getDeviceToken(context);//newRandomUUID();

                Registration registration = new Registration();

                PacketFilter packetFilter = new AndFilter(new PacketIDFilter(
                        registration.getPacketID()), new PacketTypeFilter(
                        IQ.class));

                PacketListener packetListener = new PacketListener() {

                    public void processPacket(Packet packet) {
                        Log.d("RegisterTask.PacketListener",
                                "processPacket().....");
                        Log.d("RegisterTask.PacketListener", "packet="
                                + packet.toXML());

                        if (packet instanceof IQ) {
                            IQ response = (IQ) packet;
                            if (response.getType() == IQ.Type.ERROR) {
                                if (!response.getError().toString().contains(
                                        "409")) {
                                    Log.e(LOGTAG,
                                            "Unknown error while registering XMPP account! "
                                                    + response.getError()
                                                            .getCondition());
                                }
                            } else if (response.getType() == IQ.Type.RESULT) {
                                xmppManager.setUsername(newUsername);
                                xmppManager.setPassword(newPassword);
                                Log.d(LOGTAG, "username=" + newUsername);
                                Log.d(LOGTAG, "password=" + newPassword);

                                Editor editor = sharedPrefs.edit();
                                editor.putString(Constants.XMPP_USERNAME,
                                        newUsername);
                                editor.putString(Constants.XMPP_PASSWORD,
                                        newPassword);
                                editor.commit();
                                Log
                                        .i(LOGTAG,
                                                "Account registered successfully");
                                xmppManager.runTask();
                            }
                        }
                    }
                };

                connection.addPacketListener(packetListener, packetFilter);

                registration.setType(IQ.Type.SET);
                // registration.setTo(xmppHost);
                // Map<String, String> attributes = new HashMap<String, String>();
                // attributes.put("username", rUsername);
                // attributes.put("password", rPassword);
                // registration.setAttributes(attributes);
                Map map = new HashMap();
                registration.setAttributes(map);
                registration.getAttributes().put("username", newUsername);
                registration.getAttributes().put("password", newPassword);
                connection.sendPacket(registration);

            } else {
                Log.i(LOGTAG, "Account registered already");
                xmppManager.runTask();
            }
        }
    }

    /**
     * A runnable task to log into the server. 
     */
    private class LoginTask implements Runnable {

        final XmppManager xmppManager;

        private LoginTask() {
            this.xmppManager = XmppManager.this;
        }

        public void run() {
            //Log.i(LOGTAG, "LoginTask.run()...");
            if (!xmppManager.isAuthenticated()) {
                //Log.d(LOGTAG, "username=" + username +" password=" + password);
                try {
                    xmppManager.getConnection().login(xmppManager.getUsername(), xmppManager.getPassword(), XMPP_RESOURCE_NAME);
                    AndroidpnLogger.d(LOGTAG, "Logged in successfully");
                    // connection listener
                    if (xmppManager.getConnectionListener() != null) {
                        xmppManager.getConnection().addConnectionListener(xmppManager.getConnectionListener());
                    }

                    // packet filter
                    PacketFilter packetFilter = new PacketTypeFilter(NotificationIQ.class);
                    // packet listener
                    PacketListener packetListener = xmppManager.getNotificationPacketListener();
                    connection.addPacketListener(packetListener, packetFilter);
                    runTask();//失败后不再call run task，该取消以前的队列？
                } catch (XMPPException e) {
                	AndroidpnLogger.e(LOGTAG, "Failed to login to xmpp server. Caused by: " + e.getMessage());
//                    String INVALID_CREDENTIALS_ERROR_CODE = "401";
//                    String errorMessage = e.getMessage();
                    /**
                     * 使用http保存token，不在此处注册；
                    if (errorMessage != null
                            && errorMessage
                                    .contains(INVALID_CREDENTIALS_ERROR_CODE)) {
                        xmppManager.reregisterAccount();
                        return;
                    }
                    */
                } catch (Exception e) {
                	AndroidpnLogger.e(LOGTAG, "Failed to login to xmpp server. Caused by: " + e.getMessage());
                    xmppManager.startReconnectionThread();
                } finally{
                }

            } else {
            	AndroidpnLogger.i(LOGTAG, "Logged in already, skip it");
                runTask();
            }

        }
    }
    
    /**
     * 发送心跳数据包，并等待服务器回响，超时后通知重连。</p>
     * 不使用XMPPConnection#packetWriter.startKeepAliveProcess() 的原因：
     * 1）发送失败也无法捕捉到exception，如路由器wan拔掉的情况 
     * 2）关闭手机屏幕cpu进入睡眠后，心跳线程会被挂起，这里通过 AlarmManager 30s间隔唤醒
     * AlarmManager 每30s 唤醒手机
     * 
     * @author winters_huang
     */
    private class KeepAliveSingleTask implements Runnable {
        final XmppManager xmppManager;

        private KeepAliveSingleTask() {
            xmppManager = XmppManager.this;
        }

        public void run() {
        	// KdweiboLogger.i(LOGTAG, "Alart manager run at "+ (new Date().toString()) +"...");
            // 这里本来需要判断只有 isConnected() 才进行keepAlive，但考虑到 isConnected状态并不准确，而且keepAlive的目的除了保持心跳还要检查 xmpp server是否可用,应不做任何判断
            if (xmppManager!=null &&connection!=null) {  
            	try {
                    if (!isConnected()) {
                        throw new XMPPException("Not connected to server.");
                    }
                    if (!isRegistered()) {
                        throw new XMPPException("Has not register yet.");
                    }
                    if (!isAuthenticated()) {
                        throw new XMPPException("Has not login yet.");
                    }
                    //降低流量消耗，每个5分钟询问服务器一次，并等待回响
                    if(System.currentTimeMillis() - lastActive > delay){
                    	//KdweiboLogger.i(LOGTAG, "Query server status at "+ (new Date().toString()) +"...");
	                    //Androidpn-server并阉割了XMPP服务器的大多数处理，如获取vCard,Time等，此类请求会返回503
	                    //Presence presence = new Presence(Presence.Type.available);
	                    //Time timeRequest = new Time();
	                    //timeRequest.setType(IQ.Type.GET);
	                    Authentication auth = new Authentication();
	                    auth.setType(IQ.Type.GET); 
	                    auth.setUsername(username);
	                    //IQ checkStatusIQ = new IQ();
	                    PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(auth.getPacketID()));
	                    // Send the packet
	                    connection.sendPacket(auth);
	                    // Wait up to a certain number of seconds for a response from the server.
	                    // 解包由客户端处理，这里仅发送Presence 是不会有回响信息的，某些路由器如果某个环节断网，会导致长达50+分钟的悬空连接不触发connectionClose
	                    // FIXME 后续可以通过IQ更准确的确定是否重连，但代价会有点大。
	                    // 检查网络WIFI/3G确定心跳频率，要考虑睡眠导致 
	                    int timeout = SmackConfiguration.getPacketReplyTimeout();
	                    IQ result = (IQ) collector.nextResult(timeout);
	                     if (result != null) {
	                         //KdweiboLogger.i(LOGTAG,"Reponse server time when keep alive:["+result.toXML()+"],length ["+result.toXML().length()+"]byte");
	                     }else{
	                    	 throw new XMPPException("Catch error,no response after PacketReplyTimeout["+timeout+"]");
	                     }
	                     lastActive = System.currentTimeMillis();
                    }
                    
				} catch (Exception e) {
					AndroidpnLogger.e(LOGTAG,"Keep Alive catch error,Cause by:"+e.getMessage() +",now call reconnect");
					disconnect();
					connect();
				}
            }
        }
    }

}
