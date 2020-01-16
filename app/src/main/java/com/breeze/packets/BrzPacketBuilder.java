package com.breeze.packets;

import com.breeze.datatypes.BrzFileInfo;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzMessage;
import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.GraphEvents.BrzGraphEvent;
import com.breeze.packets.GraphEvents.BrzGraphQuery;

import java.io.File;

import com.breeze.packets.MessageEvents.BrzMessageReceipt;

public class BrzPacketBuilder {

    public static BrzPacket message(String fromId, String msgTo, String msgBody, String chatId, Boolean isStatus) {
        BrzMessage body = new BrzMessage(fromId, chatId, msgBody, System.currentTimeMillis(), isStatus);
        return new BrzPacket(body, BrzPacket.BrzPacketType.MESSAGE, msgTo, false);
    }
     public static BrzPacket publicMessage(String fromId, String msgTo, String msgBody, String chatId, Boolean isStatus){
        BrzMessage body = new BrzMessage(fromId, chatId, msgBody, System.currentTimeMillis(), isStatus);
        return new BrzPacket(body, BrzPacket.BrzPacketType.PUBLIC_MESSAGE, msgTo, false);
     }
    public static BrzPacket fileInfoPacket(String fromId, String nextId, String chatId, String destinationId, String filePayloadId, String fileName) {
        BrzFileInfo body = new BrzFileInfo();

        body.fromId = fromId;
        body.nextId = nextId;
        body.chatId = chatId;
        body.destinationId = destinationId;
        body.filePayloadId = filePayloadId;
        body.fileName = fileName;
        body.datestamp = System.currentTimeMillis();

        return new BrzPacket(body, BrzPacket.BrzPacketType.FILE_INFO, nextId, false);
    }

    public static BrzPacket graphQuery(String to, String from) {
        BrzGraphQuery body = new BrzGraphQuery(true, from);
        return new BrzPacket(body, BrzPacket.BrzPacketType.GRAPH_QUERY, to, false);
    }

    public static BrzPacket graphResponse(BrzGraph graph, BrzNode hostNode, String to) {
        String from = BreezeAPI.getInstance().hostNode.id;
        BrzGraphQuery body = new BrzGraphQuery(false, from, graph.toJSON(), hostNode.toJSON());
        return new BrzPacket(body, BrzPacket.BrzPacketType.GRAPH_QUERY, to, false);
    }

    public static BrzPacket graphEvent(Boolean connection, BrzNode node1, BrzNode node2) {
        BrzGraphEvent body = new BrzGraphEvent(connection, node1, node2);
        return new BrzPacket(body, BrzPacket.BrzPacketType.GRAPH_EVENT, "BROADCAST", true);
    }

    public static BrzPacket messageReceipt(String to, String chatId, String messageId, boolean delivered) {
        String from = BreezeAPI.getInstance().hostNode.id;

        BrzMessageReceipt mr = null;
        if (delivered)
            mr = new BrzMessageReceipt(from, chatId, messageId, BrzMessageReceipt.ReceiptType.DELIVERED);
        else
            mr = new BrzMessageReceipt(from, chatId, messageId, BrzMessageReceipt.ReceiptType.READ);

        return new BrzPacket(mr, BrzPacket.BrzPacketType.MESSAGE_RECEIPT, to, false);
    }
}
