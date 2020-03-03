package com.breeze.views.Messages;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.graph.BrzGraph;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.views.ChatSettingsActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

public class MessagesView extends AppCompatActivity {
    private static final int PHOTO_REQUEST_CODE = 69;
    private static final int VIDEO_REQUEST_CODE = 70;
    private static final int AUDIO_REQUEST_CODE = 71;
    private static final int FILE_REQUEST_CODE = 72;
    private static final int CAMERA_REQUEST_CODE = 73;
    private BrzChat chat;
    private MessageList list;
    private Consumer<BrzChat> onChatUpdate;
    private Consumer<Object> onGraphUpdate;
    private Handler mHandler = new Handler();

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
        RecyclerView msgView = findViewById(R.id.messageList);
        LinearLayoutManager msgLayout = new LinearLayoutManager(this);

        this.list = new MessageList(this, msgView, this.chat);

        this.list.setMessageClickListener((selectedMessage) -> {
            if (selectedMessage.body.equals("Image")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(msgView.getContext());
                builder.setMessage(R.string.saveImage)
                        .setTitle(R.string.saveImageTitle);

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Bitmap bitmap = api.storage.getMessageFileAsBitmap(selectedMessage);
                        ContentResolver cr = getContentResolver();
                        byte[] array = new byte[7];
                        new Random().nextBytes(array);
                        MediaStore.Images.Media.insertImage(cr, bitmap, new String(array, Charset.forName("UTF-8")), "Breeze Image");
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            } else if (selectedMessage.body.equals("Video")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(msgView.getContext());
                builder.setMessage(R.string.saveVideo)
                        .setTitle(R.string.saveVideoTitle);

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Bitmap bitmap = api.storage.getMessageFileAsBitmap(selectedMessage);
                        ContentResolver cr = getContentResolver();
                        byte[] array = new byte[7];
                        new Random().nextBytes(array);
                        MediaStore.Images.Media.insertImage(cr, bitmap, new String(array, Charset.forName("UTF-8")), "Breeze Video");
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

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

        int delayMillis = 333;

        // Bring up the option to select media to send from external storage
        sendPhoto.setOnClickListener(view1 -> {
            mHandler.postDelayed(delayPhotoIntent, delayMillis);
        });

        sendPhoto.setOnLongClickListener(view1 -> {
            mHandler.postDelayed(delayCameraIntent, delayMillis);
            return false;
        });

        sendVideo.setOnClickListener(view1 -> {
            mHandler.postDelayed(delayVideoIntent, delayMillis);
        });

        sendAudio.setOnClickListener(view1 -> {
            mHandler.postDelayed(delayAudioIntent, delayMillis);
        });

        sendFile.setOnClickListener(view1 -> {
            mHandler.postDelayed(delayFileIntent, delayMillis);
        });
        
        // Live audio call
        ImageButton menu_call_button = findViewById(R.id.menu_call_button);
        menu_call_button.setVisibility(chat.isGroup ? View.GONE : View.VISIBLE);
        menu_call_button.setOnClickListener(v -> {
            api.streams.sendLiveAudioRequest(chat.otherPersonId());
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BreezeAPI api = BreezeAPI.getInstance();

        if (resultCode == Activity.RESULT_OK && data != null) {

            String bodyFortype = "";
            if (requestCode == PHOTO_REQUEST_CODE) bodyFortype = "Image";
            else if (requestCode == CAMERA_REQUEST_CODE) bodyFortype = "Image";
            else if (requestCode == VIDEO_REQUEST_CODE) bodyFortype = "Video";
            else if (requestCode == AUDIO_REQUEST_CODE) bodyFortype = "Audio";
            else if (requestCode == FILE_REQUEST_CODE) bodyFortype = "File";
            else return;
            BrzPacket p = BrzPacketBuilder.message(api.hostNode.id, "", bodyFortype, chat.id, false);
            Uri fileUri = null;

            if (requestCode == CAMERA_REQUEST_CODE && data.getExtras() != null) {
                Bitmap bmp = (Bitmap) data.getExtras().get("data");
                File messageFile = api.storage.getMessageFile(p.message());
                api.storage.saveBitmapToFile(messageFile, bmp);
                fileUri = Uri.fromFile(messageFile);
            } else {
                fileUri = data.getData();
                if (fileUri == null) return;
            }

            api.sendFileMessage(p.message(), fileUri);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        BreezeAPI api = BreezeAPI.getInstance();

        Toolbar customToolbar = findViewById(R.id.messagesToolbar);
        customToolbar.getOverflowIcon().setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_ATOP);

        setSupportActionBar(customToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);

        if (this.chat != null) {
            TextView chatName = findViewById(R.id.chat_name);
            chatName.setText(chat.name);

            TextView onlineIndicator = findViewById(R.id.online_indicator);
            TextView offlineIndicator = findViewById(R.id.offline_indicator);

            BrzGraph graph = api.getGraph();
            this.onGraphUpdate = newNode -> {
                if (!chat.isGroup && graph.getVertex(chat.otherPersonId()) != null) {
                    onlineIndicator.setVisibility(View.VISIBLE);
                    offlineIndicator.setVisibility(View.GONE);
                } else if (!chat.isGroup) {
                    offlineIndicator.setVisibility(View.VISIBLE);
                    onlineIndicator.setVisibility(View.GONE);
                } else {
                    offlineIndicator.setVisibility(View.GONE);
                    onlineIndicator.setVisibility(View.GONE);
                }
            };
            // Set up event listeners
            this.onGraphUpdate.accept(null);
            graph.on("addVertex", this.onGraphUpdate);
            graph.on("deleteVertex", this.onGraphUpdate);
            graph.on("setVertex", this.onGraphUpdate);


            // Chat's image
            ImageView chatImage = findViewById(R.id.chat_image);
            String dir = chat.isGroup ? api.storage.CHAT_DIR : api.storage.PROFILE_DIR;
            String chatImageFile = chat.isGroup ? chat.id : chat.otherPersonId();
            chatImage.setImageBitmap(api.storage.getProfileImage(dir, chatImageFile));
        }
        ;

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

        // Remove any notifications that are pending for this chat
        api.meta.removeNotification(this.chat.id);
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
        api.state.off("chat" + this.chat.id, this.onChatUpdate);
        this.list.cleanup();

        // Set up event listeners
        BrzGraph graph = api.getGraph();
        graph.off("addVertex", this.onGraphUpdate);
        graph.off("deleteVertex", this.onGraphUpdate);
        graph.off("setVertex", this.onGraphUpdate);
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

    private Runnable delayPhotoIntent = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PHOTO_REQUEST_CODE);
        }
    };

    private Runnable delayCameraIntent = new Runnable() {
        @Override
        public void run() {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    };

    private Runnable delayVideoIntent = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, VIDEO_REQUEST_CODE);
        }
    };

    private Runnable delayAudioIntent = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, AUDIO_REQUEST_CODE);
        }
    };

    private Runnable delayFileIntent = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("*/*");
            startActivityForResult(intent, FILE_REQUEST_CODE);
        }
    };
}
