package com.breeze.views;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breeze.R;
import com.breeze.packets.BrzMessage;
import com.breeze.state.BrzStateObserver;
import com.breeze.state.BrzStateStore;

import java.util.ArrayList;

public class MessageList extends BaseAdapter implements BrzStateObserver {

    private ArrayList<BrzMessage> messages = new ArrayList<>();
    private Context ctx;

    public MessageList(Context ctx, String chatId) {
        this.ctx = ctx;

        BrzStateStore store = BrzStateStore.getStore();
        store.getMessages(this, chatId);
    }

    @Override
    public void stateChange(Object messages) {
        if(messages != null) {
            this.messages = (ArrayList) messages;
            notifyDataSetChanged();
        }
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
        BrzMessage message = messages.get(i);
        LayoutInflater messageInflater = (LayoutInflater) ctx.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if(message.isStatus){
            StatusComponent msgCmp = new StatusComponent();

            convertView = messageInflater.inflate(R.layout.li_message_status, null);
            convertView.setTag(msgCmp);

            msgCmp.statusBody = (TextView) convertView.findViewById(R.id.statusBody);
            msgCmp.statusBody.setText(message.message);

        } else if( message.userName.equals("You")) {
            OutgoingMessageComponent msgCmp = new OutgoingMessageComponent();

            convertView = messageInflater.inflate(R.layout.li_message_outgoing, null);
            convertView.setTag(msgCmp);

            msgCmp.messageBody = (TextView) convertView.findViewById(R.id.messageBody);
            msgCmp.messageBody.setText(message.message);
        } else {
            MessageComponent msgCmp = new MessageComponent();

            convertView = messageInflater.inflate(R.layout.li_message, null);
            convertView.setTag(msgCmp);

            msgCmp.messageBody = (TextView) convertView.findViewById(R.id.messageBody);
            msgCmp.messageName = (TextView) convertView.findViewById(R.id.messageName);
            msgCmp.messageName.setText(message.userName);
            msgCmp.messageBody.setText(message.message);
        }


        return convertView;
    }
}

class MessageComponent {
    public TextView messageName;
    public TextView messageBody;
}

class OutgoingMessageComponent {
    public TextView messageBody;
}

class StatusComponent {
    public TextView statusBody;
}
