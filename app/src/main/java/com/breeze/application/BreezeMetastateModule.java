package com.breeze.application;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.breeze.App;
import com.breeze.R;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.graph.BrzGraph;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;

import java.util.HashMap;
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

        // Get stored nodes
        List<BrzNode> nodes;
        try {
            nodes = api.db.getAllNodes();
            if (nodes != null) {
                Log.i("STATE", "Found " + nodes.size() + " nodes in the database");
                api.state.setNodes(nodes);
            } else {
                Log.i("STATE", "No stored chats found!");
            }
        } catch (RuntimeException e) {
            Log.e("BREEZE_API", "Trying to load chats", e);
        }
    }

    void setHostNode(BrzNode hostNode) {
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

    //--------------------------------------------------------------------------------------------//
    //                                     Notifications                                          //
    //--------------------------------------------------------------------------------------------//

    private HashMap<String, Integer> activeNotifications = new HashMap<>();

    public void removeNotification(String id) {
        Integer notifId = activeNotifications.get(id);
        if (notifId == null) return;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.api);
        notificationManager.cancel(notifId);
    }

    public void removeAllNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.api);
        for (int notifId : activeNotifications.values()) {
            notificationManager.cancel(notifId);
        }
    }


    public void showMessageNotification(BrzMessage message) {
        BrzChat c = this.api.state.getChat(message.chatId);
        if (api.state.getCurrentChat().equals(message.chatId)) return;

        // Check if there's already a notification active
        Integer notifId = activeNotifications.get(message.chatId);
        if (notifId != null) {
            addMessageToNotification(this.api, notifId, message);
            return;
        }

        // ---------------------------------------------------------//
        //              Or build a new notification                 //
        // ---------------------------------------------------------//

        notifId = new Random().nextInt(2000) + 5;
        activeNotifications.put(message.chatId, notifId);

        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(getPerson(api.hostNode));
        style.setConversationTitle(c.name);

        // Add the new message
        BrzNode n = api.state.getNode(message.from);
        style.addMessage(message.body, message.datestamp, getPerson(n));
        Notification notification = getMessageNotificationBuilder(notifId, style, message.chatId).build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.api);
        notificationManager.notify(notifId, notification);
    }


    public void showHandshakeNotification(String chatId) {
        BrzChat c = this.api.state.getChat(chatId);
        if (c == null || api.state.getCurrentChat().equals(chatId)) return;

        // Check if there's already a notification active
        Integer notifId = activeNotifications.get(chatId);
        if (notifId != null) return;


        // ---------------------------------------------------------//
        //              Or build a new notification                 //
        // ---------------------------------------------------------//

        notifId = new Random().nextInt(2000) + 5;
        activeNotifications.put(chatId, notifId);

        Notification notification = new NotificationCompat.Builder(this.api, App.MESSAGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(c.name)
                .setContentText("You've been invited to join this chat!")
                .setAutoCancel(true)
                .setContentIntent(BreezeBroadcastReceiver.getAcceptChatIntent(api, chatId))
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.api);
        notificationManager.notify(notifId, notification);
    }


    // Notification helpers

    private Notification getActiveNotification(Context ctx, int notifId) {
        try {
            NotificationManager manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return null;
            StatusBarNotification[] notifications = manager.getActiveNotifications();
            for (StatusBarNotification notif : notifications) {
                if (notif.getId() == notifId)
                    return notif.getNotification();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Person getPerson(BrzNode n) {
        if (n == null) {
            return new Person.Builder()
                    .setBot(false)
                    .setName("Disconnected")
                    .build();
        }

        Person.Builder builder = new Person.Builder();

        if (api.storage.hasProfileImage(api.storage.PROFILE_DIR, n.id)) {
            Bitmap personIcon = api.storage.getProfileImage(api.storage.PROFILE_DIR, n.id);
            builder.setIcon(IconCompat.createWithBitmap(getRoundIcon(personIcon)));
        }

        return builder
                .setBot(false)
                .setName(n.id.equals(api.hostNode.id) ? "You" : n.name)
                .setKey(n.id)
                .build();
    }

    void addMessageToNotification(Context ctx, int notifId, BrzMessage message) {
        // Get the chat
        BrzChat c = api.state.getChat(message.chatId);
        if (c == null) return;

        // Get the style / messages from the notification
        Notification notif = getActiveNotification(ctx, notifId);
        NotificationCompat.MessagingStyle msgStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notif);

        // Create a new message style with all the previous messages
        NotificationCompat.MessagingStyle newMsgStyle = new NotificationCompat.MessagingStyle(getPerson(api.hostNode));
        newMsgStyle.setConversationTitle(c.name);
        if (msgStyle != null && msgStyle.getMessages() != null) {
            for (NotificationCompat.MessagingStyle.Message msg : msgStyle.getMessages()) {
                newMsgStyle.addMessage(msg);
            }
        }

        // Get the builder from the notification
        NotificationCompat.Builder builder = getMessageNotificationBuilder(notifId, newMsgStyle, message.chatId);

        // Add the new message
        BrzNode n = api.state.getNode(message.from);
        newMsgStyle.addMessage(message.body, message.datestamp, getPerson(n));

        // Set the new style to the recovered builder.
        builder.setStyle(newMsgStyle);

        // Update the active notification.
        NotificationManagerCompat.from(ctx).notify(notifId, builder.build());
    }

    private NotificationCompat.Builder getMessageNotificationBuilder(int notifId, NotificationCompat.Style style, String chatId) {
        PendingIntent pending = BreezeBroadcastReceiver.getOpenChatIntent(api, chatId);

        // Reply enabling stuff
        RemoteInput remoteInput = new RemoteInput.Builder(BreezeBroadcastReceiver.KEY_MESSAGE_REPLY)
                .setLabel("New Message")
                .build();
        PendingIntent replyPendingIntent = BreezeBroadcastReceiver.getMessageReplyIntent(api, chatId, notifId);

        // Create the reply action and add the remote input.
        NotificationCompat.Action action = new NotificationCompat.Action
                .Builder(R.drawable.ic_send_black_24dp, "Reply", replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        return new NotificationCompat.Builder(this.api, App.MESSAGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setStyle(style)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(action);
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

    private Bitmap getRoundIcon(Bitmap icon) {
        RoundedBitmapDrawable iconRounded = RoundedBitmapDrawableFactory.create(api.getResources(), icon);
        iconRounded.setCornerRadius(100.0f);
        iconRounded.setAntiAlias(true);

        return drawableToBitmap(iconRounded);
    }

}
