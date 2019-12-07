package com.breeze.views.Messages;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.breeze.R;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;

import java.util.ArrayList;
import java.util.List;

public class MessageList extends RecyclerView.Adapter<MessageList.MessageHolder> {

    public static class MessageHolder extends RecyclerView.ViewHolder {
        View v;
        int position = 0;

        public MessageHolder(View v) {
            super(v);
            this.v = v;
        }

        public void bind(BrzMessage msg, int position) {

            TextView body = this.v.findViewById(R.id.messageBody);
            body.setText(msg.body);

            body.setTextSize(15.0f);

            this.position = position;
        }
    }

    private final int TYPE_STATUS = 0;
    private final int TYPE_OUTGOING = 1;
    private final int TYPE_INCOMMING = 2;

    private List<BrzMessage> messages = new ArrayList<>();
    private Context ctx;

    MessageList(Context ctx, String chatId) {
        this.ctx = ctx;

        BrzStateStore.getStore().getMessages(chatId, messages -> {
            if(messages != null) {
                this.messages = messages;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        BrzMessage m = this.messages.get(position);
        if(m == null) return super.getItemViewType(position);

        if(m.isStatus) return TYPE_STATUS;
        else if(m.from.equals(BrzStateStore.getStore().getHostNode().id)) return TYPE_OUTGOING;
        return TYPE_INCOMMING;
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Get inflater
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate a new message component
        View messageView = null;

        if(viewType == TYPE_STATUS) messageView = inflater.inflate(R.layout.li_message_status, parent, false);
        else if(viewType == TYPE_OUTGOING) messageView = inflater.inflate(R.layout.li_message_outgoing, parent, false);
        else messageView = inflater.inflate(R.layout.li_message, parent, false);

        // Make our holder
        return new MessageHolder(messageView);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
        holder.bind(this.messages.get(position), position);
    }

    @Override
    public int getItemCount() {
        return this.messages.size();
    }
}

