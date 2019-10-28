package com.breeze.packets;

public class BrzPacketBuilder {

    public static BrzPacket message(String msgBody) {
        return BrzPacketBuilder.message("", msgBody);
    }

    public static BrzPacket message(String msgTo, String msgBody) {
        BrzBodyMessage body = new BrzBodyMessage();

        body.message = msgBody;
        body.userName = "Zach";
        body.datestamp = System.currentTimeMillis();

        BrzPacket packet = new BrzPacket(body);
        packet.to = msgTo;

        return packet;
    }
}
