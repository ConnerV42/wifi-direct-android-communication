package com.breeze.router.handlers;

import android.graphics.Bitmap;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ProfileEvents.BrzProfileImageEvent;
import com.breeze.router.BrzRouter;

import java.io.InputStream;

public class BrzChatNameAndImageUpdateHandler implements BrzRouterStreamHandler {

    private BreezeAPI api;
    private BrzRouter router;

    public BrzChatNameAndImageUpdateHandler(BrzRouter router) {
        this.api = BreezeAPI.getInstance();
        this.router = router;
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler doesn't support packets of type " + packet.type);

        
        return true;
    }

    public void handleStream(BrzPacket packet, InputStream stream) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler does not handle packets of type " + packet.type);
        BreezeAPI api = BreezeAPI.getInstance();
        api.storage.saveProfileImage(api.storage.PROFILE_DIR, packet.profileImageEvent().nodeId, stream);
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.PROFILE_REQUEST || type == BrzPacket.BrzPacketType.PROFILE_RESPONSE;
    }
}
