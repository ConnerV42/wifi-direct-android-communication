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
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzUser;
import com.breeze.state.BrzStateStore;

import java.util.ArrayList;
import java.util.List;

public class ChatList extends BaseAdapter {

    private class ChatComponent {
        ImageView chatImage;
        TextView chatName;
    }

    private Context ctx;
    private List<BrzChat> chats = new ArrayList<>();

    public ChatList(Context ctx) {
        this.ctx = ctx;

        BrzStateStore.getStore().getAllChats(brzChats -> {
            if (chats != null) {
                this.chats = brzChats;
                notifyDataSetChanged();
            }
        });
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

        BrzUser chatU = BrzStateStore.getStore().getUser(chat.id);
        chatCmp.chatImage = convertView.findViewById(R.id.chat_image);
        chatCmp.chatImage.setImageBitmap(chatU.getProfileImage());

        return convertView;
    }
}
