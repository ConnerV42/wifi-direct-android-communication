package com.breeze.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.graph.BrzGraph;

public class EditProfileActivity extends Fragment
{
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
