package com.breeze;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    public static final String CHANNEL_ID = "breezeForegroundChannel";
    public static final String PREF_HOST_NODE_ID = "hostNodeID";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Breeze Service Channel", NotificationManager.IMPORTANCE_NONE
            );
            NotificationManager m = getSystemService(NotificationManager.class);
            m.createNotificationChannel(serviceChannel);
        }
    }

}
