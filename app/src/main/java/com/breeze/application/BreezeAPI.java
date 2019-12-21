package com.breeze.application;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
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
import com.breeze.graph.BrzGraph;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ChatEvents.BrzChatHandshake;
import com.breeze.packets.ChatEvents.BrzChatResponse;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;

public class BreezeAPI extends Service {

    // Singleton

    private static BreezeAPI instance = new BreezeAPI();

    public static BreezeAPI getInstance() {
        return instance;
    }

    // Behavioral Modules

    public BrzRouter router = null;
    public BrzStorage storage = null;
    public BrzStateStore state = null;
    public DatabaseHandler db = null;

    // Api modules

    public BreezeMetastateModule meta = null;

    // Data members

    public BrzNode hostNode = null;

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

        // Initialize api modules
        this.meta = new BreezeMetastateModule(this);

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


        // TODO: create a keypair for the current hostNode.. Done
        try {
            if(!BrzEncryption.storeContainsKey("MY_KEY"))
            {
                KeyPair kp = BrzEncryption.generateAndSaveKeyPair("MY_KEY");
                this.publicKey = kp.getPublic();
                this.privateKey = kp.getPrivate();
            }
            else
            {
                this.publicKey = BrzEncryption.getPublicKeyFromKeyStore("MY_KEY");
                this.privateKey = BrzEncryption.getPrivateKeyFromStore("MY_KEY");
            }
        }catch(Exception e)
        {
            Log.i("key error", "cannot gen and save keys");
        }
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

    //
    //
    //      Host Node
    //
    //

    public void setHostNode(BrzNode hostNode) {
        if (hostNode == null) return;

        this.hostNode = hostNode;

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

    //
    //
    //      Chat handshakes
    //
    //

    public void sendChatHandshakes(BrzChat chat) {
        chat.acceptedByHost = true;
        chat.acceptedByRecipient = false;

        if(!chat.nodes.contains(this.hostNode.id))
            chat.nodes.add(this.hostNode.id);

        // TODO: Generate chat encryption keys

        BrzChatHandshake handshake = new BrzChatHandshake(this.router.hostNode.id, chat, "", "");
        BrzPacket p = new BrzPacket(handshake);
        p.type = BrzPacket.BrzPacketType.CHAT_HANDSHAKE;

        for (String nodeId : chat.nodes) {
            if(nodeId.equals(hostNode.id)) continue;
            p.to = nodeId;
            this.router.send(p);
        }

        this.state.addChat(chat);
        this.db.setChat(chat);
    }

    public void updateChat(BrzChat chat) {
        this.state.addChat(chat);
        this.db.setChat(chat);
    }

    public void incomingChatResponse(BrzChatResponse response) {
        BrzChat c = this.state.getChat(response.chatId);
        if (c == null) return;

        // Chat accepted!
        if (response.accepted) {
            c.acceptedByRecipient = true;

            BrzNode n = BrzGraph.getInstance().getVertex(response.from);
            if (n != null) {
                BrzMessage sm = new BrzMessage(n.name + " accepeted the chat request!");
                sm.chatId = c.id;
                this.state.addMessage(sm);
                this.db.addMessage(sm);
            }
        }

        // Rejected and the chat is a group
        else if (c.isGroup) {
            c.nodes.remove(response.from);

            BrzNode n = BrzGraph.getInstance().getVertex(response.from);
            if (n != null) {
                BrzMessage sm = new BrzMessage(n.name + " rejected the chat.");
                sm.chatId = c.id;
                this.state.addMessage(sm);
                this.db.addMessage(sm);
            }
        }

        // Rejected and the chat is singular
        else {
            BrzNode n = BrzGraph.getInstance().getVertex(response.from);
            if (n != null) {
                BrzMessage sm = new BrzMessage(n.name + " rejected the chat.");
                sm.chatId = c.id;
                this.state.addMessage(sm);
                this.db.addMessage(sm);
            }
        }

        this.state.addChat(c);
        this.db.setChat(c);
    }

    public void incomingHandshake(BrzChatHandshake handshake) {
        BrzChat chat = handshake.chat;
        if (!chat.isGroup) {
            BrzNode n = BrzGraph.getInstance().getVertex(handshake.from);
            chat.name = n.name;
        }

        chat.acceptedByHost = false;
        chat.acceptedByRecipient = false;

        this.state.addChat(handshake.chat);
        this.db.setChat(handshake.chat);
    }

    public void acceptHandshake(BrzChatHandshake handshake) {
        BrzChatResponse response = new BrzChatResponse(this.hostNode.id, handshake.chat.id, true);

        // Send the response
        BrzPacket p = new BrzPacket(response);
        p.type = BrzPacket.BrzPacketType.CHAT_RESPONSE;
        p.to = handshake.from;
        this.router.send(p);

        // TODO: Add the keys to our keystore

        // Set state to have the chat accepted
        BrzChat c = this.state.getChat(handshake.chat.id);
        c.acceptedByHost = true;
        c.acceptedByRecipient = true;
        this.state.addChat(c);
        this.db.setChat(c);
    }

    public void rejectHandshake(BrzChatHandshake handshake) {
        BrzChatResponse response = new BrzChatResponse(this.hostNode.id, handshake.chat.id, false);

        BrzPacket p = new BrzPacket(response);
        p.type = BrzPacket.BrzPacketType.CHAT_RESPONSE;
        p.to = handshake.from;

        this.router.send(p);

        this.state.removeChat(handshake.chat.id);
        this.db.deleteChat(handshake.chat.id);
    }

    //
    //
    //      Messaging
    //
    //

    public void sendMessage(BrzMessage message, String chatId) {

        // Send message to each recipient
        BrzChat chat = this.state.getChat(chatId);
        for (String nodeId : chat.nodes) {
            if(nodeId.equals(hostNode.id)) continue;
            p.to = nodeId;
            this.router.send(p);
        }
        chat.sendMessageToChat(message);
        this.addMessage(message);
    }

    public void addMessage(BrzMessage message) {
        this.state.addMessage(message);
        this.db.addMessage(message);
    }

}
