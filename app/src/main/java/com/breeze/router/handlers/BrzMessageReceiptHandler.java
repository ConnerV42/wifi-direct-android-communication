package com.breeze.router.handlers;

import android.util.Log;

import com.breeze.application.BreezeAPI;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.MessageEvents.BrzMessageReceipt;
import com.breeze.router.BrzRouter;

public class BrzMessageReceiptHandler implements BrzRouterHandler {
    private BreezeAPI api = null;
    private BrzRouter router;

    public BrzMessageReceiptHandler(BrzRouter router) {
        this.router = router;
        this.api = BreezeAPI.getInstance();
    }

    @Override
    public void handle(BrzPacket packet, String fromEndpointId) {
        BrzMessageReceipt mr = packet.messageReceipt();

        // For us
        if (packet.to.equals(this.api.hostNode.id)) {
            try {
                if (mr.type == BrzMessageReceipt.ReceiptType.DELIVERED) {
                    Log.i("STATE", "Message delivery success!");
                    api.db.setDelivered(mr.messageId);
                } else if (mr.type == BrzMessageReceipt.ReceiptType.READ) {
                    api.db.setRead(mr.messageId);
                } else {
                    throw new RuntimeException("Unsupported receipt type");
                }
            } catch (Exception e) {
                Log.e("RECEIPT", "Failed to set message's receipt state");
            }
        }

        // Not for us
        else {
            this.router.send(packet);
        }
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.MESSAGE_RECEIPT;
    }
}
