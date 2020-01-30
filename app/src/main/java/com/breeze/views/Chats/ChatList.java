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
import com.breeze.graph.BrzGraph;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;
import com.breeze.views.Messages.MessagesView;
import com.breeze.views.UserSelection.UserList;

import java.util.ArrayList;
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
            BrzGraph graph = BrzGraph.getInstance();
            BreezeAPI api = BreezeAPI.getInstance();

            TextView chatName = this.v.findViewById(R.id.chat_name);
            chatName.setText(chat.name);

            TextView onlineIndicator = v.findViewById(R.id.online_indicator);
            TextView offlineIndicator = v.findViewById(R.id.offline_indicator);
            onlineIndicator.setVisibility(View.GONE);
            offlineIndicator.setVisibility(View.GONE);

            if (!chat.isGroup && graph.getVertex(chat.otherPersonId()) != null) {
                onlineIndicator.setVisibility(View.VISIBLE);
            } else if (!chat.isGroup) {
                offlineIndicator.setVisibility(View.VISIBLE);
            }


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
            String dir = chat.isGroup ? api.storage.CHAT_DIR : api.storage.PROFILE_DIR;
            String chatImageFile = chat.isGroup ? chat.id : chat.otherPersonId();
            chatImage.setImageBitmap(api.storage.getProfileImage(dir, chatImageFile));

            // Unread
            TextView numberUnread = this.v.findViewById(R.id.number_unread_messages);
            int unread = api.db.getUnreadCount(chat.id, api.hostNode.id);
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
                BreezeAPI.getInstance().leaveChat(chat.id);
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
    private Consumer<Object> graphListener;

    public ChatList(Context ctx) {
        this.ctx = ctx;
        BreezeAPI api = BreezeAPI.getInstance();
        BrzGraph graph = BrzGraph.getInstance();
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

        this.graphListener = newNode -> {
            notifyDataSetChanged();
        };

        // Set up event listeners
        this.graphListener.accept(null);
        graph.on("addVertex", this.graphListener);
        graph.on("deleteVertex", this.graphListener);
        graph.on("setVertex", this.graphListener);

        api.meta.on("delivered", graphListener);
        api.meta.on("read", graphListener);
    }

    public void cleanup() {
        BreezeAPI api = BreezeAPI.getInstance();
        api.state.off("allChats", this.chatListener);
        api.state.off("messages", this.messageListener);
        api.meta.off("delivered", graphListener);
        api.meta.off("read", graphListener);

        BrzGraph graph = BrzGraph.getInstance();
        graph.off("addVertex", this.graphListener);
        graph.off("deleteVertex", this.graphListener);
        graph.off("setVertex", this.graphListener);
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
