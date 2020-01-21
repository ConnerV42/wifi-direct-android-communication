package com.breeze.router.handlers;

import android.graphics.Bitmap;
import android.telephony.mbms.FileInfo;

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

                BrzFileInfo fileInfo = new BrzFileInfo();
                fileInfo.fileName = router.hostNode.id;
                p.addStream(fileInfo);

                api.sendProfileResponse(p, bm);
            }
        }
    }

    public void handleStream(BrzPacket packet, InputStream stream) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler does not handle packets of type " + packet.type);
        BreezeAPI api = BreezeAPI.getInstance();
        api.storage.saveProfileImageFile(packet, stream);
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.PROFILE_REQUEST || type == BrzPacket.BrzPacketType.PROFILE_RESPONSE;
    }
}
