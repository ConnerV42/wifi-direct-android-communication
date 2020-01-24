package com.breeze.streams;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import com.google.android.gms.nearby.connection.Payload;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static android.os.Process.THREAD_PRIORITY_AUDIO;
import static android.os.Process.setThreadPriority;

public class BrzLiveAudioProducer {

    private boolean readyToProduce;
    private AudioRecord recorder;
    private String producerEndpointID;
    private PipedInputStream payloadStream = new PipedInputStream(BUFFER_SIZE);
    private PipedOutputStream micOut;
    private Thread recordMic;
    private volatile boolean recording;
    private boolean payloadConsumingStream = false;
    private Payload micAudioPayload;

    private static final int RECORDING_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);

    public BrzLiveAudioProducer() {
        try {
            this.micOut = new PipedOutputStream(payloadStream);
            this.micAudioPayload = Payload.fromStream(payloadStream);
            this.payloadConsumingStream = true;
        }catch (Exception e){
            this.micOut = null;
        }
    }

    public Payload getPayload(){
        if(!payloadConsumingStream || this.micOut == null || this.payloadStream == null || this.micAudioPayload == null){
            Log.e("BRZLIVEAUDIOPRODUCER", "Payload not consuming stream from mic");
            return null;
        }
        else{
            return this.micAudioPayload;
        }
    }

    public long getPayloadId(){
        if(!payloadConsumingStream || this.micOut == null || this.payloadStream == null || this.micAudioPayload == null){
            Log.e("BRZLIVEAUDIOPRODUCER", "Payload not consuming stream from mic");
            return -1;
        }
        else{
            return this.micAudioPayload.getId();
        }
    }
    public PipedInputStream getPayloadStream() {
        return payloadStream;
    }

    public boolean isRecording(){
        return recording;
    }
    public void startRecording(){
        if(isRecording()) {
            Log.i("BRZLOVEAUDIOPRODUCER", "Already recording");
            return;
        }
        this.recording = true;
        this.recordMic = new Thread(){
            @Override
            public void run(){
                setThreadPriority(THREAD_PRIORITY_AUDIO);
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);
                if(recorder.getState() != AudioRecord.STATE_INITIALIZED){
                    Log.i("BRZLOVEAUDIOPRODUCER", "Failed to record");
                    recording = false;
                    return;
                }
                recorder.startRecording();
                int len;
                byte[] buffer = new byte[BUFFER_SIZE];
                try{
                    while(isRecording() && (len = recorder.read(buffer, 0, buffer.length)) > 0){
                        micOut.write(buffer, 0, buffer.length);
                    }
                }catch(IOException e){
                    Log.e("BrzAudioConsumer", "Exception with playing stream", e);
                } finally {
                    stopInternal();
                    recorder.release();
                }
            }
        };
        this.recordMic.start();
    }
    public void stopInternal(){
        this.recording = false;
        try {
            this.payloadStream.close();
            this.micOut.close();
            this.readyToProduce = false;
        } catch(IOException e){
            Log.e("BRZLIVEAUDIOCONSUMER", "Failed to close input stream", e);

        }
    }
    public void stopRecording() {
        stopInternal();
        try {
            this.recordMic.join();
        } catch (InterruptedException e) {
            Log.e("BRZLIVEAUDIOCONSUMER", "Interrupted while joining AudioRecorder thread", e);
            Thread.currentThread().interrupt();
        }
    }
    public String getProducerEndpointID() {
        return producerEndpointID;
    }

    public void setProducerEndpointID(String producerEndpointID) {
        this.producerEndpointID = producerEndpointID;
    }
}
