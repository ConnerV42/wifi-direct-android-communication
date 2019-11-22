package com.breeze.router.handlers;

import com.breeze.packets.BrzPacket;
import com.breeze.router.BrzRouter;

public class BrzHandshakeHandler implements BrzRouterHandler {
    public BrzHandshakeHandler(BrzRouter brzRouter) {
    }

    @Override
    public void handle(BrzPacket packet, String fromEndpointId) {
        
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.HANDSHAKE_PACKET;
    }
}
