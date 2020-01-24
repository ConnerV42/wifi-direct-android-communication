package com.breeze.application;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.breeze.App;
import com.breeze.R;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.storage.BrzStorage;
import com.breeze.views.Chats.ChatHandshakeView;
import com.breeze.views.Messages.MessagesView;

import java.util.List;
import java.util.Random;

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
        // this.setDelivered(m.id);
    }

    public void sendReadReceipt(BrzMessage m) {
        BrzPacket p = BrzPacketBuilder.messageReceipt(m.from, m.chatId, m.id, false);
        api.router.send(p);
        this.setRead(m.id);
    }

    public void setDelivered(String messageId) {
        this.api.db.setDelivered(messageId);
        this.emit("delivered", messageId);
    }

    public void setRead(String messageId) {
        this.api.db.setRead(messageId);
        this.emit("read", messageId);
    }

    public void showMessageNotification(BrzMessage message) {
        BrzChat c = this.api.state.getChat(message.chatId);
        if (api.state.getCurrentChat().equals(message.chatId)) return;

        int notifId = new Random().nextInt(2000);

        Intent chatIntent = MessagesView.getIntent(this.api, message.chatId);
        PendingIntent pending = PendingIntent.getActivity(this.api, 0, chatIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap chatImage;
        if (!c.isGroup)
            chatImage = api.storage.getProfileImage(c.otherPersonId(), api);
        else
            chatImage = api.storage.getProfileImage(c.id, api);

        // Reply enabling stuff
        RemoteInput remoteInput = new RemoteInput.Builder(BreezeBroadcastReceiver.KEY_MESSAGE_REPLY)
                .setLabel("New Message")
                .build();
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                api,
                notifId,
                BreezeBroadcastReceiver.getMessageReplyIntent(api, c.id, notifId),
                PendingIntent.FLAG_ONE_SHOT);
        // Create the reply action and add the remote input.
        NotificationCompat.Action action = new NotificationCompat.Action
                .Builder(R.drawable.ic_send_black_24dp, "Reply", replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        RoundedBitmapDrawable chatRounded = RoundedBitmapDrawableFactory.create(api.getResources(), chatImage);
        chatRounded.setCornerRadius(100.0f);
        chatRounded.setAntiAlias(true);

        RemoteViews notifLayout = new RemoteViews(api.getPackageName(), R.layout.message_notification);
        notifLayout.setImageViewBitmap(R.id.message_notif_image, drawableToBitmap(chatRounded));
        notifLayout.setTextViewText(R.id.message_notif_name, c.name);
        notifLayout.setTextViewText(R.id.message_notif_body, message.body);

        Notification notification = new NotificationCompat.Builder(this.api, App.MESSAGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notifLayout)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(action)
                .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.api);
        notificationManager.notify(notifId, notification);
    }

    public void showHandshakeNotification(String chatId) {
        BrzChat c = this.api.state.getChat(chatId);
        Intent chatIntent = ChatHandshakeView.getIntent(api, chatId);
        PendingIntent pending = PendingIntent.getActivity(this.api, 0, chatIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this.api, App.MESSAGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(c.name)
                .setContentText("You've been invited to join this chat!")
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.api);
        notificationManager.notify(3, notification);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
