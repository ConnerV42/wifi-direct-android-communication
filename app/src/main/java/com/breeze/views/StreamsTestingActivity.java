package com.breeze.views;

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
import com.breeze.application.BreezeAPI;
import com.google.android.gms.nearby.connection.Payload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Scanner;

public class StreamsTestingActivity extends AppCompatActivity { // the audio recording options
    private static String TAG = "AudioClient";
    private static final int RECORDING_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private AudioTrack recordedTack;
    // the button the user presses to send the audio stream to the server
    private Button sendAudioButton;

    // the audio recorder
    private AudioRecord recorder;

    // the minimum buffer size needed for audio recording
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDING_RATE, CHANNEL, FORMAT);
    private boolean isRecording = false;

    private Toolbar toolbar;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_testing);
        Button startStream = findViewById(R.id.startMicStream);
        Button stopStream = findViewById(R.id.stopMicStream);
        startStream.setOnClickListener((view) -> {
            this.isRecording = true;
            this.startStreaming();
            Log.i("MIC_STREAM", "Started streaming from microphone");
        });
        stopStream.setOnClickListener((view) -> {
            this.stopRecording();
            Log.i("MIC_STREAM", "Stopped streaming from microphone");
        });
    }

    private void startStreaming() {
        Thread streamThread = new Thread(() -> {
                try {

                    Log.d(TAG, "Creating the buffer of size " + BUFFER_SIZE);
                    byte[] buffer = new byte[BUFFER_SIZE];


                    Log.d(TAG, "Creating the AudioRecord");
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);

                    Log.d(TAG, "AudioRecord recording...");
                    recorder.startRecording();
                    InputStream in = new ByteArrayInputStream(buffer);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
//                    File dir = getFilesDir();
//                    File file = new File(dir, "mic_out.pcm");
//                    file.delete();
                    int intSize = android.media.AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO , AudioFormat.ENCODING_PCM_16BIT);
                    AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM);
                    FileOutputStream os = openFileOutput("mic_out.pcm", Context.MODE_PRIVATE);
                    Payload p = Payload.fromStream(in);
                    Log.d(TAG, "Payload created...");
                    while (isRecording == true) {
                        // read the data into the buffer
                        int read = recorder.read(buffer, 0, buffer.length);
                        if (read != -1) {
                            at.play();
                             out.write(buffer, 0, read);
                             os.write(buffer, 0, read);
                             at.write(buffer, 0, read);
                        }
                    }
                    InputStream stream = p.asStream().asInputStream();
                    Scanner sc = new Scanner(stream);
                    StringBuilder sb = new StringBuilder();
                    while (sc.hasNext()) {
                        sb.append(sc.nextLine());
                    }
                    sc.close();
                    try {
                        stream.close();
                    } catch (Exception e) {
                    }

                    Log.d(TAG, sb.toString());
                    recorder.release();
                    Log.d(TAG, "AudioRecord finished recording");
                    os.close();
                    out.close();
                    in.close();
                    at.stop();
                    at.release();

                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e);
                }
        });
        streamThread.start();
    }
    private void stopRecording() {

        Log.i(TAG, "Stopping the audio stream");
        isRecording = false;
       // recorder.release();
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
