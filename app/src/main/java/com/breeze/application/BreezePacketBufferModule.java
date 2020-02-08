package com.breeze.application;

import android.util.Log;

import com.breeze.packets.BrzPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public final class BreezePacketBufferModule extends BreezeModule {

        private volatile List<BrzPacket> packetBuffer;
        private final static String TAG = "BreezePacketBufferModule";

        public BreezePacketBufferModule(BreezeAPI api){
            super(api);
            this.packetBuffer = new ArrayList<>();
        }

        public final void addPacket(BrzPacket p){
            if(packetBuffer.contains(p)){
                Log.i(TAG, "Cannot add BrzPacket to packet buffer; already exists in the buffer");
                return;
            }
            this.packetBuffer.add(p);
            Log.i(TAG, "BrzPacket added to packet buffer with key: " + p.to);
        }

        public final void removePacket(BrzPacket p){
            if(!packetBuffer.contains(p)){
                Log.i(TAG, "Cannot remove BrzPacket from packet buffer; doesn't exists in the buffer");
                return;
            }
            this.packetBuffer.remove(p);
        }

        public final List<BrzPacket> getAllPacketsToAnEndpoint(String endpointId){
            return this.packetBuffer.stream().filter(packet -> packet.to.equals(endpointId)).collect(Collectors.toList());
        }

        public final boolean checkEndpointForPackets(String endpointId){
            return this.packetBuffer.stream().filter(packet -> packet.to.equals(endpointId)).collect(Collectors.toList()).size() > 0;
        }

        public final void sendAllToConnectedEndpoints(){
            for(BrzPacket p : this.packetBuffer) {
                if (this.api.router.checkIfEndpointExists(p.to)) {
                    Log.i(TAG, "BrzPacket sent and removed from packet buffer with key: " + p.to);
                    this.api.router.send(p);
                    this.removePacket(p);
                }
            }
        }
}
