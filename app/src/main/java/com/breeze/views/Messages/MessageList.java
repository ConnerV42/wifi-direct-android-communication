package com.breeze.views.Messages;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breeze.R;
import com.breeze.datatypes.BrzMessage;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;

import java.util.ArrayList;
import java.util.List;

public class MessageList extends BaseAdapter {

    private class MessageComponent {
        TextView messageBody;
    }

    private class OutgoingMessageComponent {
        TextView messageBody;
    }

    private class StatusComponent {
        TextView statusBody;
    }

    private List<BrzMessage> messages = new ArrayList<>();
    private Context ctx;

    public MessageList(Context ctx, String chatId) {
        this.ctx = ctx;

        BrzStateStore.getStore().getMessages(chatId, messages -> {
            if(messages != null) {
                this.messages = messages;
                notifyDataSetChanged();
            }
        });
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

            msgCmp.statusBody = convertView.findViewById(R.id.statusBody);
            msgCmp.statusBody.setText(message.message);

        } else if( message.from.equals(BrzRouter.getInstance().hostNode.id)) {
            OutgoingMessageComponent msgCmp = new OutgoingMessageComponent();

            convertView = messageInflater.inflate(R.layout.li_message_outgoing, null);
            convertView.setTag(msgCmp);

            msgCmp.messageBody = convertView.findViewById(R.id.messageBody);
            msgCmp.messageBody.setText(message.message);
        } else {
            MessageComponent msgCmp = new MessageComponent();

            convertView = messageInflater.inflate(R.layout.li_message, null);
            convertView.setTag(msgCmp);

            msgCmp.messageBody = convertView.findViewById(R.id.messageBody);
            msgCmp.messageBody.setText(message.message);

            //msgCmp.messageName = convertView.findViewById(R.id.messageName);
            //msgCmp.messageName.setText(message.userName);
        }


        return convertView;
    }
}

