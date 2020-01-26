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


        BrzMessage m = packet.publicMessage();
        if(!m.chatId.equals("PUBLIC_THREAD")){
            return false;
        }
        BreezeAPI api = BreezeAPI.getInstance();
        try {
           // api.meta.showNotification(m);
            api.addMessage(m);

            // Send delivery acknowledgement
            api.meta.sendDeliveryReceipt(m);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.PUBLIC_MESSAGE;
    }
}
