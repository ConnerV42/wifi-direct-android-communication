package com.breeze.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.breeze.R;
import com.breeze.application.BreezeAPI;

public class MainSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupClickListeners();
    }

    private void setupClickListeners(){
        Button bWipeDB = findViewById(R.id.wipeDatabase);
        Button bRestartService = findViewById(R.id.restartService);
        bWipeDB.setOnClickListener((View v) -> {
            this.showWipeDatabaseDialog();
        });
        bRestartService.setOnClickListener((View v) -> {
           this.showRestartServiceDialog();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void showWipeDatabaseDialog(){
        new AlertDialog.Builder(this)
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

    private void showRestartServiceDialog(){
        new AlertDialog.Builder(this)
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
