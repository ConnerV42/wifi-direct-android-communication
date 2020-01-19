package com.breeze.router.handlers;

import android.graphics.Bitmap;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.packets.ProfileEvents.BrzProfileRequest;
import com.breeze.packets.ProfileEvents.BrzProfileResponse;
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
            BrzProfileRequest request = packet.profileRequest();

            BreezeAPI api = BreezeAPI.getInstance();
            if (api.storage.hasProfileImage(router.hostNode.id)) {
                Bitmap bm = api.storage.getProfileImage(router.hostNode.id, api);

                BrzProfileResponse response = new BrzProfileResponse(router.hostNode.id, false);
                BrzPacket p = new BrzPacket(response, BrzPacket.BrzPacketType.PROFILE_RESPONSE, request.from, false);

                // TODO: Add FileInfo
                p.stream = new BrzFileInfo();

                // TODO: Add bitmap as an InputStream to the packet
                //p.addStream(bm);

                router.send(p);
            }

        } else {

        }
        // TODO: If it's a BrzProfileResponse, send to handleStream
    }

    public void handleStream(BrzPacket packet, InputStream stream) {
        BreezeAPI api = BreezeAPI.getInstance();

        api.incomingProfileResponse(packet.profileResponse(), stream);
        this.handle(packet, "");
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.PROFILE_REQUEST || type == BrzPacket.BrzPacketType.PROFILE_RESPONSE;
    }
}
