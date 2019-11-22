package com.breeze.views;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzNode;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;

import static androidx.navigation.Navigation.findNavController;

public class ProfileActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;

    private BrzNode node = new BrzNode();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Set our new node's id to something random
        node.generateID();

        // When the image uploader is clicked, choose a profile image
        ImageView profileImage = findViewById(R.id.profile_image);
        profileImage.setOnClickListener(e -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, READ_REQUEST_CODE);
        });

        // When we're done, save our new node
        Button setProfBtn = findViewById(R.id.profile_set_button);
        setProfBtn.setOnClickListener(e -> {
            EditText profName = findViewById(R.id.profile_name);
            this.node.name = profName.getText().toString();

            EditText profAlias = findViewById(R.id.profile_alias);
            this.node.alias = "@" + profAlias.getText().toString();

            // Set our new node and navigate away!
            BreezeAPI.getInstance().setHostNode(this.node);
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
                ImageView profileImage = findViewById(R.id.profile_image);
                profileImage.setImageBitmap(bitmap);

                // Set user profileImage
                BrzStorage.getInstance().saveProfileImage(bitmap, this.node.id);

            } catch (Exception e) {
                Log.e("FILE_ACCESS", "Failure ", e);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportActionBar().setTitle("Setup Profile");
    }

    @Override
    public void onBackPressed() {
        // Don't allow going back
    }
}
