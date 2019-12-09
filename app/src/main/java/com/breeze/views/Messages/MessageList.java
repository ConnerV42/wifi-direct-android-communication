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
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.graph.BrzGraph;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;

import java.util.ArrayList;
import java.util.List;

public class MessageList extends RecyclerView.Adapter<MessageList.MessageHolder> {

    static class MessageHolder extends RecyclerView.ViewHolder {
        View v;
        int position = 0;
        int viewType = 0;

        MessageHolder(View v, int viewType) {
            super(v);
            this.v = v;
            this.viewType = viewType;
        }

        void bind(BrzMessage msg, Context ctx, int position) {
            TextView body = this.v.findViewById(R.id.messageBody);
            body.setText(msg.body);

            if (viewType == 4) {
                TextView name = this.v.findViewById(R.id.messageName);
                ImageView image = this.v.findViewById(R.id.li_message_image);

                BrzNode n = BrzGraph.getInstance().getVertex(msg.from);
                if (n == null) {
                    name.setText("");
                    image.setImageBitmap(BrzStorage.getInstance().getProfileImage("", ctx));
                } else {
                    name.setText(n.name);
                    image.setImageBitmap(BrzStorage.getInstance().getProfileImage(n.id, ctx));
                }
            }


            this.position = position;
        }
    }

    private final int TYPE_STATUS = 0;
    private final int TYPE_OUTGOING = 1;
    private final int TYPE_INCOMMING = 2;
    private final int TYPE_GROUP = 4;

    private List<BrzMessage> messages = new ArrayList<>();
    private Context ctx;
    private BrzChat chat;

    MessageList(Context ctx, BrzChat chat) {
        this.ctx = ctx;
        this.chat = chat;

        BrzStateStore.getStore().getMessages(chat.id, messages -> {
            if (messages != null) {
                this.messages = messages;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        BrzMessage m = this.messages.get(position);
        if (m == null) return super.getItemViewType(position);

        if (m.isStatus) return TYPE_STATUS;
        else if (m.from.equals(BrzStateStore.getStore().getHostNode().id)) return TYPE_OUTGOING;
        else if (this.chat.isGroup) return TYPE_GROUP;
        return TYPE_INCOMMING;
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Get inflater
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate a new message component
        View messageView = null;

        if (viewType == TYPE_STATUS)
            messageView = inflater.inflate(R.layout.li_message_status, parent, false);
        else if (viewType == TYPE_OUTGOING)
            messageView = inflater.inflate(R.layout.li_message_outgoing, parent, false);
        else if (viewType == TYPE_GROUP)
            messageView = inflater.inflate(R.layout.li_message_group, parent, false);
        else if (viewType == TYPE_INCOMMING)
            messageView = inflater.inflate(R.layout.li_message, parent, false);

        // Make our holder
        return new MessageHolder(messageView, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
        holder.bind(this.messages.get(position), this.ctx, position);
    }

    @Override
    public int getItemCount() {
        return this.messages.size();
    }
}

