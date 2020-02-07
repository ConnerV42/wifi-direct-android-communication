package com.breeze.views.PublicMessages;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.breeze.views.Messages.AudioMessage;
import com.breeze.views.Messages.VideoMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

public class PublicMessageList extends RecyclerView.Adapter<PublicMessageList.MessageHolder> {

    static class MessageHolder extends RecyclerView.ViewHolder {
        View v;
        int position = 0;
        int viewType;

        MessageHolder(View v, int viewType) {
            super(v);
            this.v = v;
            this.viewType = viewType;
        }

        void bind(BrzMessage msg, Context ctx, boolean outgoing, int position) {
            BreezeAPI api = BreezeAPI.getInstance();

            if (msg.isStatus) {
                TextView messageBody = this.v.findViewById(R.id.messageBody);
                messageBody.setText(msg.body);
                this.position = position;
                return;
            }

            // Set the sender's name and image
            TextView messageSenderName = this.v.findViewById(R.id.messageSenderName);
            ImageView messageSenderImage = this.v.findViewById(R.id.messageSenderImage);

            if (messageSenderName != null && messageSenderImage != null) {
                BrzNode n = api.getGraph().getVertex(msg.from);
                if (!outgoing && n != null) {
                    messageSenderImage.setImageBitmap(api.storage.getProfileImage(api.storage.PROFILE_DIR, n.id));
                    messageSenderImage.setVisibility(View.VISIBLE);
                    messageSenderName.setText(n.name);
                    messageSenderName.setVisibility(View.VISIBLE);
                } else {
                    messageSenderImage.setVisibility(View.GONE);
                    messageSenderName.setVisibility(View.GONE);
                }
            }

            TextView messageDatestamp = this.v.findViewById(R.id.messageDatestamp);
            ImageView messageStatus = this.v.findViewById(R.id.messageStatus);
            messageStatus.setVisibility(View.GONE);

            // Set timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa", Locale.US);
            sdf.setTimeZone(TimeZone.getDefault());
            String time = sdf.format(new Date(msg.datestamp));
            if (messageDatestamp != null) messageDatestamp.setText(time);

            // Set the message's body
            TextView messageBody = this.v.findViewById(R.id.messageBody);
            ImageView messageImage = this.v.findViewById(R.id.messageImage);
            View messageMediaControls = this.v.findViewById(R.id.messageMediaControls);
            VideoView messageVideo = this.v.findViewById(R.id.messageVideo);
            LinearLayout messageFileContainer = this.v.findViewById(R.id.messageFileContainer);

            messageBody.setVisibility(View.GONE);
            messageImage.setVisibility(View.GONE);
            messageMediaControls.setVisibility(View.GONE);
            messageVideo.setVisibility(View.GONE);
            messageFileContainer.setVisibility(View.GONE);

            messageImage.setImageDrawable(null);
            messageVideo.stopPlayback();

            messageBody.setVisibility(View.VISIBLE);
            messageBody.setText(msg.body);

            if (outgoing) messageBody.setBackgroundResource(R.drawable.status_bubble);
            else messageBody.setBackgroundResource(R.drawable.message_bubble);

            // Set the alignment
            LinearLayout messageLayout = this.v.findViewById(R.id.messageLinearLayout);
            LinearLayout messageInnerLayout = this.v.findViewById(R.id.messageInnerLayout);
            if (outgoing) {
                messageLayout.setGravity(Gravity.END);
                messageInnerLayout.setGravity(Gravity.END);
                messageInnerLayout.setPadding(100, 0, 0, 0);
            } else {
                messageLayout.setGravity(Gravity.START);
                messageInnerLayout.setGravity(Gravity.START);
                messageInnerLayout.setPadding(0, 0, 100, 0);
            }

            this.position = position;
        }
    }


    private List<BrzMessage> messages = new ArrayList<>();
    private Context ctx;

    private Consumer<List<BrzMessage>> messageListener;

    PublicMessageList(Context ctx, RecyclerView view) {
        this.ctx = ctx;

        BreezeAPI api = BreezeAPI.getInstance();
        this.messageListener = messages -> {
            if (messages != null) {
                this.messages = messages;
                notifyDataSetChanged();
                view.smoothScrollToPosition(getItemCount());
            }
        };

        this.messageListener.accept(api.state.getPublicMessages());
        api.state.on("publicMessages", this.messageListener);
    }

    public void cleanup() {
        BreezeAPI api = BreezeAPI.getInstance();
        api.state.off("publicMessages", this.messageListener);
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Get inflater
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate a new message component
        View messageView = inflater.inflate(R.layout.message_component, parent, false);

        // Make our holder
        return new MessageHolder(messageView, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
        BreezeAPI api = BreezeAPI.getInstance();
        BrzMessage m = this.messages.get(position);
        boolean outgoing = m.from.equals(api.hostNode.id);
        holder.bind(m, this.ctx, outgoing, position);
    }

    @Override
    public int getItemCount() {
        return this.messages.size();
    }
}

