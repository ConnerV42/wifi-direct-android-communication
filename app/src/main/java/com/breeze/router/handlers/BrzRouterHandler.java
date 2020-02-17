package com.breeze.router.handlers;

import com.breeze.packets.BrzPacket;

public interface BrzRouterHandler {
    boolean handle(BrzPacket packet, String fromEndpointId);
    boolean handles(BrzPacket.BrzPacketType type);
}
