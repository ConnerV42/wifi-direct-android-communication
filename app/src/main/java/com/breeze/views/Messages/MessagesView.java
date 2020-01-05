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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
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
    private BrzChat chat;
    private MessageList list;

    private Consumer<List<BrzMessage>> messageListener;

    public static Intent getIntent(Context ctx, String chatId) {
        Intent i = new Intent(ctx, MessagesView.class);
        i.putExtra("ARG_CHAT_ID", chatId);
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

        // Set up content
        final BrzRouter router = BrzRouter.getInstance();

        this.list = new MessageList(this, this.chat);
        RecyclerView msgView = findViewById(R.id.messageList);
        msgView.setAdapter(this.list);


        this.messageListener = messages -> {
            if (messages != null) {
                msgView.scrollToPosition(messages.size() - 1);
                Log.i("BLAH", "Scrolling list");
            }
        };
        api.state.on("messages" + chatId, messageListener);


        LinearLayoutManager msgLayout = new LinearLayoutManager(this);

        msgLayout.setStackFromEnd(true);
        msgView.setLayoutManager(msgLayout);

        Log.i("STATE", "Bound message list to " + this.chat.id);

        ImageButton sendMessage = findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(view1 -> { // send message

            EditText messageBox = findViewById(R.id.editText);
            String messageBoxText = messageBox.getText().toString();

            // Reset message box
            messageBox.setText("");

            BrzPacket p = BrzPacketBuilder.message(router.hostNode.id, "", messageBoxText, chat.id, false);
            BreezeAPI.getInstance().sendMessage(p.message());
        });

        // Bring up the option to select media to send from external storage
        
        ImageButton sendPhoto = findViewById(R.id.sendPhoto);
        sendPhoto.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PHOTO_REQUEST_CODE);
        });

        ImageButton sendVideo = findViewById(R.id.sendVideo);
        sendPhoto.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, VIDEO_REQUEST_CODE);
        });

        ImageButton sendAudio = findViewById(R.id.sendVideo);
        sendPhoto.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, AUDIO_REQUEST_CODE);
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("Send Attachment", "Attachment data: " + data.getAction());

        if (requestCode == PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();

                // For Payload (FILE)
                ParcelFileDescriptor parcel = this.getContentResolver().openFileDescriptor(imageUri, "r");
                Payload filePayload = Payload.fromFile(parcel);
                // Payload filePayloadAsStream = Payload.fromStream(parcel);

                // For File Name Payload (BYTES)
                String filePayloadId = "" + filePayload.getId();
                String fileName = imageUri.getLastPathSegment();

                final BrzRouter router = BrzRouter.getInstance();
                BrzPacket packet = BrzPacketBuilder.fileInfoPacket(router.hostNode.id, chat.id, filePayloadId, fileName);
                BrzRouter.getInstance().sendFilePayload(filePayload, packet);
            } catch (Exception e) {
                Log.e("FILE_ACCESS", "Failure ", e);
            }
        } else if (requestCode == VIDEO_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            // TODO: Send mp4

        } else if (requestCode == AUDIO_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            // TODO: send mp3
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        ab.setTitle(this.chat.name);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
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
    protected void onDestroy() {
        super.onDestroy();
        BreezeAPI.getInstance().state.off("messages" + chat.id, this.messageListener);
        this.list.cleanup();
    }
}
