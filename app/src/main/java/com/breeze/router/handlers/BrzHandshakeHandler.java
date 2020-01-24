package com.breeze.router.handlers;

import com.breeze.application.BreezeAPI;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ChatEvents.BrzChatHandshake;
import com.breeze.router.BrzRouter;

public class BrzHandshakeHandler implements BrzRouterHandler {
    private BrzRouter router;

    public BrzHandshakeHandler(BrzRouter brzRouter) {
        this.router = brzRouter;
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        BreezeAPI api = BreezeAPI.getInstance();

        if (packet.type == BrzPacket.BrzPacketType.CHAT_HANDSHAKE) {
            BrzChatHandshake handshake = packet.chatHandshake();
            api.incomingHandshake(handshake);
            api.meta.showHandshakeNotification(handshake.chat.id);
        } else if (packet.type == BrzPacket.BrzPacketType.CHAT_RESPONSE) {
            api.incomingChatResponse(packet.chatResponse());
        }
        return true;
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.CHAT_HANDSHAKE || type == BrzPacket.BrzPacketType.CHAT_RESPONSE;
    }
}
