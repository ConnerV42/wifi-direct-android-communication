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

    private HashMap<Long, Payload> packetPayloads = new HashMap<>();
    private HashMap<Long, Payload> streamPayloads = new HashMap<>();

    public void addPacketPayload(Payload payload) {
        String packetJSON = BrzPayloadBuffer.getStreamString(payload);
        BrzPacket packet = new BrzPacket(packetJSON);

        if (!packet.hasStream()) throw new RuntimeException("Packet does not have stream info");

        long payloadId = packet.stream.filePayloadId;
        packetPayloads.put(payloadId, payload);
    }

    public Payload getPacketPayload(long payloadId) {
        return this.packetPayloads.get(payloadId);
    }

    public void addStreamPayload(Payload payload) {
        if (!this.isStreamPayload(payload.getId()))
            throw new RuntimeException("There is no packet to go with this stream");

        streamPayloads.put(payload.getId(), payload);
    }

    public Payload getStreamPayload(long payloadId) {
        return this.streamPayloads.get(payloadId);
    }

    public boolean isStreamPayload(long payloadId) {
        return packetPayloads.get(payloadId) != null;
    }

    public void removeStream(long payloadId) {
        this.streamPayloads.remove(payloadId);
        this.packetPayloads.remove(payloadId);
    }

}
