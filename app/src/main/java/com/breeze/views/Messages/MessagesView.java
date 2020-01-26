package com.breeze.views.Messages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.google.android.gms.nearby.connection.Payload;
import com.breeze.views.ChatSettingsActivity;

import java.util.List;
import java.util.function.Consumer;

public class MessagesView extends AppCompatActivity {
    private static final int PHOTO_REQUEST_CODE = 69;
    private static final int VIDEO_REQUEST_CODE = 70;
    private static final int AUDIO_REQUEST_CODE = 71;
    private static final int FILE_REQUEST_CODE = 72;
    private BrzChat chat;
    private MessageList list;
    private Consumer<BrzChat> onChatUpdate;

    private Consumer<List<BrzMessage>> messageListener;

    public static Intent getIntent(Context ctx, String chatId) {
        Intent i = new Intent(ctx, MessagesView.class);
        i.putExtra("ARG_CHAT_ID", chatId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages_view);
        BreezeAPI api = BreezeAPI.getInstance();

        // Get the chat from the argument
        Intent i = getIntent();
        String chatId = i.getStringExtra("ARG_CHAT_ID");
        this.chat = api.state.getChat(chatId);
        Log.i("STATE", this.chat.toJSON());

        if (chat == null) return;

        api.state.setCurrentChat(chatId);

        // Set up content
        this.list = new MessageList(this, this.chat);

        RecyclerView msgView = findViewById(R.id.messageList);
        LinearLayoutManager msgLayout = new LinearLayoutManager(this);

        ImageButton sendPhoto = findViewById(R.id.sendPhoto);
        ImageButton sendVideo = findViewById(R.id.sendVideo);
        ImageButton sendAudio = findViewById(R.id.sendAudio);
        ImageButton sendFile = findViewById(R.id.sendFile);

        EditText messageBox = findViewById(R.id.editText);
        ImageButton sendMessage = findViewById(R.id.sendMessage);

        // Set up message list's layout
        msgView.setAdapter(this.list);
        msgLayout.setStackFromEnd(true);
        msgView.setLayoutManager(msgLayout);

        // Set up a listener that shows and hides items depending on user's input
        messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                typingAction(messageBox);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        this.typingAction(messageBox);

        // Set up message sending listener
        sendMessage.setOnClickListener(this::sendStringMessage);

        // Bring up the option to select media to send from external storage
        sendPhoto.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PHOTO_REQUEST_CODE);
        });

        sendVideo.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, VIDEO_REQUEST_CODE);
        });

        sendAudio.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, AUDIO_REQUEST_CODE);
        });

        sendFile.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("*/*");
            startActivityForResult(intent, FILE_REQUEST_CODE);
        });

        ImageButton liveStreamAudio = findViewById(R.id.streamAudio);
        liveStreamAudio.setOnClickListener(v -> {
            if(this.chat.isGroup){
                Toast.makeText(this, "Cannot start a live audio stream with a group yet", Toast.LENGTH_SHORT).show();
            }
            String otherNode = "";
            for(String n : this.chat.nodes){
                if(!n.equals(api.hostNode.id)){
                    otherNode = n;
                    break;
                }
            }
            if(otherNode != "")
               api.streams.sendBrzLiveConnectionRequest(otherNode);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri == null) return;

            BreezeAPI api = BreezeAPI.getInstance();
            String bodyFortype = "";
            if (requestCode == PHOTO_REQUEST_CODE) bodyFortype = "Image";
            else if (requestCode == VIDEO_REQUEST_CODE) bodyFortype = "Video";
            else if (requestCode == AUDIO_REQUEST_CODE) bodyFortype = "Audio";
            else if (requestCode == FILE_REQUEST_CODE) bodyFortype = "File";
            else return;

            BrzPacket p = BrzPacketBuilder.message(api.hostNode.id, "", bodyFortype, chat.id, false);
            api.sendFileMessage(p.message(), fileUri);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        BreezeAPI api = BreezeAPI.getInstance();

        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);

        if (this.chat != null) ab.setTitle(this.chat.name);

        RecyclerView msgView = findViewById(R.id.messageList);
        this.messageListener = messages -> {
            if (messages != null) {
                msgView.scrollToPosition(messages.size() - 1);
                Log.i("BLAH", "Scrolling list");
            }
        };
        api.state.on("messages" + this.chat.id, messageListener);

        //-------------------------------------------------------------------------------//

        LinearLayout messageEditor = findViewById(R.id.messages_editor);
        TextView messageNotAccepted = findViewById(R.id.messages_not_accepted);
        this.onChatUpdate = chat -> {
            if (chat == null) return;
            this.chat = chat;
            if (this.chat.acceptedByRecipient) {
                messageEditor.setVisibility(View.VISIBLE);
                messageNotAccepted.setVisibility(View.GONE);
            } else {
                messageEditor.setVisibility(View.GONE);
                messageNotAccepted.setVisibility(View.VISIBLE);
            }
        };

        api.state.on("chat" + this.chat.id, this.onChatUpdate);
        this.onChatUpdate.accept(chat);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.chat.isGroup) getMenuInflater().inflate(R.menu.menu_messages_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (id == R.id.action_settings) {
            Log.i("STATE", "Settings selected");
            startActivity(ChatSettingsActivity.getIntent(this, this.chat.id));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        BreezeAPI api = BreezeAPI.getInstance();
        api.state.setCurrentChat("");
        api.state.off("messages" + chat.id, this.messageListener);
        api.state.off("chat" + this.chat.id, this.onChatUpdate);
        this.list.cleanup();
    }

    // Helpers
    private void typingAction(View v) {
        if (!(v instanceof EditText)) return;
        EditText messageBox = (EditText) v;

        ImageButton sendPhoto = findViewById(R.id.sendPhoto);
        ImageButton sendVideo = findViewById(R.id.sendVideo);
        ImageButton sendAudio = findViewById(R.id.sendAudio);
        ImageButton sendFile = findViewById(R.id.sendFile);
        ImageButton sendMessage = findViewById(R.id.sendMessage);

        if (!messageBox.getText().toString().isEmpty()) {
            sendPhoto.setVisibility(View.GONE);
            sendVideo.setVisibility(View.GONE);
            sendAudio.setVisibility(View.GONE);
            sendFile.setVisibility(View.GONE);

            sendMessage.setVisibility(View.VISIBLE);
        } else {
            sendPhoto.setVisibility(View.VISIBLE);
            sendVideo.setVisibility(View.VISIBLE);
            sendAudio.setVisibility(View.VISIBLE);
            sendFile.setVisibility(View.VISIBLE);

            sendMessage.setVisibility(View.GONE);
        }
    }

    private void sendStringMessage(View v) {
        BreezeAPI api = BreezeAPI.getInstance();
        EditText messageBox = findViewById(R.id.editText);
        String messageBoxText = messageBox.getText().toString();

        // Reset message box
        messageBox.setText("");

        BrzPacket p = BrzPacketBuilder.message(api.hostNode.id, "", messageBoxText, chat.id, false);
        try {
            BreezeAPI.getInstance().sendMessage(p.message());
        } catch (Exception e) {
            Log.i("MESSAGE_SEND_ERROR", "Cannot send message to " + p.to);
            Toast.makeText(this.getApplicationContext(), "Cannot send message to " + p.to + "; verify they're in the graph", Toast.LENGTH_SHORT).show();
        }

    }
}
