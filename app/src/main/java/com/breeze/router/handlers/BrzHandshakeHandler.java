package com.breeze.router.handlers;

import com.breeze.packets.BrzPacket;
import com.breeze.router.BrzRouter;

public class BrzHandshakeHandler implements BrzRouterHandler {
    public BrzHandshakeHandler(BrzRouter brzRouter) {
    }

    @Override
    public void handle(BrzPacket packet, String fromEndpointId) {
        //TODO CHECK if user name from is in store already or database (check with the user ID)
        //TODO if they're in the db/store, compare the public key passed in w/ whatever public key is associated w/ the id
        //TODO If its a new user id and new public, add them, send an ack that tells them they're added
        //TODO If other issues, like public keys not matching, etc TODO figure out later
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.HANDSHAKE_PACKET;
    }
}
