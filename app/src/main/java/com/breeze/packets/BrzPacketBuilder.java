package com.breeze.packets;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzMessage;
import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.GraphEvents.BrzGraphEvent;
import com.breeze.packets.GraphEvents.BrzGraphQuery;

import com.breeze.packets.MessageEvents.BrzMessageReceipt;
import com.breeze.packets.ProfileEvents.BrzAliasAndNameEvent;

public class BrzPacketBuilder {

    public static BrzPacket message(String fromId, String msgTo, String msgBody, String chatId, Boolean isStatus) {
        BrzMessage body = new BrzMessage(fromId, chatId, msgBody, System.currentTimeMillis(), isStatus);
        return new BrzPacket(body, BrzPacket.BrzPacketType.MESSAGE, msgTo, false);
    }

    public static BrzPacket graphQuery(String to, String from) {
        BrzGraphQuery body = new BrzGraphQuery(true, from);
        return new BrzPacket(body, BrzPacket.BrzPacketType.GRAPH_QUERY, to, false);
    }

    public static BrzPacket aliasAndNameEvent(String to, String from, String name, String alias) {
        BrzAliasAndNameEvent body = new BrzAliasAndNameEvent(from, name, alias);
        return new BrzPacket(body, BrzPacket.BrzPacketType.ALIAS_AND_NAME_UPDATE, to, false);
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
