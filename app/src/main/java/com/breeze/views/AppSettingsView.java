package com.breeze.views;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.breeze.R;
import com.breeze.application.BreezeAPI;

public class AppSettingsView extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button bWipeDB = view.findViewById(R.id.wipeDatabase);
        Button bRestartService = view.findViewById(R.id.restartService);
        bWipeDB.setOnClickListener((View v) -> {
            this.showWipeDatabaseDialog();
        });
        bRestartService.setOnClickListener((View v) -> {
            this.showRestartServiceDialog();
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;
        ActionBar ab = activity.getSupportActionBar();
        if (ab == null) return;
        ab.setTitle("Settings");
        
    }

    private void showWipeDatabaseDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Wipe the Database")
                .setMessage("Are you sure you would like to wipe the database?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        BreezeAPI api = BreezeAPI.getInstance();
                        api.db.onUpgrade(api.db.getWritableDatabase(), 0, 0);
                        Toast.makeText(api.getApplicationContext(), "DB wiped", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void showRestartServiceDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Restart Background Service")
                .setMessage("Are you sure you'd like to restart the background service?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(BreezeAPI.getInstance().getApplicationContext(), "TODO", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}

