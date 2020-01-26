package com.breeze.router.handlers.LiveConnectionHandlers;

import com.breeze.packets.BrzPacket;
import com.breeze.router.handlers.BrzRouterHandler;

public class BrzLiveConnectionEventHandler implements BrzRouterHandler {
    public BrzLiveConnectionEventHandler() {
        super();
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        return false;
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return false;
    }
}
