package com.breeze.views;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import android.graphics.Color;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzNode;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzNode;
import com.breeze.graph.BrzGraph;

public class EditProfileActivity extends Fragment
{

    private static final int READ_REQUEST_CODE = 42;
    private BrzNode node = new BrzNode();
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);



    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_edit_profile_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BreezeAPI api = BreezeAPI.getInstance();
        BrzGraph graph = api.getGraph();
        ImageView profileImage = getView().findViewById(R.id.profile_image_edit);


        Button editPicture = getView().findViewById(R.id.edit_image_button);
        //editPicture.setBackgroundColor(Color.RED);
        editPicture.setOnClickListener(e -> {
            profileImage.setImageBitmap(api.storage.getProfileImage(api.storage.PROFILE_DIR, "default"));
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, READ_REQUEST_CODE);
        });


    }

    @Override
    public void onStart()
    {
        super.onStart();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;
        ActionBar ab = activity.getSupportActionBar();
        if (ab == null) return;
        ab.setTitle("Edit Profile");


    }



}
