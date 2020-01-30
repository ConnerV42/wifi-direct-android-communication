package com.breeze.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzNode;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;

public class ChatSettingsActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;
    private BrzChat chat = null;

    public static Intent getIntent(Context ctx, String chatId) {
        Intent i = new Intent(ctx, ChatSettingsActivity.class);
        i.putExtra("ARG_CHAT_ID", chatId);
        return i;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_settings);

        BreezeAPI api = BreezeAPI.getInstance();

        // Snag the chat object from the argument
        Intent i = getIntent();
        String chatId = i.getStringExtra("ARG_CHAT_ID");
        this.chat = api.state.getChat(chatId);

        // Set name textbox to be the chat's current name
        EditText nameTextbox = findViewById(R.id.chat_settings_name);
        nameTextbox.setText(this.chat.name);

        // Get an image when the imageview is clicked
        ImageView chatImage = findViewById(R.id.chat_settings_image);
        chatImage.setImageBitmap(api.storage.getProfileImage(api.storage.CHAT_DIR, this.chat.id));
        chatImage.setOnClickListener(e -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, READ_REQUEST_CODE);
        });

        // Save button
        Button doneButton = findViewById(R.id.chat_settings_done);
        doneButton.setOnClickListener(e -> {
            // Set chat name
            this.chat.name = nameTextbox.getText().toString();
            BreezeAPI.getInstance().updateChat(this.chat);
            finish();
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 10;
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);

                // Set image for ui
                ImageView chatImage = findViewById(R.id.chat_settings_image);
                chatImage.setImageBitmap(bitmap);

                // Set chat image
                BreezeAPI api = BreezeAPI.getInstance();
                api.storage.saveProfileImage(api.storage.CHAT_DIR, this.chat.id, bitmap);

            } catch (Exception e) {
                Log.e("FILE_ACCESS", "Failure ", e);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;

        ab.setTitle("Chat settings");
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}
