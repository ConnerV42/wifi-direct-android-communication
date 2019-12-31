package com.breeze.application;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.breeze.App;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.views.ProfileActivity;

import java.util.List;

class BreezeMetastateModule extends BreezeModule {
    BreezeMetastateModule(BreezeAPI api) {
        super(api);

        // Get stored hostNode info
        SharedPreferences sp = api.getSharedPreferences("Breeze", Context.MODE_PRIVATE);
        String hostNodeId = sp.getString(App.PREF_HOST_NODE_ID, "");
        BrzNode hostNode = api.db.getNode(hostNodeId);
        if (hostNode != null) {
            api.setHostNode(hostNode);
        } else {
            // Get a new profile since one isn't set
            Intent profileIntent = new Intent(api, ProfileActivity.class);
            profileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            api.startActivity(profileIntent);
        }

        // Get stored chats
        List<BrzChat> chats = null;
        try {
            chats = api.db.getAllChats();
            if (chats != null) {
                Log.i("STATE", "Found " + chats.size() + " chats in the database");
                // for(BrzChat b : chats) {
                // api.db.deleteChat(b.id);
                // api.db.deleteChatMessages(b.id);
                // }
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

    }

}
