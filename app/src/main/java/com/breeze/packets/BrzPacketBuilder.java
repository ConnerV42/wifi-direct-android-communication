package com.breeze.packets;

import com.breeze.graph.BrzGraph;
import com.breeze.graph.BrzNode;
import com.breeze.packets.graph.BrzGraphEvent;
import com.breeze.packets.graph.BrzGraphQuery;
import java.io.File;

public class BrzPacketBuilder {

    public static BrzPacket message(String id, String msgBody) {
        return BrzPacketBuilder.message(id, msgBody);
    }

    public static BrzPacket fileName(String fromUUID, String toUUID, String filePayloadId, String fileName) {
        BrzFileInfo body = new BrzFileInfo();

        body.from = fromUUID;
        body.destinationUUID = toUUID;
        body.filePayloadId = filePayloadId;
        body.fileName = fileName;
        body.userName = "Zach";
        body.datestamp = System.currentTimeMillis();

        BrzPacket packet = new BrzPacket(body);
        packet.type = BrzPacket.BrzPacketType.FILE_INFO;
        packet.to = "";

        return packet;
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
        packet.type = BrzPacket.BrzPacketType.GRAPH_QUERY;
        return packet;
    }

    public static BrzPacket graphResponse(BrzGraph graph, BrzNode hostNode, String to) {
        BrzGraphQuery body = new BrzGraphQuery(false, "", graph.toJSON(), hostNode.toJSON());
        BrzPacket packet = new BrzPacket(body);
        packet.to = to;
        packet.type = BrzPacket.BrzPacketType.GRAPH_QUERY;
        return packet;
    }

    public static BrzPacket graphEvent(Boolean connection, BrzNode node1, BrzNode node2) {
        BrzGraphEvent body = new BrzGraphEvent(connection, node1, node2);
        BrzPacket packet = new BrzPacket(body);

        packet.type = BrzPacket.BrzPacketType.GRAPH_EVENT;
        packet.to = "BROADCAST";

        return packet;
    }
}
