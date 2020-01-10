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
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.google.android.gms.nearby.connection.Payload;
import com.breeze.views.ChatSettingsActivity;

import java.util.List;
import java.util.function.Consumer;

public class PublicMessagesView extends AppCompatActivity {

    private BrzChat chat;
    private MessageList list;
    private Consumer<BrzChat> onChatUpdate;

    private Consumer<List<BrzMessage>> messageListener;

    public static Intent getIntent(Context ctx, String chatId) {
        Intent i = new Intent(ctx, PublicMessagesView.class);
        i.putExtra("ARG_CHAT_ID", chatId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.public_activity_messages_view);
        BreezeAPI api = BreezeAPI.getInstance();

        // Get the chat from the argument
        Intent i = getIntent();
        String chatId = i.getStringExtra("ARG_CHAT_ID");
        this.chat = api.state.getChat(chatId);
        Log.i("STATE", this.chat.toJSON());

        if (chat == null) return;

        // Set up content
        final BrzRouter router = BrzRouter.getInstance();

        this.list = new MessageList(this, this.chat);
        RecyclerView msgView = findViewById(R.id.publicMessageList);
        msgView.setAdapter(this.list);
        LinearLayoutManager msgLayout = new LinearLayoutManager(this);
        msgLayout.setStackFromEnd(true);
        msgView.setLayoutManager(msgLayout);

        ImageButton sendMessage = findViewById(R.id.publicSendMessage);
        sendMessage.setOnClickListener(view1 -> { // send message

            EditText messageBox = findViewById(R.id.publicEditText);
            String messageBoxText = messageBox.getText().toString();

            // Reset message box
            messageBox.setText("");

            BrzPacket p = BrzPacketBuilder.message(router.hostNode.id, "", messageBoxText, chat.id, false);
            try {
                BreezeAPI.getInstance().sendMessage(p.message());
            } catch (Exception e) {
                Log.i("MESSAGE_SEND_ERROR", "Cannot send message to " + p.to);
                Toast.makeText(this.getApplicationContext(), "Cannot send message to " + p.to + "; verify they're in the graph", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onStart() {
        super.onStart();
        BreezeAPI api = BreezeAPI.getInstance();

        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);

        if(this.chat != null) ab.setTitle(this.chat.name);

        RecyclerView msgView = findViewById(R.id.publicMessageList);
        this.messageListener = messages -> {
            if (messages != null) {
                msgView.scrollToPosition(messages.size() - 1);
                Log.i("BLAH", "Scrolling list");
            }
        };
        api.state.on("messages" + this.chat.id, messageListener);

        //-------------------------------------------------------------------------------//

        LinearLayout messageEditor = findViewById(R.id.public_messages_editor);
        TextView messageNotAccepted = findViewById(R.id.public_messages_not_accepted);
        this.onChatUpdate = chat -> {
            if(chat == null) return;
            this.chat = chat;
            if(this.chat.acceptedByRecipient) {
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
        BreezeAPI.getInstance().state.off("messages" + chat.id, this.messageListener);
        BreezeAPI.getInstance().state.off("chat" + this.chat.id, this.onChatUpdate);
        this.list.cleanup();
    }
}
