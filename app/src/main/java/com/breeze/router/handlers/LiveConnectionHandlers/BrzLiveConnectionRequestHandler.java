package com.breeze.router.handlers.LiveConnectionHandlers;

import com.breeze.application.BreezeAPI;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.LiveConnectionEvents.BrzLiveConnectionRequest;
import com.breeze.router.handlers.BrzRouterHandler;

/**
 * This class will be responsible for responding to a request for a live audio stream with another
 * device. The received BrzLiveConnectionRequest contains data about the connection parameters and has
 * an encrypted body, and a secret key for the data that's been encrypted by this device's public key
 *
 * From the LiveConnectionRequest's data, this class:
 *  0. CHECK if user wants to open a BrzLiveAudioStream activity (if not, send BrzLiveConnectionDeclined to other device)
 *  1. adds the BrzLiveConnectionRequest to the state,
 *  2. creates a BrzLiveAudioConsumer object from it and saves to state, (marks it as ready to consume audio),
 *  3. sends back a BrzLiveConnectionReady object to the other device, containing encrypted information
 *     about the BrzLiveAudioConsumer
 */
public class BrzLiveConnectionRequestHandler implements BrzRouterHandler {
    public BrzLiveConnectionRequestHandler() {
        super();
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        BrzLiveConnectionRequest req = packet.connectionRequest();
   //     req.setProducerEndpointID(fromEndpointId);
        BreezeAPI.getInstance().streams.receiveBrzLiveConnectionRequest(req.getProducerEndpointID(), req);
        return true;
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return BrzPacket.BrzPacketType.STREAM_REQ == type;
    }
}
