package com.breeze.application;

import android.app.Notification;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.breeze.App;
import com.breeze.R;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;

public class BreezeBroadcastReceiver extends BroadcastReceiver {
    public static final String KEY_MESSAGE_REPLY = "MessageReply";
    public static final String ACTION_MESSAGE_REPLY = "com.breeze.actions.MessageReply";

    public static Intent getMessageReplyIntent(Context ctx, String chatId, int notifId) {
        Intent i = new Intent(ctx, BreezeBroadcastReceiver.class);
        i.setAction(ACTION_MESSAGE_REPLY);
        i.putExtra("chatId", chatId);
        i.putExtra("notifId", notifId);

        return i;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        BreezeAPI api = BreezeAPI.getInstance();
        String action = intent.getAction();
        if (action == null || action.isEmpty()) return;

        Log.i("BROADCAST", "Received a broadcast! " + action);

        if (action.equals(ACTION_MESSAGE_REPLY)) {
            String chatId = intent.getStringExtra("chatId");
            int notifId = intent.getIntExtra("notifId", 0);
            String messageText = getMessageText(intent);

            BrzPacket p = BrzPacketBuilder.message(api.hostNode.id, "", messageText, chatId, false);
            Log.i("BROADCAST", p.body);
            api.sendMessage(p.message());

            Notification repliedNotification = new NotificationCompat.Builder(context, App.MESSAGE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentText("Reply Sent")
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(api);
            notificationManager.notify(notifId, repliedNotification);
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
