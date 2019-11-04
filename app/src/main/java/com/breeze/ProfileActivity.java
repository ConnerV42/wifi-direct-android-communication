package com.breeze;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import com.breeze.packets.BrzUser;

import java.io.File;

public class ProfileActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;

    private BrzUser user = new BrzUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageView profileImage = findViewById(R.id.profile_image);
        profileImage.setOnClickListener(e -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 50;
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
}
