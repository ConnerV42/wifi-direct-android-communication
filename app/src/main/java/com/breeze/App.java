package com.breeze;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    public static final String SERVICE_CHANNEL_ID = "breezeForegroundChannel";
    public static final String MESSAGE_CHANNEL_ID = "breezeMessageChannel";
    public static final String PREF_HOST_NODE_ID = "hostNodeID";

    public NotificationManager notificationManager = null;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    SERVICE_CHANNEL_ID, "Breeze Service Channel", NotificationManager.IMPORTANCE_NONE
            );
            NotificationChannel messageChannel = new NotificationChannel(
                    MESSAGE_CHANNEL_ID, "Breeze Messages", NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager m = getSystemService(NotificationManager.class);
            if (m != null) {
                this.notificationManager = m;
                m.createNotificationChannel(serviceChannel);
                m.createNotificationChannel(messageChannel);
            }
        }
    }

}
