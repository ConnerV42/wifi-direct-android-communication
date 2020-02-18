package com.breeze.router;

import com.breeze.packets.BrzPacket;
import com.google.android.gms.nearby.connection.Payload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class BrzStreamBuffer {

    private HashMap<Long, BrzPacket> packetMap = new HashMap<>();
    private HashMap<Long, InputStream> streamMap = new HashMap<>();

    public BrzPacket getPacket(long payloadId) {
        return packetMap.get(payloadId);
    }

    public InputStream getStream(long payloadId) {
        return streamMap.get(payloadId);
    }

    public void addPacket(BrzPacket packet) {
        if (!packet.hasStream()) throw new RuntimeException("Packet does not have stream info");
        long payloadId = packet.stream.filePayloadId;
        packetMap.put(payloadId, packet);
    }

    public void addStream(Payload payload) {
        if (!this.isStreamPayload(payload.getId()))
            throw new RuntimeException("There is no packet to go with this stream");
        streamMap.put(payload.getId(), payload.asStream().asInputStream());
    }

    public boolean isStreamPayload(long payloadId) {
        return packetMap.get(payloadId) != null;
    }

    public void removeStream(long payloadId) {
        streamMap.remove(payloadId);
        packetMap.remove(payloadId);
    }
}
