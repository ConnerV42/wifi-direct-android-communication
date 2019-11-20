package com.breeze.application;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.breeze.App;
import com.breeze.MainActivity;
import com.breeze.R;
import com.breeze.database.DatabaseHandler;
import com.breeze.datatypes.BrzNode;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;

public class BreezeAPI extends Service {

    // Singleton

    private static BreezeAPI instance = new BreezeAPI();
    public static BreezeAPI getInstance() {
        return instance;
    }

    // Service overrides

    public BrzRouter router = null;
    public BrzStorage storage = null;
    public BrzStateStore state = null;
    public DatabaseHandler db = null;

    @Override
    public void onCreate() {
        super.onCreate();

        // Singleton
        instance = this;

        // Start our router and stuff with the service context
        this.router = BrzRouter.initialize(this, "BREEZE_MESSENGER");
        this.storage = BrzStorage.initialize(this);
        this.state = BrzStateStore.getStore();
        this.db = new DatabaseHandler(this);

        // Get stored hostNode info
        SharedPreferences sp = getSharedPreferences("Breeze", Context.MODE_PRIVATE);
        String hostNodeId = sp.getString(App.PREF_HOST_NODE_ID, null);
        if(hostNodeId != null) {
            BrzNode hostNode = this.db.getNode(hostNodeId);
            this.setHostNode(hostNode);
        }

        // Upgrade to foreground process
        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, notifIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("Breeze Service")
                .setContentText("Breeze is running in the background")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pending)
                .build();

        startForeground(1, notification);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service destroyed", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    // Application interface

    public void setHostNode(BrzNode hostNode) {

        // Set our id in preferences
        SharedPreferences sp = getSharedPreferences("Breeze", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(App.PREF_HOST_NODE_ID, hostNode.id);
        editor.apply();

        // Push the change elsewhere
        this.router.start(hostNode);
        this.state.setHostNode(hostNode);
        this.db.setNode(hostNode);
    }

    public void sendMessage(BrzMessage message, String to) {
        BrzPacket p = new BrzPacket(message);
        p.to = to;
        p.type = BrzPacket.BrzPacketType.MESSAGE;

        this.router.send(p);
        this.state.addMessage(message);
    }

    public void addChat(BrzChat chat) {
        this.state.addChat(chat);
    }
    public void addMessage(BrzMessage message) {
        this.state.addMessage(message);
    }
}
