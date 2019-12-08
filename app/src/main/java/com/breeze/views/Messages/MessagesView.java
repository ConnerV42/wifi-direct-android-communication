package com.breeze.views.Messages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.breeze.views.ChatSettingsActivity;

public class MessagesView extends AppCompatActivity {
    private BrzChat chat;

    public static Intent getIntent(Context ctx, String chatId) {
        Intent i = new Intent(ctx, MessagesView.class);
        i.putExtra("ARG_CHAT_ID", chatId);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages_view);

        // Get the chat from the argument
        Intent i = getIntent();
        String chatId = i.getStringExtra("ARG_CHAT_ID");
        BrzStateStore.getStore().getChat(chatId, chat -> this.chat = chat);

        // Set up content

        final BrzRouter router = BrzRouter.getInstance();

        MessageList msgList = new MessageList(this, this.chat.id);
        RecyclerView msgView = findViewById(R.id.messageList);
        msgView.setAdapter(msgList);

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

            BreezeAPI.getInstance().sendMessage(BrzPacketBuilder.makeMessage(router.hostNode.id, messageBoxText, chat.id, false), chat.id);
        });
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
}
