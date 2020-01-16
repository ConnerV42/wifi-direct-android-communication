package com.breeze.views.Messages;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.graph.BrzGraph;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;

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
            if(body != null){
                body.setText(msg.body);
            }


            /* date formatter in local timezone */
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");
            sdf.setTimeZone(TimeZone.getDefault());
            String time = sdf.format(new Date(msg.datestamp));

            TextView datestamp = this.v.findViewById(R.id.messageDatestamp);
            if(datestamp != null) {
                datestamp.setText(time);
            }
            if (viewType == 1) {
                ImageView status = this.v.findViewById(R.id.messageStatus);

                BrzStorage stor = BrzStorage.getInstance();
                BreezeAPI api = BreezeAPI.getInstance();
                int statusIcon = R.drawable.ic_alarm_black_24dp;
                status.setColorFilter(Color.parseColor("#555555"));

                if (api.db.isRead(msg.id)) {
                    statusIcon = R.drawable.ic_done_all_black_24dp;
                    status.setColorFilter(R.color.colorAccent);
                } else if (api.db.isDelivered(msg.id)) {
                    statusIcon = R.drawable.ic_done_black_24dp;
                }

                status.setImageBitmap(stor.bitmapFromVector(ctx, statusIcon));
            }


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

            if (viewType == 5) {
                ImageView messageImage = this.v.findViewById(R.id.messageImage);
                BreezeAPI api = BreezeAPI.getInstance();
                messageImage.setImageBitmap(api.storage.getMessageFileAsBitmap(msg));

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

            if (viewType == 6) {
                // inflate VideoView
                TextView name = this.v.findViewById(R.id.messageName);
                VideoView video = this.v.findViewById(R.id.messageVideo);
                BreezeAPI api = BreezeAPI.getInstance();

                BrzNode n = BrzGraph.getInstance().getVertex(msg.from);
                if (n == null)
                    name.setText("");
                else
                    name.setText(n.name);

                Uri uri = Uri.fromFile(new File(BrzStorage.getInstance().getMessageFileLocation(msg)));
                video.setVideoURI(uri);

                // add MediaController to VideoView
                MediaController mediaController = new MediaController(api.getBaseContext());
                mediaController.setAnchorView(video);
                video.setMediaController(mediaController);

                // Set video thumbnail to 1 millisecond into video
                try {
                    video.seekTo(1);
                }
                catch (Exception e) {
                    Log.i("Video", "Unable to access video content to set seekTo");
                }

            }

            this.position = position;
        }
    }

    private final int TYPE_STATUS = 0;
    private final int TYPE_OUTGOING = 1;
    private final int TYPE_INCOMMING = 2;
    private final int TYPE_GROUP = 4;
    private final int TYPE_IMAGE = 5;
    private final int TYPE_VIDEO = 6;

    private List<BrzMessage> messages = new ArrayList<>();
    private Context ctx;
    private BrzChat chat;

    private Consumer<List<BrzMessage>> messageListener;
    private Consumer receiptListener;

    MessageList(Context ctx, BrzChat chat) {
        this.ctx = ctx;
        this.chat = chat;

        BreezeAPI api = BreezeAPI.getInstance();
        this.messageListener = messages -> {
            if (messages != null) {
                this.messages = messages;
                notifyDataSetChanged();

                for (BrzMessage m : this.messages) {
                    // Message has not been read by the user yet, send receipt
                    if (!m.from.equals(api.hostNode.id) && !api.db.isRead(m.id)) {
                        Log.i("STATE", "Message " + m.body + " from " + m.from + " wasn't read yet!");
                        try {
                            api.meta.sendReadReceipt(m);
                        }catch(Exception e){
                            Log.i("STATE", "Cannot send read receipt to " + m.from);
                        }
                    }
                }
            }
        };

        this.messageListener.accept(api.state.getMessages(chat.id));
        api.state.on("messages" + chat.id, this.messageListener);

        this.receiptListener = msgId -> {
            notifyDataSetChanged();
            Log.i("BLAH", "A receipt changed!");
        };

        api.meta.on("delivered", receiptListener);
        api.meta.on("read", receiptListener);
    }

    public void cleanup() {
        BreezeAPI api = BreezeAPI.getInstance();
        api.state.off("messages" + chat.id, this.messageListener);
        api.meta.off("delivered", receiptListener);
        api.meta.off("read", receiptListener);
    }


    @Override
    public int getItemViewType(int position) {
        BreezeAPI api = BreezeAPI.getInstance();
        BrzMessage m = this.messages.get(position);
        if (m == null) return super.getItemViewType(position);

        if (m.isStatus) return TYPE_STATUS;
        else if (api.storage.hasMessageFile(m) && m.body.equals("Image")) return TYPE_IMAGE;
        else if (api.storage.hasMessageFile(m) && m.body.equals("Video")) return TYPE_VIDEO;
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
        else if (viewType == TYPE_IMAGE)
            messageView = inflater.inflate(R.layout.li_message_image, parent, false);
        else if (viewType == TYPE_VIDEO)
            messageView = inflater.inflate(R.layout.li_message_video, parent, false);
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

