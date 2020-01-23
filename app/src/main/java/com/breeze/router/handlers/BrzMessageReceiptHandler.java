package com.breeze.router.handlers;

import android.util.Log;

import com.breeze.application.BreezeAPI;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.MessageEvents.BrzMessageReceipt;
import com.breeze.router.BrzRouter;

public class BrzMessageReceiptHandler implements BrzRouterHandler {
    private BreezeAPI api = null;

    public BrzMessageReceiptHandler() {
        this.api = BreezeAPI.getInstance();
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        BrzMessageReceipt mr = packet.messageReceipt();
        Log.i("STATE", "Message " + mr.messageId + " was " + mr.type);

        try {
            if (mr.type == BrzMessageReceipt.ReceiptType.DELIVERED) {
                Log.i("STATE", "Message delivery success!");
                api.meta.setDelivered(mr.messageId);
            } else if (mr.type == BrzMessageReceipt.ReceiptType.READ) {
                Log.i("STATE", "Message read success!");
                api.meta.setRead(mr.messageId);
            } else {
                throw new RuntimeException("Unsupported receipt type");
            }
        } catch (Exception e) {
            Log.e("RECEIPT", "Failed to set message's receipt state");
        }
        return true;
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.MESSAGE_RECEIPT;
    }
}
