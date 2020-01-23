package com.breeze.router.handlers;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.router.BrzRouter;

import java.io.InputStream;

public class BrzMessageHandler implements BrzRouterStreamHandler {

    private BrzRouter router;

    public BrzMessageHandler(BrzRouter router) {
        this.router = router;
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler does not handle packets of type " + packet.type);

        BrzMessage m = packet.message();
        BreezeAPI api = BreezeAPI.getInstance();

        // Attempt to decrypt the message, or ignore it if there's an error
        try {
            api.encryption.decryptMessage(m);
            api.meta.showMessageNotification(m);
            api.addMessage(m);

            // Send delivery acknowledgement
            api.meta.sendDeliveryReceipt(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void handleStream(BrzPacket packet, InputStream stream) {
        BreezeAPI api = BreezeAPI.getInstance();
        BrzMessage m = packet.message();
        InputStream decryptedStream = api.encryption.decryptStream(m.chatId, packet.stream, stream);
        api.storage.saveMessageFile(packet.message(), decryptedStream);
        this.handle(packet, "");
    }

    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.MESSAGE;
    }

}
