package com.breeze.application;

import android.util.Log;

import com.breeze.packets.BrzPacket;

import java.util.HashMap;

public final class BreezePacketBufferModule extends BreezeModule {

        private volatile HashMap<String, BrzPacket> packetBuffer;
        private final static String TAG = "BreezePacketBufferModule";

        public BreezePacketBufferModule(BreezeAPI api){
            super(api);
            this.packetBuffer = new HashMap<>();
        }

        public final void addPacket(BrzPacket p){
            this.packetBuffer.put(p.to, p);
            Log.i(TAG, "BrzPacket added to packet buffer with key: " + p.to);
        }

        public final void removePacket(String endpointId){
            this.packetBuffer.remove(endpointId);
            Log.i(TAG, "BrzPacket removed from packet buffer with key: " + endpointId);
        }

        public final boolean checkBuffer(){
            for(BrzPacket p : this.packetBuffer.values()){
                if(this.api.router.checkIfEndpointExists(p.to)){
                    return true;
                }
            }
            return false;
        }

        public final void sendPackets(){
            for(BrzPacket p : this.packetBuffer.values()) {
                if (this.api.router.checkIfEndpointExists(p.to)) {
                    Log.i(TAG, "BrzPacket sent and removed from packet buffer with key: " + p.to);
                    this.api.router.send(p);
                    this.packetBuffer.remove(p.to);
                }
            }
        }
}
