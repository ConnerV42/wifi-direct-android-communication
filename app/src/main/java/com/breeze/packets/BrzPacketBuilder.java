package com.breeze.packets;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzMessage;
import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.GraphEvents.BrzGraphEvent;
import com.breeze.packets.GraphEvents.BrzGraphQuery;
import com.breeze.packets.MessageEvents.BrzMessageReceipt;

public class BrzPacketBuilder {

    public static BrzPacket ack(BrzPacket packet, String to) {
        BrzPacket ackPacket = new BrzPacket();

        ackPacket.type = BrzPacket.BrzPacketType.ACK;
        ackPacket.to = to;
        ackPacket.id = packet.id;

        return ackPacket;
    }

    public static BrzPacket message(String id, String msgTo, String msgBody, String chatId, Boolean isStatus) {
        BrzMessage body = new BrzMessage();

        body.from = id;
        body.body = msgBody;
        body.chatId = chatId;

        body.isStatus = isStatus;
        body.datestamp = System.currentTimeMillis();

        BrzPacket packet = new BrzPacket(body);
        packet.to = msgTo;

        return packet;
    }

    public static BrzMessage makeMessage(String fromId, String msgBody, String chatId, Boolean isStatus) {
        BrzMessage body = new BrzMessage();

        body.from = fromId;
        body.body = msgBody;
        body.chatId = chatId;

        body.isStatus = isStatus;
        body.datestamp = System.currentTimeMillis();

        return body;
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

    public static BrzPacket messageReceipt(String to, String chatId, boolean delivered) {
        String from = BreezeAPI.getInstance().hostNode.id;

        BrzMessageReceipt mr = null;
        if (delivered)
            mr = new BrzMessageReceipt(from, chatId, BrzMessageReceipt.ReceiptType.DELIVERED);
        else
            mr = new BrzMessageReceipt(from, chatId, BrzMessageReceipt.ReceiptType.READ);

        BrzPacket packet = new BrzPacket(mr);
        packet.type = BrzPacket.BrzPacketType.MESSAGE_RECEIPT;
        packet.to = to;

        return packet;
    }
}
