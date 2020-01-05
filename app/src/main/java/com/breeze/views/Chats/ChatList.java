package com.breeze.views.Chats;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatList extends BaseAdapter {

    private class ChatComponent {
        ImageView chatImage;
        TextView chatName;
        TextView numberUnread;
    }

    private Context ctx;
    private List<BrzChat> chats = new ArrayList<>();
    private Consumer<List<BrzChat>> chatListener;

    public ChatList(Context ctx) {
        this.ctx = ctx;
        BreezeAPI api = BreezeAPI.getInstance();
        this.chatListener = brzChats -> {
            if (brzChats != null) {
                this.chats = brzChats;
                notifyDataSetChanged();
            }
        };

        this.chatListener.accept(api.state.getAllChats());
        api.state.on("allChats", this.chatListener);
    }

    public void cleanup() {
        BreezeAPI api = BreezeAPI.getInstance();
        api.state.off("allChats", this.chatListener);
    }

    @Override
    public int getCount() {
        return chats.size();
    }

    @Override
    public Object getItem(int i) {
        return chats.get(i);
    }

    public String getChatId(int i) {
        return chats.get(i).id;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        BrzChat chat = chats.get(i);
        LayoutInflater chatInflater = (LayoutInflater) ctx.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        ChatComponent chatCmp = new ChatComponent();

        convertView = chatInflater.inflate(R.layout.li_chat, null);
        convertView.setTag(chatCmp);

        chatCmp.chatName = convertView.findViewById(R.id.chat_name);
        chatCmp.chatName.setText(chat.name);

        chatCmp.chatImage = convertView.findViewById(R.id.chat_image);

        if (!chat.isGroup) {
            chatCmp.chatImage.setImageBitmap(BrzStorage.getInstance().getProfileImage(chat.otherPersonId(), ctx));
        } else {
            chatCmp.chatImage.setImageBitmap(BrzStorage.getInstance().getChatImage(chat.id, ctx));
        }

//
        return convertView;
    }
}
