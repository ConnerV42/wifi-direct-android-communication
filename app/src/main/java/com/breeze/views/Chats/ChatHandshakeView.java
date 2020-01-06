package com.breeze.views.Chats;

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
        import android.util.Log;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.ImageButton;
        import android.widget.ImageView;
        import android.widget.TextView;

        import com.breeze.R;
        import com.breeze.application.BreezeAPI;
        import com.breeze.datatypes.BrzChat;
        import com.breeze.packets.BrzPacket;
        import com.breeze.packets.BrzPacketBuilder;
        import com.breeze.router.BrzRouter;
        import com.breeze.state.BrzStateStore;
        import com.breeze.storage.BrzStorage;
        import com.breeze.views.Messages.MessageList;
        import com.breeze.views.Messages.MessagesView;
        import com.google.android.gms.nearby.connection.Payload;
        import com.breeze.views.ChatSettingsActivity;

public class ChatHandshakeView extends AppCompatActivity {
    private BrzChat chat;

    public static Intent getIntent(Context ctx, String chatId) {
        Intent i = new Intent(ctx, ChatHandshakeView.class);
        i.putExtra("ARG_CHAT_ID", chatId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_handshake_view);

        // Get the chat from the argument
        Intent i = getIntent();
        String chatId = i.getStringExtra("ARG_CHAT_ID");

        BreezeAPI api = BreezeAPI.getInstance();
        this.chat = api.state.getChat(chatId);

        TextView chatName = findViewById(R.id.handshake_chat_name);
        chatName.setText(chat.name);

        ImageView chatImage = findViewById(R.id.profile_image);
        chatImage.setImageBitmap(api.storage.getChatImage(this.chat.id, this));

        Button acceptChat = findViewById(R.id.chat_handshake_accept);
        acceptChat.setOnClickListener(view1 -> { // send message
            api.acceptHandshake(this.chat.id);
            finish();
        });

        Button rejectChat = findViewById(R.id.chat_handshake_reject);
        rejectChat.setOnClickListener(view1 -> { // send message
            api.rejectHandshake(this.chat.id);
            finish();
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        ab.setTitle("Chat Invitation");
//        ab.setDisplayHomeAsUpEnabled(true);
//        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
    }
}
