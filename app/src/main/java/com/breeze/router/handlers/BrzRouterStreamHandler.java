package com.breeze.router.handlers;

import com.breeze.packets.BrzPacket;

import java.io.InputStream;

public interface BrzRouterStreamHandler extends BrzRouterHandler {
    public void handleStream(BrzPacket packet, InputStream stream);
}
