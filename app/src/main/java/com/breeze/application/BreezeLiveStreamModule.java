package com.breeze.application;

import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.breeze.MainActivity;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.LiveAudio.AudioPlayer;
import com.breeze.packets.LiveAudio.AudioRecorder;
import com.breeze.packets.LiveAudio.BrzLiveAudioEvent;
import com.breeze.views.CallActivity;
import com.google.android.gms.nearby.connection.Payload;

import java.io.IOException;
import java.io.InputStream;

public class BreezeLiveStreamModule extends BreezeModule {

    BreezeLiveStreamModule(BreezeAPI api) {
        super(api);
    }

    private AudioRecorder recorder = null;
    private AudioPlayer player = null;
    private ParcelFileDescriptor outputStream = null;

    public void incomingRequest(BrzLiveAudioEvent e) {
        api.startActivity(CallActivity.getIntent(api, e.from, false));
    }

    public void incomingResponse(BrzLiveAudioEvent e) {
        if (e.accepted) {
            sendLiveAudioStream(e.from);
        } else {
            Intent shellIntent = new Intent(api, MainActivity.class);
            shellIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            api.startActivity(shellIntent);

            stopRecording();
            stopPlaying();
        }
    }

    public void sendLiveAudioRequest(String nodeId) {
        BrzPacket packet = new BrzPacket(new BrzLiveAudioEvent());
        packet.type = BrzPacket.BrzPacketType.LIVE_AUDIO_REQUEST;
        packet.to = nodeId;
        packet.broadcast = false;

        api.startActivity(CallActivity.getIntent(api, nodeId, true));

        this.api.router.send(packet);
    }

    public void sendLiveAudioResponse(String nodeId, boolean accepted) {
        BrzPacket packet = new BrzPacket(new BrzLiveAudioEvent(accepted));
        packet.type = BrzPacket.BrzPacketType.LIVE_AUDIO_RESPONSE;
        packet.to = nodeId;
        packet.broadcast = false;

        this.api.router.send(packet);
    }

    public void sendLiveAudioStream(String nodeId) {
        // Create a stream packet for the audio
        BrzPacket p = new BrzPacket(new BrzLiveAudioEvent());
        p.type = BrzPacket.BrzPacketType.LIVE_AUDIO_STREAM;
        p.to = nodeId;
        p.broadcast = false;
        p.addStream(new BrzFileInfo());

        ParcelFileDescriptor recorderStream = startRecording();
        if (recorderStream != null)
            api.router.sendStream(p, Payload.fromStream(recorderStream));
    }

    public void startPlaying(InputStream stream) {
        stopPlaying();
        player = new AudioPlayer(stream);
        player.start();
    }

    public void stopPlaying() {
        if (player != null) {
            player.stop();
        }
        player = null;
    }

    private ParcelFileDescriptor startRecording() {
        stopRecording();

        try {
            Log.w("LIVE", "startRecording()");
            ParcelFileDescriptor[] payloadPipe = ParcelFileDescriptor.createPipe();

            // Use the second half of the payload (the write side) in AudioRecorder.
            recorder = new AudioRecorder(payloadPipe[1]);
            recorder.start();

            // Return the first half of the payload (the read side).
            outputStream = payloadPipe[0];
            return outputStream;
        } catch (IOException e) {
            Log.e("LIVE", "startRecording() failed", e);
        }

        return null;
    }

    public void stopRecording() {
        if (recorder != null && recorder.isRecording()) {
            recorder.stop();
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Exception e) {
            }
        }

        outputStream = null;
        recorder = null;
    }
}
