package com.breeze;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.breeze.application.BreezeAPI;

public class KillAppActivity extends AppCompatActivity {

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent calledIntent = getIntent();
        String action = calledIntent.getAction();
        if (action != null && action.equals("KILLER")) {
            BreezeAPI api = BreezeAPI.getInstance();
            api.meta.removeAllNotifications();
            api.stopSelf();
            finishAffinity();
            System.exit(1);
        } else {
            Intent i = new Intent(this, KillAppActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            i.setAction("KILLER");
            startActivity(i);
        }

    }

}
