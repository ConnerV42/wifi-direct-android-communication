package com.breeze.router.handlers;

import android.util.Log;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;

public class BrzMessageHandler implements BrzRouterHandler {

    private BrzRouter router;

    public BrzMessageHandler(BrzRouter router) {
        this.router = router;
    }

    @Override
    public void handle(BrzPacket packet, String fromEndpointId) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler does not handle packets of type " + packet.type);

        BrzMessage m = packet.message();
        BreezeAPI.getInstance().addMessage(m);

        // Send delivery acknowledgement
        BrzPacket p = BrzPacketBuilder.messageReceipt(m.from, m.chatId, true);
        router.send(p);

    }

    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.MESSAGE;
    }

}
