package com.breeze.application;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.breeze.KillAppActivity;
import com.breeze.MainActivity;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.views.Chats.ChatHandshakeView;
import com.breeze.views.Messages.MessagesView;

public class BreezeBroadcastReceiver extends BroadcastReceiver {
    public static final String KEY_MESSAGE_REPLY = "MessageReply";

    public static final String ACTION_KILL_APPLICATION = "com.breeze.actions.KillApplication";
    public static final String ACTION_MESSAGE_REPLY = "com.breeze.actions.MessageReply";
    public static final String ACTION_CHAT_OPEN = "com.breeze.actions.ChatOpen";
    public static final String ACTION_CHAT_ACCEPT = "com.breeze.actions.ChatAccept";

    public static PendingIntent getKillApplicationIntent(Context ctx) {
        BreezeAPI api = BreezeAPI.getInstance();
        Intent i = new Intent(ctx, BreezeBroadcastReceiver.class);
        i.setAction(ACTION_KILL_APPLICATION);
        return PendingIntent.getBroadcast(api, 0, i, PendingIntent.FLAG_ONE_SHOT);
    }

    public static PendingIntent getMessageReplyIntent(Context ctx, String chatId, int notifId) {
        BreezeAPI api = BreezeAPI.getInstance();
        Intent i = new Intent(ctx, BreezeBroadcastReceiver.class);
        i.setAction(ACTION_MESSAGE_REPLY);
        i.putExtra("chatId", chatId);
        i.putExtra("notifId", notifId);

        return PendingIntent.getBroadcast(api, notifId, i, PendingIntent.FLAG_ONE_SHOT);
    }

    public static PendingIntent getOpenChatIntent(Context ctx, String chatId) {
        BreezeAPI api = BreezeAPI.getInstance();
        Intent i = new Intent(ctx, BreezeBroadcastReceiver.class);
        i.setAction(ACTION_CHAT_OPEN);
        i.putExtra("chatId", chatId);

        return PendingIntent.getBroadcast(api, 0, i, PendingIntent.FLAG_ONE_SHOT);
    }

    public static PendingIntent getAcceptChatIntent(Context ctx, String chatId) {
        BreezeAPI api = BreezeAPI.getInstance();
        Intent i = new Intent(ctx, BreezeBroadcastReceiver.class);
        i.setAction(ACTION_CHAT_ACCEPT);
        i.putExtra("chatId", chatId);
        return PendingIntent.getBroadcast(api, 0, i, PendingIntent.FLAG_ONE_SHOT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        BreezeAPI api = BreezeAPI.getInstance();
        String action = intent.getAction();
        if (action == null || action.isEmpty()) return;

        Log.i("BROADCAST", "Received a broadcast! " + action);
        if (action.equals(ACTION_KILL_APPLICATION)) {
            Intent i = new Intent(api, KillAppActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            api.startActivity(i);
        }
        else if (action.equals(ACTION_MESSAGE_REPLY)) {
            String chatId = intent.getStringExtra("chatId");
            int notifId = intent.getIntExtra("notifId", 0);
            String messageText = getMessageText(intent);

            BrzPacket p = BrzPacketBuilder.message(api.hostNode.id, "", messageText, chatId, false);
            Log.i("BROADCAST", p.body);
            api.sendMessage(p.message());

            // Update the notification
            api.meta.addMessageToNotification(api, notifId, p.message());
        }

        else if (action.equals(ACTION_CHAT_OPEN)) {
            String chatId = intent.getStringExtra("chatId");
            api.startActivity(MessagesView.getIntent(api, chatId));
            api.meta.removeNotification(chatId);
        }

        else if (action.equals(ACTION_CHAT_ACCEPT)) {
            String chatId = intent.getStringExtra("chatId");
            api.startActivity(ChatHandshakeView.getIntent(api, chatId));
            api.meta.removeNotification(chatId);
        }

    }

    private String getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getString(KEY_MESSAGE_REPLY);
        }
        return null;
    }
}
