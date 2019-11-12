package com.breeze;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.breeze.packets.BrzUser;
import com.breeze.state.BrzStateStore;

import java.io.File;

import static androidx.navigation.Navigation.findNavController;

public class ProfileActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;

    private BrzUser user = new BrzUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageView profileImage = findViewById(R.id.profile_image);
        profileImage.setOnClickListener(e -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            //intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, READ_REQUEST_CODE);
        });

        Button setProfBtn = findViewById(R.id.profile_set_button);
        setProfBtn.setOnClickListener(e -> {
            EditText profName = findViewById(R.id.profile_name);
            this.user.name = profName.getText().toString();

            EditText profAlias = findViewById(R.id.profile_alias);
            this.user.alias = profAlias.getText().toString();

            BrzStateStore.getStore().setUser(this.user);

            // Navigate back to main activity
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
                user.setProfileImage(bitmap);

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
}
