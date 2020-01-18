package com.breeze.router.handlers;

import com.breeze.application.BreezeAPI;
import com.breeze.packets.BrzPacket;
import com.breeze.router.BrzRouter;

import java.io.InputStream;

public class BrzProfileHandler implements BrzRouterStreamHandler {
    private BrzRouter router;

    public BrzProfileHandler(BrzRouter router) {
        this.router = router;
    }

    @Override
    public void handle(BrzPacket packet, String fromEndpointId) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler doesn't support packets of type " + packet.type);

        if (packet.type == BrzPacket.BrzPacketType.PROFILE_REQUEST) {
            BreezeAPI api = BreezeAPI.getInstance();
            api.storage.getProfileImage(router.hostNode.id, api);
        } else {

        }
        // TODO: If it's a BrzProfileResponse, send to handleStream
    }

    public void handleStream(BrzPacket packet, InputStream stream) {
        // TODO: save and assign profilePhoto to node if it's a BrzProfileResponse
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.PROFILE_REQUEST || type == BrzPacket.BrzPacketType.PROFILE_RESPONSE;
    }
}
