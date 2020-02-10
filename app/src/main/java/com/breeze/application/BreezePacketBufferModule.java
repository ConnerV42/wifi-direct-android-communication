package com.breeze.application;

import android.os.Handler;
import android.util.Log;
import com.breeze.packets.BrzPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class BreezePacketBufferModule extends BreezeModule {

    private final Handler bufferCheckHandler;
    private final int bufferCheckInterval = 10000;
    private volatile List<BrzPacket> packetBuffer;
    private final static String TAG = "BreezePacketBufferModule";
    private final static int MAX_BUFFER_SIZE = 2000;

    private final Runnable bufferCheckTask = new Runnable() {
        @Override
        public void run() {
            sendAllToConnectedEndpoints();
            bufferCheckHandler.postDelayed(bufferCheckTask, bufferCheckInterval);
        }
    };

    private final void startCheckingGraph() {
        bufferCheckTask.run();
    }

    final void stopCheckingGraph() {
        bufferCheckHandler.removeCallbacks(bufferCheckTask);
    }

    public BreezePacketBufferModule(BreezeAPI api) {
        super(api);
        this.packetBuffer = new ArrayList<>();
        this.bufferCheckHandler = new Handler();
        this.startCheckingGraph();
    }

    public final void addPacket(BrzPacket p) {
        if (packetBuffer.contains(p)) {
            Log.e(TAG, "Cannot add BrzPacket to packet buffer; already exists in the buffer");
            return;
        }
        if(packetBuffer.size() >= MAX_BUFFER_SIZE){
            Log.e(TAG, "Cannot add BrzPacket to packet buffer; buffer size exceeded");
            return;
        }
        this.emit("PACKET_BUFFERED", p);
        this.packetBuffer.add(p);
        Log.i(TAG, "BrzPacket added to packet buffer with key: " + p.to);
    }

    public final void removePacket(BrzPacket p) {
        if (!packetBuffer.contains(p)) {
            Log.i(TAG, "Cannot remove BrzPacket from packet buffer; doesn't exists in the buffer");
            return;
        }
        this.emit("PACKET_REMOVED", p);
        this.packetBuffer.remove(p);
    }

    public final List<BrzPacket> getAllPacketsToAnEndpoint(String endpointId) {
        return this.packetBuffer.stream().filter(packet -> packet.to.equals(endpointId)).collect(Collectors.toList());
    }

    public final boolean checkEndpointForPackets(String endpointId) {
        return this.packetBuffer.stream().filter(packet -> packet.to.equals(endpointId)).collect(Collectors.toList()).size() > 0;
    }

    public synchronized final void sendAllToConnectedEndpoints() {
        if(packetBuffer.size() == 0){
            Log.i(TAG, "Buffer Check: Buffer is empty; nothing to send... " +  this.toString());
            return;
        }
        Log.i(TAG, "Buffer Check: Packet buffer checking for endpoints... " + this.toString());
        for (BrzPacket p : this.packetBuffer) {
            if (this.api.router.checkIfEndpointExists(p.to)) {
                Log.i(TAG, "BrzPacket sent and removed from packet buffer with key: " + p.to);
                this.api.router.send(p);
                this.removePacket(p);
            }
        }
    }

    @Override
    public final String toString(){
        return "Packet Buffer | Size: " + this.packetBuffer.size() + " |";
    }
}
