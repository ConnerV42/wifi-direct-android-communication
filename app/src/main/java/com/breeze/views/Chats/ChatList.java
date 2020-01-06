package com.breeze.views.Chats;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.breeze.MainActivity;
import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;
import com.breeze.views.Messages.MessagesView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ChatList extends BaseAdapter {

    private class ChatComponent {
        ImageView chatImage;
        TextView chatName;
        TextView numberUnread;
        Button deleteButton;
    }

    private Context ctx;
    private List<BrzChat> chats = new ArrayList<>();
    private Consumer<List<BrzChat>> chatListener;
    private boolean showDelete = true;
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

        chatCmp.deleteButton = convertView.findViewById(R.id.delete_button);
        View finalConvertView = convertView;
        chatCmp.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BreezeAPI api = BreezeAPI.getInstance();
                finalConvertView.setVisibility(View.GONE);
                api.db.deleteChat(chat.id);
                api.state.removeChat(chat.id);
                Toast.makeText(ctx, "Chat with id " + chat.id + " has been deleted successfully", Toast.LENGTH_SHORT).show();
            }
        });

        chatCmp.chatName = convertView.findViewById(R.id.chat_name);
        chatCmp.chatName.setText(chat.name);
        chatCmp.chatName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (chatCmp.deleteButton.getVisibility() == View.INVISIBLE) {
                    chatCmp.deleteButton.setVisibility(View.VISIBLE);
                } else {
                    chatCmp.deleteButton.setVisibility(View.INVISIBLE);
                }
                return true;
            }
        });
        chatCmp.chatName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctx.startActivity(MessagesView.getIntent(ctx, chat.id));
            }
        });
        chatCmp.chatImage = convertView.findViewById(R.id.chat_image);

        if (!chat.isGroup) {
            chatCmp.chatImage.setImageBitmap(BrzStorage.getInstance().getProfileImage(chat.otherPersonId(), ctx));
        } else {
            chatCmp.chatImage.setImageBitmap(BrzStorage.getInstance().getChatImage(chat.id, ctx));
        }
        BreezeAPI api = BreezeAPI.getInstance();
        try{
            int count = 0;
            chatCmp.numberUnread = convertView.findViewById(R.id.number_unread_messages);
            Stream<BrzMessage> stream = api.db.getChatMessages(chat.id).stream();
            List<BrzMessage> unread = stream.filter(msg -> api.db.isRead(msg.id)).collect(Collectors.toList());
            if(unread.size() == 0){
                chatCmp.numberUnread.setText("" + 0);
                chatCmp.numberUnread.setVisibility(View.INVISIBLE);
            }
            else {
                chatCmp.numberUnread.setText("" + unread.size());
                chatCmp.numberUnread.setVisibility(View.VISIBLE);
            }
        }catch(Exception e){
            chatCmp.numberUnread.setText("" + 0);
            chatCmp.numberUnread.setVisibility(View.INVISIBLE);
        }


//
        return convertView;
    }
}
