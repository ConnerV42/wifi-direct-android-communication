package com.breeze.application;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.breeze.datatypes.BrzNode;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
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

    @Override
    public void onCreate() {
        super.onCreate();

        this.router = BrzRouter.initialize(this, "BREEZE_MESSENGER");
        this.storage = BrzStorage.initialize(this);
        this.state = BrzStateStore.getStore();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Not bindable
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Application interface

    public void setHostNode(BrzNode hostNode) {
        this.router.start(hostNode);
        this.state.setHostNode(hostNode);
    }

    public void addChat(BrzChat chat) {
        this.state.addChat(chat);
    }

    public void addMessage(BrzMessage message) {
        this.state.addMessage(message);
    }
}
