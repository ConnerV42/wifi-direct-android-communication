package com.breeze.application;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.breeze.App;
import com.breeze.R;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.views.Messages.MessagesView;

import java.util.List;

public class BreezeMetastateModule extends BreezeModule {
    BreezeMetastateModule(BreezeAPI api) {
        super(api);

        // Get stored chats
        List<BrzChat> chats = null;
        try {
            chats = api.db.getAllChats();
            if (chats != null) {
                Log.i("STATE", "Found " + chats.size() + " chats in the database");
                api.state.addAllChats(chats);
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
                    List<BrzMessage> messages = api.db.getChatMessages(c.id);
                    if (messages != null) {
                        Log.i("STATE", "Found " + messages.size() + " messages in chat " + c.id);
                        api.state.addAllMessages(messages);
                    } else {
                        Log.i("STATE", "Failed to find messages in chat " + c.id);
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.e("BREEZE_API", "Trying to load chats", e);
        }

//        List<String> nodes = new ArrayList<>();

//        nodes.add("test");
//        BrzChat chat = new BrzChat("Test Chat", nodes);
//
//        BrzMessage msg = new BrzMessage("Testing date stuff", "test");
//        msg.chatId = chat.id;
//
//        BrzMessage msg2 = new BrzMessage("Testing date stuff for outgoing", api.hostNode.id);
//        msg2.chatId = chat.id;
//
//        api.state.addChat(chat);
//        api.state.addMessage(msg);
//        api.state.addMessage(msg2);
//        BrzGraph.getInstance().addVertex(new BrzNode("test", "", "", "Jake", "@JJ"));
//
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

    }

    public boolean getCachedHostNode() {
        // Get stored hostNode info
        SharedPreferences sp = api.getSharedPreferences("Breeze", Context.MODE_PRIVATE);
        String hostNodeId = sp.getString(App.PREF_HOST_NODE_ID, "");
        BrzNode hostNode = api.db.getNode(hostNodeId);
        if (hostNode != null) {
            api.setHostNode(hostNode);
            return true;
        } else {
            return false;
        }
    }

    public void sendDeliveryReceipt(BrzMessage m) {
        BrzPacket p = BrzPacketBuilder.messageReceipt(m.from, m.chatId, m.id, true);
        api.router.send(p);

        // Tracking delivery isn't really necessary
        // this.api.db.setDelivered(m.id);
    }

    public void sendReadReceipt(BrzMessage m) {
        BrzPacket p = BrzPacketBuilder.messageReceipt(m.from, m.chatId, m.id, false);
        api.router.send(p);
        this.api.db.setRead(m.id);
    }

    public void setDelivered(String messageId) {
        this.api.db.setDelivered(messageId);
        this.emit("delivered", messageId);
    }

    public void setRead(String messageId) {
        this.api.db.setRead(messageId);
        this.emit("read", messageId);
    }

    public void showNotification(BrzMessage message) {
        BrzChat c = this.api.state.getChat(message.chatId);

        Intent chatIntent = MessagesView.getIntent(this.api, message.chatId);
        PendingIntent pending = PendingIntent.getActivity(this.api, 0, chatIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this.api, App.MESSAGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setColor(Color.WHITE)
                .setContentTitle(c.name)
                .setContentText(message.body)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.api);
        notificationManager.notify(2, notification);

        Log.i("STATE", "Showing a notification");
    }
}
