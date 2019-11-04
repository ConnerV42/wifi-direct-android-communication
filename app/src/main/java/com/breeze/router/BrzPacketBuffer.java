package com.breeze.router;

import com.breeze.packets.BrzPacket;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class BrzPacketBuffer {
    private HashMap<String, BrzPacket> packets = new HashMap<>();

    public void addPacket(BrzPacket packet, long timeoutDuration, int retries, Consumer<BrzPacket> callback) {
        packets.put(packet.id, packet);
        scheduleTimeout(packet, timeoutDuration, retries, callback);
    }

    private void scheduleTimeout(BrzPacket packet, long timeoutDuration, int retries, Consumer<BrzPacket> callback) {
        if(retries <= 0) return;
        Timer timeout = new Timer();
        timeout.schedule(new TimerTask() {
            @Override
            public void run() {
                if (packets.get(packet.id) != null) {
                    callback.accept(packet);
                    scheduleTimeout(packet, timeoutDuration, retries - 1, callback);
                }
            }
        }, timeoutDuration);
    }

    public void removePacket(String packetId) {
        packets.remove(packetId);
    }
}
