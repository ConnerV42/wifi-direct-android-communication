package com.breeze.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import android.widget.TextView;

import android.os.Bundle;
import android.widget.Toast;
import android.widget.Toolbar;

import com.breeze.MainActivity;
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
    private BrzNode node = BreezeAPI.getInstance().state.getHostNode();

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
        //Toast.makeText(getActivity(), api.hostNode.name, Toast.LENGTH_LONG).show();
//        MainActivity ma = new MainActivity();
//        ImageButton scanButt = ma.findViewById(R.id.scanButton);
//        scanButt.setVisibility(View.INVISIBLE);

        //set image
        ImageView profileImage = getView().findViewById(R.id.profile_image_edit);
        profileImage.setImageBitmap(api.storage.getProfileImage(api.storage.PROFILE_DIR, api.hostNode.id));

        //set username
        TextView userName = getView().findViewById(R.id.user_name);
        if(api.hostNode.name.equals(""))
        {
            String s1 = "[no username entered]";
            userName.setTextColor(Color.RED);
            userName.setText(s1);
        }
        else
        {
            userName.setText(api.hostNode.name);
        }

        //set alias name
        TextView aliasName = getView().findViewById(R.id.alias_name);

        if(api.hostNode.alias.equals("@"))
        {
            String s = "[no alias entered]";
            aliasName.setTextColor(Color.RED);
            aliasName.setText(s);
        }
        else
        {
            aliasName.setText(api.hostNode.alias);
        }
        //set save button
        //Button saveButton = getView().findViewById(R.id.edit_save_button);
        //saveButton.setVisibility (View.INVISIBLE);

        //edit picture dialog pop up and save
        Button editPicture = getView().findViewById(R.id.edit_image_button);

        editPicture.setOnClickListener(e -> {

            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, READ_REQUEST_CODE);

            //saveButton.setVisibility (View.VISIBLE);

        });

        //edit username dialog pop up and save
        Button editUserNameButton = getView().findViewById(R.id.edit_user_button);
        editUserNameButton.setOnClickListener(e ->{

            final EditText txtNewName = new EditText(getContext());
            txtNewName.setMaxWidth(10);

            new AlertDialog.Builder(getContext())
                    .setTitle("Edit Username")
                    .setMessage("Enter new username.")
                    .setView(txtNewName)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String newName = txtNewName.getText().toString();
                            updateNewUsername(newName);
                            userName.setTextColor(Color.BLACK);
                            userName.setText(api.hostNode.name);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();

        });

        //edit alias dialog pop up and save
        Button editAliasButton = getView().findViewById(R.id.edit_alias_button);
        editAliasButton.setOnClickListener(e ->{

            final EditText txtNewAlias = new EditText(getContext());
            txtNewAlias.setMaxWidth(10);

            new AlertDialog.Builder(getContext())
                    .setTitle("Edit Alias")
                    .setMessage("Enter new Alias.")
                    .setView(txtNewAlias)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String newAlias = txtNewAlias.getText().toString();
                            updateNewAlias(newAlias);
                            aliasName.setTextColor(Color.BLACK);
                            aliasName.setText(api.hostNode.alias);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();

        });

        //using save button save changes to the state
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 10;
                Bitmap bitmap = BitmapFactory.decodeStream(getActivity().getApplicationContext().getContentResolver().openInputStream(imageUri), null, options);

                // Set image for ui

                new AlertDialog.Builder(getContext())
                        .setTitle("Edit Photo")
                        .setMessage("Save new photo?")
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ImageView profileImage = getView().findViewById(R.id.profile_image_edit);
                                profileImage.setImageBitmap(bitmap);

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();


                // Set user profileImage
                BreezeAPI api = BreezeAPI.getInstance();
                api.storage.saveProfileImage(api.storage.PROFILE_DIR, node.id, bitmap);

            } catch (Exception e) {
                Log.e("FILE_ACCESS", "Failure ", e);
            }
        }
    }



    public void updateNewUsername(String newUsername)
    {
        node = BreezeAPI.getInstance().state.getHostNode();
        node.name = newUsername;
        BreezeAPI.getInstance().setHostNode(node);
    }

    public void updateNewAlias(String newAlias)
    {
        String newA = "";
        newA += "@" + newAlias;
        node = BreezeAPI.getInstance().state.getHostNode();
        node.alias = newA;
        BreezeAPI.getInstance().setHostNode(node);

    }


    @Override
    public void onStart()
    {
        super.onStart();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;
        ActionBar ab = activity.getSupportActionBar();
        ImageButton ac = activity.findViewById(R.id.scanButton);
        ac.setVisibility(View.INVISIBLE);
        if (ab == null) return;
        ab.setTitle("Edit Profile");




    }



}
