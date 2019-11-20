package com.breeze.packets;

import com.breeze.graph.BrzGraph;
import com.breeze.packets.graph.BrzGraphQuery;

import java.security.PublicKey;

public class BrzPacketBuilder {

    public static BrzPacket message(String id, String msgBody) {
        return BrzPacketBuilder.message(id, msgBody);
    }

    public static BrzPacket ack(BrzPacket packet, String to) {
        BrzPacket ackPacket = new BrzPacket();

        ackPacket.type = BrzPacket.BrzPacketType.ACK;
        ackPacket.to = to;
        ackPacket.id = packet.id;

        return ackPacket;
    }

    public static BrzPacket message(String id, String msgTo, String msgBody) {
        BrzMessage body = new BrzMessage();

        body.from = id;
        body.message = msgBody;
        body.userName = "Zach";
        body.datestamp = System.currentTimeMillis();

        BrzPacket packet = new BrzPacket(body);
        packet.to = msgTo;

        return packet;
    }

    public static BrzPacket graphQuery(String to, String id) {
        BrzGraphQuery body = new BrzGraphQuery(true, id);
        BrzPacket packet = new BrzPacket(body);
        packet.to = to;
        return packet;
    }

    public static BrzPacket graphResponse(BrzGraph graph, String to) {
        BrzGraphQuery body = new BrzGraphQuery(false, "", graph.toJSON());
        BrzPacket packet = new BrzPacket(body);
        packet.to = to;
        return packet;
    }

    public static BrzPacket Handshake(BrzGraph graph, String to, String pubKey, String id)
    {
        BrzMessage body = new BrzMessage();

        body.message =pubKey;
        body.userName = "Zach";
        body.datestamp = System.currentTimeMillis();
        body.from = id;

        BrzPacket packet = new BrzPacket(body);
        packet.to = to;
        return packet;


    }
}
