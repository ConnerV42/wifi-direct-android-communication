package com.breeze.router.handlers;

import android.graphics.Bitmap;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ProfileEvents.BrzProfileImageEvent;
import com.breeze.router.BrzRouter;

import java.io.InputStream;

public class BrzProfileHandler implements BrzRouterStreamHandler {
    private BreezeAPI api;
    private BrzRouter router;

    public BrzProfileHandler(BrzRouter router) {
        this.api = BreezeAPI.getInstance();
        this.router = router;
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler doesn't support packets of type " + packet.type);

        // If the event isn't a request, event isn't handled
        if (packet.type != BrzPacket.BrzPacketType.PROFILE_REQUEST) return false;

        // If we dont have the image, event isn't handled
        BrzProfileImageEvent request = packet.profileImageEvent();
        if (!api.storage.hasProfileImage(api.storage.PROFILE_DIR, request.nodeId)) return false;

        // Otherwise, handle the request!
        Bitmap bm = api.storage.getProfileImage(api.storage.PROFILE_DIR, request.nodeId);

        BrzProfileImageEvent response = new BrzProfileImageEvent(router.hostNode.id, request.nodeId, false);
        BrzPacket p = new BrzPacket(response, BrzPacket.BrzPacketType.PROFILE_RESPONSE, request.from, false);

        BrzFileInfo fileInfo = new BrzFileInfo();
        fileInfo.fileName = request.nodeId;
        p.addStream(fileInfo);

        api.sendProfileResponse(p, bm);
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
