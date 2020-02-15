package com.breeze.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.breeze.R;
import com.breeze.application.BreezeAPI;

import static java.security.AccessController.getContext;


public class EditChatActivity extends AppCompatActivity{

    private static final int READ_REQUEST_CODE = 42;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_chat_view);

        //set all things needed buttons etc
        //set current names/images
        //set new name by retrieving through putextra?
        //set new image - look at original chat image how to retrieve

    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 10;
                Bitmap bitmap = BitmapFactory.decodeStream(getApplicationContext().getContentResolver().openInputStream(imageUri), null, options);

                // Set image for ui

//                new AlertDialog.Builder()
//                        .setTitle("Edit Photo")
//                        .setMessage("Save new photo?")
//                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int whichButton) {
//                                ImageView profileImage = findViewById(R.id.profile_image_edit);
//                                profileImage.setImageBitmap(bitmap);
//
//                            }
//                        })
//                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int whichButton) {
//                            }
//                        })
//                        .show();
//
//
//                // Set user profileImage
//                BreezeAPI api = BreezeAPI.getInstance();
//                api.storage.saveProfileImage(api.storage.PROFILE_DIR, node.id, bitmap);

            } catch (Exception e) {
                Log.e("FILE_ACCESS", "Failure ", e);
            }
        }
    }




    @Override
    public void onStart()
    {
        super.onStart();
        getActionBar().setTitle("Edit Chat");
    }


}
