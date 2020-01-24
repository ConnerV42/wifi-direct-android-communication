package com.breeze.router.handlers.LiveConnectionHandlers;

import com.breeze.packets.BrzPacket;
import com.breeze.router.handlers.BrzRouterHandler;

public class BrzLiveConnectionEventHandler implements BrzRouterHandler {
    public BrzLiveConnectionEventHandler() {
        super();
    }

    @Override
    public void handle(BrzPacket packet, String fromEndpointId) {

    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return false;
    }
}
