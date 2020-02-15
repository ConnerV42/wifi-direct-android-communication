package com.breeze.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzNode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.breeze.R;

public class CallActivity extends AppCompatActivity {

    public static Intent getIntent(Context ctx, String nodeId, boolean outgoing) {
        Intent i = new Intent(ctx, CallActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("nodeId", nodeId);
        i.putExtra("outgoing", outgoing);
        return i;
    }

    private String nodeId = "";
    private boolean outgoing = false;

    private boolean userLeft = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        BreezeAPI api = BreezeAPI.getInstance();
        nodeId = getIntent().getStringExtra("nodeId");
        outgoing = getIntent().getBooleanExtra("outgoing", false);

        BrzNode n = api.state.getNode(nodeId);

        TextView call_text = findViewById(R.id.call_text);
        if (outgoing) call_text.setText("Calling " + n.name);
        else call_text.setText("Call from " + n.name + "");

        ImageView call_image = findViewById(R.id.call_image);
        call_image.setImageBitmap(api.storage.getProfileImage(api.storage.PROFILE_DIR, nodeId));

        FloatingActionButton call_accept = findViewById(R.id.call_accept);
        FloatingActionButton call_reject = findViewById(R.id.call_reject);
        if (outgoing) call_accept.setVisibility(View.GONE);

        call_accept.setOnClickListener(v -> {
            api.streams.sendLiveAudioResponse(nodeId, true);
            api.streams.sendLiveAudioStream(nodeId);
            call_accept.setVisibility(View.GONE);
        });

        call_reject.setOnClickListener(v -> {
            api.streams.sendLiveAudioResponse(nodeId, false);
            api.streams.stopPlaying();
            api.streams.stopRecording();
            userLeft = true;

            if (proxLock != null) {
                proxLock.release();
                proxLock = null;
            }

            finish();
        });
    }

    PowerManager.WakeLock proxLock;

    @Override
    protected void onResume() {
        super.onResume();

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(Color.parseColor("#000000"));

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            proxLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "brz:prox");
            proxLock.acquire();
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            audioManager.setSpeakerphoneOn(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(getResources().getColor(R.color.colorPrimary));

        if (proxLock != null) {
            proxLock.release();
            proxLock = null;
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }

        if (!userLeft) {
            BreezeAPI api = BreezeAPI.getInstance();
            api.streams.sendLiveAudioResponse(nodeId, false);
            api.streams.stopPlaying();
            api.streams.stopRecording();
        }

    }
}
