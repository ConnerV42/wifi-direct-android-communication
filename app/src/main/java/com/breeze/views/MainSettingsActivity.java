package com.breeze.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
        Button bWipeDB = findViewById(R.id.wipeDatabase);
        Button bRestartService = findViewById(R.id.restartService);
        bWipeDB.setOnClickListener((View v) -> {
            this.showWipeDatabaseDialog();
        });
        bRestartService.setOnClickListener((View v) -> {
            Toast.makeText(BreezeAPI.getInstance().getApplicationContext(), "Not implemented yet", Toast.LENGTH_SHORT).show();
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
                .setMessage("Are you sure you wipe the database?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        BreezeAPI api = BreezeAPI.getInstance();
                        api.db.onUpgrade(api.db.getWritableDatabase(), 0, 0);
                        Toast.makeText(api.getApplicationContext(), "DB wiped", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
