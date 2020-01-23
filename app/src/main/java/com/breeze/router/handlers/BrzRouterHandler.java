package com.breeze.router.handlers;

import com.breeze.packets.BrzPacket;

public interface BrzRouterHandler {
    public boolean handle(BrzPacket packet, String fromEndpointId);
    public boolean handles(BrzPacket.BrzPacketType type);
}
