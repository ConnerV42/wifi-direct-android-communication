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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.breeze.MainActivity;
import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;
import com.breeze.views.Messages.MessagesView;
import com.breeze.views.UserSelection.UserList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ChatList extends RecyclerView.Adapter<ChatList.ChatHolder> {

    public static class ChatHolder extends RecyclerView.ViewHolder {
        View v;
        int position = 0;

        public ChatHolder(View v) {
            super(v);
            this.v = v;
        }

        public void bind(BrzChat chat, int position, Context ctx) {

            if(chat.id == "PUBLIC_THREAD"){
                return;
            }
            TextView chatName = this.v.findViewById(R.id.chat_name);
            chatName.setText(chat.name);

            // Chat awaiting acceptance
            TextView chatSubText = this.v.findViewById(R.id.chat_sub_text);
            if (!chat.acceptedByHost) {
                chatSubText.setText("You've been invited to join this chat");
                chatSubText.setVisibility(View.VISIBLE);
            } else {
                chatSubText.setText("");
                chatSubText.setVisibility(View.GONE);
            }

            // Chat's image
            ImageView chatImage = this.v.findViewById(R.id.chat_image);
            if (!chat.isGroup)
                chatImage.setImageBitmap(BrzStorage.getInstance().getProfileImage(chat.otherPersonId(), ctx));
            else
                chatImage.setImageBitmap(BrzStorage.getInstance().getChatImage(chat.id, ctx));

            // Unread
            TextView numberUnread = this.v.findViewById(R.id.number_unread_messages);
            int unread = BreezeAPI.getInstance().db.getUnreadCount(chat.id);
            Log.i("STATE", "Unread " + unread);
            if (unread == 0) {
                numberUnread.setVisibility(View.GONE);
            } else {
                numberUnread.setText("" + unread);
                numberUnread.setVisibility(View.VISIBLE);
            }

            // Delete
            Button deleteButton = this.v.findViewById(R.id.delete_button);
            deleteButton.setOnClickListener(v -> {
                BreezeAPI api = BreezeAPI.getInstance();
//                public BrzMessage(String from, String chatId, String body, long datestamp, boolean isStatus) {
//                    this.from = from;
//                    this.chatId = chatId;
//                    this.body = body;
//                    this.datestamp = datestamp;
//                    this.isStatus = isStatus;
//                }
                api.sendMessage(new BrzMessage(api.hostNode.id, chat.id, api.hostNode.alias + " has left the chat", System.currentTimeMillis(), false));
                api.leaveChat(chat.id);
                api.deleteChat(chat.id);
            });

            this.v.setOnLongClickListener(v -> {
                if (deleteButton.getVisibility() == View.GONE)
                    deleteButton.setVisibility(View.VISIBLE);
                else
                    deleteButton.setVisibility(View.GONE);
                return true;
            });

            this.position = position;
        }

    }


    private Context ctx;
    private List<BrzChat> chats = new ArrayList<>();

    private Consumer<List<BrzChat>> chatListener;
    private Consumer<Object> messageListener;

    public ChatList(Context ctx) {
        this.ctx = ctx;
        BreezeAPI api = BreezeAPI.getInstance();
        this.chatListener = brzChats -> {
            if (brzChats != null) {
                this.chats = brzChats;
                notifyDataSetChanged();
            }
        };

        this.messageListener = msgs -> {
            notifyDataSetChanged();
        };

        this.chatListener.accept(api.state.getAllChats());
        api.state.on("allChats", this.chatListener);
        api.state.on("messages", this.messageListener);
    }

    public void cleanup() {
        BreezeAPI api = BreezeAPI.getInstance();
        api.state.off("allChats", this.chatListener);
        api.state.off("messages", this.messageListener);
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Get inflater
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate a new li_chat
        View chat_list_item = inflater.inflate(R.layout.li_chat, parent, false);

        // Make our holder
        ChatHolder holder = new ChatHolder(chat_list_item);

        // Make the item clickable!
        chat_list_item.setOnClickListener(e -> {
            if (this.itemSelectedListener != null)
                this.itemSelectedListener.accept(this.chats.get(holder.position));
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ChatHolder holder, int position) {
        holder.bind(chats.get(position), position, ctx);
    }

    @Override
    public int getItemCount() {
        return this.chats.size();
    }

    private Consumer<BrzChat> itemSelectedListener = null;

    public void setItemSelectedListener(Consumer<BrzChat> itemSelectedListener) {
        this.itemSelectedListener = itemSelectedListener;
    }
}
