package com.breeze.router.handlers.LiveConnectionHandlers;

import com.breeze.application.BreezeAPI;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.LiveConnectionEvents.BrzLiveConnectionReady;
import com.breeze.router.handlers.BrzRouterHandler;
import com.breeze.streams.BrzLiveAudioProducer;


/**
 * This class will be responsible for
 *
 * [0. On mic record or mic stream start check if BrzLiveAudioStream is ready,]
 * 1. Mark BrzLiveAudioProducer as ready to get the mic input and pipe it to a Payload as an input stream
 * 2. Send Payload to other device as BrzMicStream packet type
 * 3. Clean up any streams
 */
public class BrzLiveConnectionReadyHandler implements BrzRouterHandler {
    @Override
    public void handle(BrzPacket packet, String fromEndpointId) {
        BrzLiveConnectionReady ready = packet.connectionReady();
        BreezeAPI.getInstance().streams.receiveBrzLiveConnectionReady(ready.getProducerEndpointID(), ready);
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.STREAM_READY;
    }
}
