package com.breeze.router.handlers;

import android.graphics.Bitmap;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.datatypes.BrzNode;
import com.breeze.graph.BrzGraph;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ProfileEvents.BrzProfileImageEvent;
import com.breeze.router.BrzRouter;

import java.io.InputStream;
import java.util.Collection;

public class BrzAliasNameUpdateHandler implements  BrzRouterHandler{
    private BreezeAPI api;
    private BrzRouter router;
    private BrzGraph graph = new BrzGraph();

    public BrzAliasNameUpdateHandler(BrzRouter router) {
        this.api = BreezeAPI.getInstance();
        this.router = router;
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler doesn't support packets of type " + packet.type);

        //if of packet type then extract name with alias and update that node
        String newName, newAlias;
        newName = packet.aliasAndNameEvent().name;
        newAlias = packet.aliasAndNameEvent().alias;

        Collection<BrzNode> nodes = this.graph.getNodeCollection();

        for (BrzNode node : nodes){
            if(node.id == fromEndpointId){
               node.name = newName;
               node.alias = newAlias;
               api.state.setNode(node);
            }
        }
        return true;
    }

    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.ALIAS_AND_NAME_UPDATE;
    }

}
