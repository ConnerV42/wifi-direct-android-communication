package com.breeze.router.handlers;

import android.util.Log;

import com.breeze.graph.BrzGraph;
import com.breeze.graph.BrzNode;
import com.breeze.packets.BrzMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.packets.graph.BrzGraphEvent;
import com.breeze.packets.graph.BrzGraphQuery;
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

        // If we got a message that is for us
        if (packet.to.equals(this.router.hostNode.id)) {
            BrzMessage message = packet.message();
            BrzStateStore.getStore().addMessage(message.from, message);
        }

        // forward packets that aren't for us onwards
        else {
            this.router.send(packet);
            Log.i("ENDPOINT", "Relaying message from " + fromEndpointId + " to " + packet.to);
        }
    }

    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.MESSAGE;
    }

}
