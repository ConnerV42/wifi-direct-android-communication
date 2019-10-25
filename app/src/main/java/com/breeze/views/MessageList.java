package com.breeze.views;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breeze.R;
import com.breeze.packets.BrzBodyMessage;
import com.breeze.state.BrzStateChangeEvent;
import com.breeze.state.BrzStateObserver;
import com.breeze.state.BrzStateStore;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MessageList extends BaseAdapter implements BrzStateObserver {

    ArrayList<BrzBodyMessage> messages = new ArrayList<>();
    Context ctx;

    public MessageList(Context ctx) {
        this.ctx = ctx;

        BrzStateStore store = BrzStateStore.getStore();
        if(store.getVal("messages/messages") == null)
            store.setVal("messages/messages", new ArrayList<BrzBodyMessage>());

        store.listen(this, "messages/messages");
    }

    @Override
    public void stateChange(BrzStateChangeEvent event) {
        this.messages = (ArrayList) event.value;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {

        MessageViewHolder holder = new MessageViewHolder();

        LayoutInflater messageInflater = (LayoutInflater) ctx.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        BrzBodyMessage message = messages.get(i);

        convertView = messageInflater.inflate(R.layout.message_item, null);
        convertView.setTag(holder);

        holder.messageBody = (TextView) convertView.findViewById(R.id.messageBody);
        holder.messageName = (TextView) convertView.findViewById(R.id.messageName);

        holder.messageName.setText(message.userName);
        holder.messageBody.setText(message.message);

        return convertView;
    }
}

class MessageViewHolder {
    public TextView messageName;
    public TextView messageBody;
}
