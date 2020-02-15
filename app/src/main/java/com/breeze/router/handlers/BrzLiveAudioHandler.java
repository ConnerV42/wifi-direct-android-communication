package com.breeze.router.handlers;

import com.breeze.application.BreezeAPI;
import com.breeze.packets.BrzPacket;

import java.io.InputStream;

public class BrzLiveAudioHandler implements BrzRouterStreamHandler {


    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        BreezeAPI api = BreezeAPI.getInstance();

        if (packet.type == BrzPacket.BrzPacketType.LIVE_AUDIO_REQUEST)
            api.streams.incomingRequest(packet.liveAudioEvent());
        else if (packet.type == BrzPacket.BrzPacketType.LIVE_AUDIO_RESPONSE)
            api.streams.incomingResponse(packet.liveAudioEvent());

        return true;
    }

    @Override
    public void handleStream(BrzPacket packet, InputStream stream) {
        BreezeAPI api = BreezeAPI.getInstance();
        api.streams.startPlaying(stream);
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.LIVE_AUDIO_REQUEST ||
                type == BrzPacket.BrzPacketType.LIVE_AUDIO_RESPONSE ||
                type == BrzPacket.BrzPacketType.LIVE_AUDIO_STREAM;
    }
}
