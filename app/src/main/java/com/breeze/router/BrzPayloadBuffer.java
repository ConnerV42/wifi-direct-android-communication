package com.breeze.router;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;

import com.google.android.gms.nearby.connection.Payload;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class BrzPayloadBuffer {


    public Payload sendStreamFromMic(){
        int minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 4);

            if(NoiseSuppressor.isAvailable()){
                NoiseSuppressor.create(recorder.getAudioSessionId()).setEnabled(true);
            }
            if(AcousticEchoCanceler.isAvailable()){
                AcousticEchoCanceler.create(recorder.getAudioSessionId()).setEnabled(true);
            }

        byte[] tempBuffer = new byte[minBufferSize];
        recorder.startRecording();
        long currentTime = System.currentTimeMillis();
        Payload p = Payload.fromStream(new ByteArrayInputStream(tempBuffer));
        while(System.currentTimeMillis() < currentTime + 10000){

        }
        return null;
    }

    public Payload getStreamPayload(String str) {
        return Payload.fromStream(new ByteArrayInputStream(str.getBytes()));
    }

    public String getStreamString(Payload p) {
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
        return sb.toString();
    }

    // Broadcast payloads

    private final int MAX_BROADCASTS = 100;
    private List<String> seenBroadcasts = new ArrayList<>();

    public void addBroadcast(String id) {
        if (this.seenBroadcasts.size() > this.MAX_BROADCASTS)
            this.seenBroadcasts.remove(0);
        this.seenBroadcasts.add(id);
    }

    public boolean broadcastSeen(String id) {
        return this.seenBroadcasts.contains(id);
    }

    // Incoming payloads

    private HashMap<Long, Payload> incoming = new HashMap<>();

    public Payload getIncoming(Long payloadId) {
        return this.incoming.get(payloadId);
    }

    public Payload popIncoming(Long payloadId) {
        Payload p = this.incoming.get(payloadId);
        this.removeIncoming(payloadId);
        return p;
    }

    public void addIncoming(Payload payload) {
        this.incoming.put(payload.getId(), payload);
    }

    public void removeIncoming(Long payloadId) {
        this.incoming.remove(payloadId);
    }

    public Boolean isIncomming(Long payloadId) {
        return this.incoming.get(payloadId) != null;
    }

    // Outgoing payloads

    private HashMap<Long, Payload> outgoing = new HashMap<>();

    public Payload getOutgoing(Long payloadId) {
        return this.outgoing.get(payloadId);
    }

    public void addOutgoing(Payload payload, long timeoutDuration, Consumer<Payload> callback) {
        this.outgoing.put(payload.getId(), payload);
        scheduleTimeout(payload, timeoutDuration, 1, callback);
    }

    public void addOutgoing(Payload payload, long timeoutDuration, int retries, Consumer<Payload> callback) {
        this.outgoing.put(payload.getId(), payload);
        scheduleTimeout(payload, timeoutDuration, retries, callback);
    }

    public void removeOutgoing(Long payloadId) {
        this.outgoing.remove(payloadId);
    }

    // Helpers

    private void scheduleTimeout(Payload payload, long timeoutDuration, int retries, Consumer<Payload> callback) {

        // Start by running the callback immediately
        callback.accept(payload);

        // If retries has run out, don't schedule another timeout
        if (retries <= 0) return;
        Timer timeout = new Timer();
        timeout.schedule(new TimerTask() {
            @Override
            public void run() {
                if (outgoing.get(payload.getId()) != null) {
                    callback.accept(payload);
                    scheduleTimeout(payload, timeoutDuration, retries - 1, callback);
                }
            }
        }, timeoutDuration);
    }
}
