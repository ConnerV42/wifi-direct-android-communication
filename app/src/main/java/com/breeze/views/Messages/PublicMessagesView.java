package com.breeze.views.Messages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.MotionEvent;
import android.widget.Switch;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CompoundButton;

import com.breeze.MainActivity;
import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.BrzRouter;
import com.breeze.views.MainSettingsActivity;
import com.breeze.views.ProfileActivity;

import com.breeze.R.layout.*;

import org.w3c.dom.Text;

import java.util.List;
import java.util.function.Consumer;

public class PublicMessagesView extends AppCompatActivity {

    private BrzChat chat;
    private MessageList list;
    private Consumer<BrzChat> onChatUpdate;

    private Consumer<List<BrzMessage>> messageListener;

    private Consumer<List<BrzNode>> endpointListener;

    public static Intent getIntent(Context ctx, String chatId) {
        Intent i = new Intent(ctx, PublicMessagesView.class);
        i.putExtra("ARG_CHAT_ID", chatId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_messages_view);

        // check current state of a Switch (true or false).
        //Boolean switchState = publicSwitch.isChecked();


            BreezeAPI api = BreezeAPI.getInstance();

            // Get the chat from the argument
            Intent i = getIntent();
            String chatId = "PUBLIC_THREAD";
            this.chat = api.state.getChat(chatId);
            Log.i("STATE", this.chat.toJSON());

            if (chat == null) return;

            // Set up content
            final BrzRouter router = BreezeAPI.getInstance().router;
            Switch publicSwitch = findViewById(R.id.PublicSwitch);
            publicSwitch.setChecked(true);
            this.list = new MessageList(this, this.chat);
            RecyclerView msgView = findViewById(R.id.publicMessageList);
            msgView.setAdapter(this.list);

            TextView numberOnline = findViewById(R.id.number_online);
            numberOnline.setText("Online: " + api.router.getEndpointIDs().values().size());

            publicSwitch.setOnTouchListener((View v, MotionEvent e) -> {
                switch(e.getAction()){
                    case MotionEvent.ACTION_UP:
                        if(publicSwitch.isChecked()){
                            publicSwitch.setChecked(false);
                            msgView.setAdapter(null);
                            return true;
                        }
                        else {
                            publicSwitch.setChecked(true);
                            RecyclerView r = findViewById(R.id.publicMessageList);
                            msgView.setAdapter(this.list);
                            return true;
                        }
                }
                return false;
            });

            LinearLayoutManager msgLayout = new LinearLayoutManager(this);
            msgLayout.setStackFromEnd(true);
            msgView.setLayoutManager(msgLayout);


            ImageButton sendMessage = findViewById(R.id.publicSendMessage);
            sendMessage.setOnClickListener(view1 -> { // send message

                EditText messageBox = findViewById(R.id.publicEditText);
                String messageBoxText = messageBox.getText().toString();

                // Reset message box
                messageBox.setText("");
                //Toast.makeText(this.getApplicationContext(), "button clicked", Toast.LENGTH_SHORT).show();

                BrzPacket p = BrzPacketBuilder.publicMessage(router.hostNode.id, "", messageBoxText, false);

                try {
                    BreezeAPI.getInstance().sendPublicMessage(p.message());
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

        if(this.chat != null){
            ab.setTitle("Public Feed");

        }

        RecyclerView msgView = findViewById(R.id.publicMessageList);
        this.messageListener = messages -> {
            if (messages != null) {
                msgView.scrollToPosition(messages.size() - 1);
                Log.i("BLAH", "Scrolling list");
            }
        };
        api.state.on("messages" + this.chat.id, messageListener);

        this.endpointListener = endpoint -> {
            if(endpoint != null){
                TextView numberOnline = findViewById(R.id.number_online);
                numberOnline.setText("Online: " + endpoint);
            }
        };
        api.router.on("endpointAdded", this.endpointListener);
        //-------------------------------------------------------------------------------//

        LinearLayout messageEditor = findViewById(R.id.public_messages_editor);
        TextView messageNotAccepted = findViewById(R.id.public_messages_not_accepted);
        this.onChatUpdate = chat -> {
            if(chat == null) return;
            this.chat = chat;

            messageEditor.setVisibility(View.VISIBLE);
            messageNotAccepted.setVisibility(View.GONE);

        };

        api.state.on("chat" + this.chat.id, this.onChatUpdate);
        this.onChatUpdate.accept(chat);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.chat.isGroup) getMenuInflater().inflate(R.menu.public_thread_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }
        else if(id == R.id.profile_option)
        {
            Intent i = new Intent(PublicMessagesView.this, ProfileActivity.class);
            startActivity(i);
        }
        else if(id == R.id.private_chat_option)
        {
            Intent i = new Intent(PublicMessagesView.this, MainActivity.class);
            startActivity(i);
        }
//        else if(id == R.id.activate_feed_switch) {
//           // pause/start feed
//        }
        else if (id == R.id.action_settings) {
            Intent i = new Intent(PublicMessagesView.this, MainSettingsActivity.class);
            startActivity(i);
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
