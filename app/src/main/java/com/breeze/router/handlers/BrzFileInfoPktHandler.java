package com.breeze.router.handlers;

import com.breeze.datatypes.BrzFileInfo;
import com.breeze.packets.BrzPacket;
import com.breeze.router.BrzRouter;

public class BrzFileInfoPktHandler implements BrzRouterHandler {

    private BrzRouter router;

    public BrzFileInfoPktHandler(BrzRouter router) { this.router = router; }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
//        if (!this.handles(packet.type))
//            throw new RuntimeException("This handler does not handle packets of type " + packet.type);
//
//        BrzFileInfo fileInfoPacket = packet.fileInfoPacket();
//        long id = Long.parseLong(fileInfoPacket.filePayloadId);
//        router.fileInfoPackets.put(id, packet);
//        router.handleFilePayload(id);
        return false;
    }

    public boolean handles(BrzPacket.BrzPacketType type) {
//        return type == BrzPacket.BrzPacketType.FILE_INFO;
        return false;
    }
}
