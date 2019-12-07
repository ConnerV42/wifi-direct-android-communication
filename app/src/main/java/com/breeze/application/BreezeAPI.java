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
import com.breeze.graph.BrzGraph;
import com.breeze.encryption.BrzEncryption;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ChatEvents.BrzChatHandshake;
import com.breeze.packets.ChatEvents.BrzChatResponse;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;
import com.breeze.views.ProfileActivity;

import java.util.List;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

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

        // Get stored hostNode info
        SharedPreferences sp = getSharedPreferences("Breeze", Context.MODE_PRIVATE);
        String hostNodeId = sp.getString(App.PREF_HOST_NODE_ID, "");
        BrzNode hostNode = this.db.getNode(hostNodeId);
        if (hostNode != null) {
            this.setHostNode(hostNode);
        } else {
            // Get a new profile since one isn't set
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            profileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(profileIntent);
        }

        // Get stored chats
        List<BrzChat> chats = null;
        try {
            chats = this.db.getAllChats();
            if (chats != null) {
                Log.i("STATE", "Found " + chats.size() + " chats in the database");
                this.state.addAllChats(chats);
            } else {
                Log.i("STATE", "No stored chats found!");
            }

        } catch (RuntimeException e) {
            Log.e("BREEZE_API", "Trying to load chats", e);
        }

        // Get stored messages
        try {
            if (chats != null) {
                for (BrzChat c : chats) {
                    List<BrzMessage> messages = this.db.getChatMessages(c.id);
                    if (messages != null) {
                        Log.i("STATE", "Found " + messages.size() + " messages in chat " + c.id);
                        this.state.addAllMessages(messages);
                    } else {
                        Log.i("STATE", "Failed to find messages in chat " + c.id);
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.e("BREEZE_API", "Trying to load chats", e);
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

//        BrzGraph.getInstance().addVertex(new BrzNode("1", "", "", "Jake", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("2", "", "", "Paul", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("3", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("4", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("5", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("6", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("7", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("8", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("9", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("10", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("11", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("12", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("13", "", "", "Conner", "@JJ"));
//        BrzGraph.getInstance().addVertex(new BrzNode("14", "", "", "Conner", "@JJ"));

        // TODO: create a keypair for the current hostNode
//        KeyPair keyPairForThisNode = null;
//        try{
//            keyPairForThisNode = BrzEncryption.generateAndSaveKeyPair();
//        }catch (Exception e){
//            Log.i("kp error", "Cannot generate key pair for this device");
//        }
//        if(keyPairForThisNode != null){
//            this.publicKey = keyPairForThisNode.getPublic();
//            this.privateKey = keyPairForThisNode.getPrivate();
//        }
//        else{
//            Log.i("bad keypair", "This device has no valid keypair in the store");
//        }

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

        // TODO: Generate chat encryption keys

        BrzChatHandshake handshake = new BrzChatHandshake(this.router.hostNode.id, chat, "", "");
        BrzPacket p = new BrzPacket(handshake);
        p.type = BrzPacket.BrzPacketType.CHAT_HANDSHAKE;

        for (String nodeId : chat.nodes) {
            p.to = nodeId;
            this.router.send(p);
        }

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
        BrzPacket p = new BrzPacket(message);
        p.type = BrzPacket.BrzPacketType.MESSAGE;

        // Send message to each recipient
        BrzChat chat = this.state.getChat(chatId);
        for (String nodeId : chat.nodes) {
            p.to = nodeId;
            this.router.send(p);
        }

        this.addMessage(message);
    }

    public void addMessage(BrzMessage message) {
        this.state.addMessage(message);
        this.db.addMessage(message);
    }

}
