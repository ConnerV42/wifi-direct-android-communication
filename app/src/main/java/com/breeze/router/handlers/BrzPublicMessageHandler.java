package com.breeze.router.handlers;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.router.BrzRouter;

public class BrzPublicMessageHandler implements BrzRouterHandler{

    private BrzRouter router;

    public BrzPublicMessageHandler(BrzRouter router) {
        this.router = router;
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler does not handle packets of type " + packet.type);

        // Add the message to our store
        BreezeAPI api = BreezeAPI.getInstance();
        if(api.state.isPublicThreadOn()){
            api.state.addPublicMessage(packet.message());
        }
        // Set "handled" to false so the message continues broadcasting
        return false;
    }

    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.PUBLIC_MESSAGE;
    }
}
