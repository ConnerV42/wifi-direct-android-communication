package com.breeze.streams;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import static android.os.Process.THREAD_PRIORITY_AUDIO;
import static android.os.Process.setThreadPriority;

/**
 *
 * Currently, every payload is read in with getBytes(), or assembled into a string, then consumed as a BrzPacket.
 *
 * What I will do is when the BrzLiveAudioProducer is created (device A wants to start a call),
 * device A will create a payload for the BrzLiveAudioProducer and save it for later.
 * When the BrzLiveAudioRequest is sent, the ID of this payload is also sent, encrypted(?) from device A
 * Then, on the device B, when the request is received,
 * a BrzLiveAudioConsumer is created and contains the payload ID from A that's saved in memory, and the endpoint ID
 * as well.
 * From there, on subsequent payload receive, the router will check the map for BrzLiveAudioConsumers
 * with a
 *
 */

public class BrzLiveAudioConsumer {
    private boolean readyForConsume;
    private volatile boolean streaming;
    private String producerEndpointID;
    private long producerPayloadID;
    private InputStream rawAudioInput;
    private AudioTrack speakers;
    private Thread playThread;
    private int intSize;
    private boolean readyToPlay;

    private static final int RECORDING_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDING_RATE, CHANNEL, FORMAT);

    public BrzLiveAudioConsumer(String endpointId, long payloadId, InputStream is){
        this.producerEndpointID = endpointId;
        this.producerPayloadID = payloadId;
        this.intSize = android.media.AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO , AudioFormat.ENCODING_PCM_16BIT);
        this.speakers = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM);
        this.rawAudioInput = is;
    }
    public boolean isStreaming() {
        return streaming;
    }
    public void playToSpeakers(){
        this.streaming = true;
        this.playThread = new Thread(){
            @Override
            public void run(){
                setThreadPriority(THREAD_PRIORITY_AUDIO);
                byte[] buffer = new byte[BUFFER_SIZE];
                speakers.play();
                int len;
                try {
                    while(isStreaming() && (len = rawAudioInput.read(buffer, 0, buffer.length)) > 0 ){
                        speakers.write(buffer, 0, len);
                    }
                    stopInternal();
                    speakers.release();
                } catch(IOException e){
                    Log.e("BrzAudioConsumer", "Pipe for speakers closed");
                } finally {
                    stopInternal();
                    speakers.release();
                }

            }
        };
        this.playThread.start();
    }
    public void stopInternal(){
        this.streaming = false;
        try {
            this.rawAudioInput.close();
        } catch(IOException e){
            Log.e("BRZLIVEAUDIOCONSUMER", "Failed to close input stream", e);

        }
    }
    public void stop() {
        stopInternal();
        try {
            this.playThread.join();
        } catch (InterruptedException e) {
            Log.e("BRZLIVEAUDIOCONSUMER", "Interrupted while joining AudioRecorder thread", e);
            Thread.currentThread().interrupt();
        }
    }
    public String getProducerEndpointId() {
        return producerEndpointID;
    }

    public void setProducerEndpointId(String endpointId) {
        this.producerEndpointID = endpointId;
    }

    public boolean isReadyForConsume() {
        return readyForConsume;
    }

    public void setReadyToPlay(boolean readyToPlay){
        if(!this.readyForConsume || this.rawAudioInput == null){
            Log.e("BRZLIVEAUDIOCONSUMER", "Cannot mark audio stream consumer as ready to play if not ready to consume or input stream is null");
            this.readyToPlay = false;
            return;
        }
        this.readyToPlay = true;
    }

    public boolean isReadyToPlay(){
        return this.readyToPlay;
    }

    public void setReadyForConsume(boolean readyForConsume) {
        this.readyForConsume = readyForConsume;
    }
    public void setRawAudioInput(InputStream rawAudioInput) {
        this.rawAudioInput = rawAudioInput;
    }

    public long getProducerPayloadID() {
        return producerPayloadID;
    }

    public void setProducerPayloadID(long producerPayloadID) {
        this.producerPayloadID = producerPayloadID;
    }
}
