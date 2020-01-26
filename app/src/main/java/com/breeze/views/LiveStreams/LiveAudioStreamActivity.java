package com.breeze.views.LiveStreams;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.breeze.R;
import com.breeze.streams.BrzLiveAudioConsumer;
import com.breeze.streams.BrzLiveAudioProducer;
import com.google.android.gms.nearby.connection.Payload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Scanner;

public class LiveAudioStreamActivity extends AppCompatActivity { // the audio recording options
    private static String TAG = "AudioClient";
    private static final int RECORDING_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDING_RATE, CHANNEL, FORMAT);
    private BrzLiveAudioProducer microphone;
    private BrzLiveAudioConsumer speakers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_testing);

        Button startMicStream2 = findViewById(R.id.startMicStreamV2);
        Button stopMicStream2 = findViewById(R.id.stopMicStreamV2);

        startMicStream2.setOnClickListener((view) -> {
            if(this.microphone != null && this.microphone.isRecording()){
                Log.e("LIVEAUDIOSTREAMACTIVITY", "microphone already running");
                Toast.makeText(this, "microphone already running", Toast.LENGTH_SHORT).show();
                return;
            }
            if(this.microphone == null){
                this.microphone = new BrzLiveAudioProducer();
            }
            this.microphone.startRecording();
            Payload p = microphone.getPayload();

            if(p != null) {
                InputStream payloadStream = p.asStream().asInputStream();
                this.speakers = new BrzLiveAudioConsumer("0", p.getId(), payloadStream);
                this.speakers.playToSpeakers();
            }
            else{
                Toast.makeText(this, "Bad payload from audio producer, stopping recording", Toast.LENGTH_SHORT).show();
                this.microphone.stopRecording();
                this.microphone = null;
            }
        });

        startMicStream2.setOnLongClickListener((view) -> {
            Log.e("LIVEAUDIOSTREAMACTIVITY", "microphone being destroyed");
            Toast.makeText(this, "microphone destroyed", Toast.LENGTH_SHORT).show();
            if(this.microphone != null){
                if(this.speakers != null){
                    this.speakers.stop();
                    this.speakers = null;
                }
                this.microphone.stopRecording();
                this.microphone = null;
                return true;
            }
            else {
                this.microphone = null;
                this.speakers = null;
            }
            return true;
        });

        stopMicStream2.setOnClickListener((view) -> {
            if(microphone == null || !microphone.isRecording()){
                Log.e("LIVEAUDIOSTREAMACTIVITY", "microphone not running already");
                Toast.makeText(this, "microphone already  stopped", Toast.LENGTH_SHORT).show();
                return;
            }
            this.microphone.stopRecording();
            this.speakers.stop();
            this.microphone = null;
            this.speakers = null;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stream_testing, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
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
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
