package com.breeze.views.Messages;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

public class MessageList extends RecyclerView.Adapter<MessageList.MessageHolder> {

    static class MessageHolder extends RecyclerView.ViewHolder {
        View v;
        int position = 0;
        int viewType;

        AudioMessage audioController;
        VideoMessage videoController;


        MessageHolder(View v, int viewType) {
            super(v);
            this.v = v;
            this.viewType = viewType;
        }

        void bind(BrzMessage msg, Context ctx, boolean outgoing, boolean group, MessageType type, boolean downloading, int position) {
            BreezeAPI api = BreezeAPI.getInstance();

            if (msg.isStatus) {
                TextView messageBody = this.v.findViewById(R.id.messageBody);
                messageBody.setText(msg.body);
                this.position = position;
                return;
            }

            if (audioController != null) {
                audioController.destroy();
                audioController = null;
            }
            if (videoController != null) {
                videoController.destroy();
                videoController = null;
            }

            // Set the sender's name and image
            TextView messageSenderName = this.v.findViewById(R.id.messageSenderName);
            ImageView messageSenderImage = this.v.findViewById(R.id.messageSenderImage);

            if (messageSenderName != null && messageSenderImage != null) {
                BrzNode n = api.state.getNode(msg.from);
                if (group && !outgoing && n != null) {
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

            // Set timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa", Locale.US);
            sdf.setTimeZone(TimeZone.getDefault());
            String time = sdf.format(new Date(msg.datestamp));
            if (messageDatestamp != null) messageDatestamp.setText(time);

            // Set read reciept status
            if (outgoing && messageStatus != null) {
                messageStatus.setVisibility(View.VISIBLE);
                int statusIcon = R.drawable.ic_alarm_black_24dp;
                if (api.db.isRead(msg.id))
                    statusIcon = R.drawable.ic_done_all_black_24dp;
                else if (api.db.isDelivered(msg.id))
                    statusIcon = R.drawable.ic_done_black_24dp;
                messageStatus.setImageBitmap(api.storage.getVectorAsBitmap(statusIcon));
            } else if (messageStatus != null) {
                messageStatus.setVisibility(View.GONE);
            }

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

            if (downloading) {
                messageFileContainer.setVisibility(View.VISIBLE);
                TextView messageFileName = v.findViewById(R.id.messageFileName);
                messageFileName.setText("Downloading...");

                ProgressBar messageProgressBar = v.findViewById(R.id.messageProgressBar);
                messageProgressBar.setVisibility(View.VISIBLE);

                ImageButton messageFile = v.findViewById(R.id.messageFile);
                messageFile.setVisibility(View.GONE);

                if (outgoing) messageFileContainer.setBackgroundResource(R.drawable.status_bubble);
                else messageFileContainer.setBackgroundResource(R.drawable.message_bubble);
            } else if (type == MessageType.IMAGE) {
                messageImage.setVisibility(View.VISIBLE);
                messageImage.setImageBitmap(api.storage.getMessageFileAsBitmap(msg));
            } else if (type == MessageType.AUDIO) {
                messageMediaControls.setVisibility(View.VISIBLE);
                Uri audioFile = Uri.fromFile(api.storage.getMessageFile(msg));
                audioController = new AudioMessage(
                        messageMediaControls,
                        ctx,
                        audioFile,
                        outgoing ? R.drawable.status_bubble : R.drawable.message_bubble
                );
            } else if (type == MessageType.VIDEO) {
                LinearLayout messageVideoContainer = v.findViewById(R.id.messageVideoContainer);
                messageVideoContainer.setVisibility(View.VISIBLE);

                messageVideo.setVisibility(View.VISIBLE);
                messageMediaControls.setVisibility(View.VISIBLE);

                Uri uri = Uri.fromFile(api.storage.getMessageFile(msg));
                messageVideo.setVideoURI(uri);

                videoController = new VideoMessage(messageMediaControls, messageVideo,
                        outgoing ? R.drawable.message_media_controls_outgoing : R.drawable.message_media_controls
                );
            } else if (type == MessageType.FILE) {
                messageFileContainer.setVisibility(View.VISIBLE);
                TextView messageFileName = v.findViewById(R.id.messageFileName);
                messageFileName.setText(msg.body);

                ProgressBar messageProgressBar = v.findViewById(R.id.messageProgressBar);
                messageProgressBar.setVisibility(View.GONE);

                ImageButton messageFile = v.findViewById(R.id.messageFile);
                messageFile.setVisibility(View.VISIBLE);
                messageFile.setOnClickListener((e) -> {
                    api.openFile(api.storage.getMessageFile(msg), msg.body.replace("File: ", ""));
                });

                if (outgoing) messageFileContainer.setBackgroundResource(R.drawable.status_bubble);
                else messageFileContainer.setBackgroundResource(R.drawable.message_bubble);
            } else {
                messageBody.setVisibility(View.VISIBLE);
                messageBody.setText(msg.body);

                if (outgoing) messageBody.setBackgroundResource(R.drawable.status_bubble);
                else messageBody.setBackgroundResource(R.drawable.message_bubble);
            }

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

    private final int TYPE_STATUS = 0;
    private final int TYPE_NORMAL = 1;

    public enum MessageType {
        STRING, IMAGE, VIDEO, AUDIO, FILE
    }

    private List<BrzMessage> messages = new ArrayList<>();
    private Context ctx;
    private RecyclerView view;
    private BrzChat chat;

    private Consumer<List<BrzMessage>> messageListener;
    private Consumer receiptListener;

    MessageList(Context ctx, RecyclerView view, BrzChat chat) {
        this.ctx = ctx;
        this.view = view;
        this.chat = chat;

        BreezeAPI api = BreezeAPI.getInstance();
        this.messageListener = messages -> {
            if (messages != null) {
                this.messages = messages;
                notifyDataSetChanged();
                view.smoothScrollToPosition(getItemCount());

                for (BrzMessage m : this.messages) {
                    // Message has not been read by the user yet, send receipt
                    if (!m.from.equals(api.hostNode.id) && !api.db.isRead(m.id)) {
                        try {
                            api.meta.sendReadReceipt(m);
                        } catch (Exception e) {
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
        };
        api.meta.on("delivered", receiptListener);
        api.meta.on("read", receiptListener);
        api.storage.on("downloadDone", receiptListener);
    }

    public void cleanup() {
        BreezeAPI api = BreezeAPI.getInstance();
        api.state.off("messages" + chat.id, this.messageListener);
        api.meta.off("delivered", receiptListener);
        api.meta.off("read", receiptListener);
        api.storage.off("downloadDone", receiptListener);
    }


    @Override
    public int getItemViewType(int position) {
        BrzMessage m = this.messages.get(position);
        if (m == null) return super.getItemViewType(position);

        if (m.isStatus) return TYPE_STATUS;
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Get inflater
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate a new message component
        View messageView;
        if (viewType == TYPE_STATUS)
            messageView = inflater.inflate(R.layout.li_message_status, parent, false);
        else {
            messageView = inflater.inflate(R.layout.message_component, parent, false);
        }

        // Make our holder
        MessageList.MessageHolder holder = new MessageList.MessageHolder(messageView, viewType);

        if (viewType == TYPE_NORMAL) {
            // Make a message clickable
            messageView.setOnClickListener(e -> {
                if (this.messageClickListener != null) {
                    BrzMessage message = this.messages.get(holder.position);
                    this.messageClickListener.accept(message);
                }
            });
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
        BreezeAPI api = BreezeAPI.getInstance();
        BrzMessage m = this.messages.get(position);
        BrzChat c = api.state.getChat(m.chatId);
        boolean outgoing = m.from.equals(api.hostNode.id);
        MessageType type = MessageType.STRING;
        if (api.storage.messageFileExists(m) && m.body.equals("Image"))
            type = MessageType.IMAGE;
        else if (api.storage.messageFileExists(m) && m.body.equals("Video"))
            type = MessageType.VIDEO;
        else if (api.storage.messageFileExists(m) && m.body.equals("Audio"))
            type = MessageType.AUDIO;
        else if (api.storage.messageFileExists(m))
            type = MessageType.FILE;

        holder.bind(m, this.ctx, outgoing, c.isGroup, type, api.storage.messageFileIsDownloading(m), position);
    }

    @Override
    public int getItemCount() {
        return this.messages.size();
    }

    private Consumer<BrzMessage> messageClickListener = null;

    public void setMessageClickListener(Consumer<BrzMessage> messageClickListener) {
        this.messageClickListener = messageClickListener;
    }
}

