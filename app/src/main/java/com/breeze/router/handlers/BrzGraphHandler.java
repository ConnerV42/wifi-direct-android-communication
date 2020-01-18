package com.breeze.router.handlers;

import android.util.Log;

import com.breeze.application.BreezeAPI;
import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.packets.GraphEvents.BrzGraphEvent;
import com.breeze.packets.GraphEvents.BrzGraphQuery;
import com.breeze.router.BrzRouter;

public class BrzGraphHandler implements BrzRouterHandler {

    private BrzRouter router;
    private BrzGraph graph = BrzGraph.getInstance();

    public BrzGraphHandler(BrzRouter router) {
        this.router = router;
    }

    @Override
    public void handle(BrzPacket packet, String fromEndpointId) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler does not handle packets of type " + packet.type);

        // Respond to graph queries
        if (packet.type == BrzPacket.BrzPacketType.GRAPH_QUERY) {
            BrzGraphQuery query = packet.graphQuery();
            Log.i("ENDPOINT", "Got a graph query " + query.toJSON());

            // Respond to graph query
            if (query.type == BrzGraphQuery.BrzGQType.REQUEST) {
                BrzPacket resPacket = BrzPacketBuilder.graphResponse(this.graph, router.hostNode, query.from);
                this.router.send(resPacket);
                Log.i("ENDPOINT", "Responed to graph query");
            }

            // Update graph
            else {
                Log.i("ENDPOINT", "Merging graph information");

                // Add the newly connected node
                BrzNode hostNode = new BrzNode(query.hostNode);
                hostNode.endpointId = fromEndpointId;
                graph.setVertex(hostNode);
                graph.addEdge(this.router.hostNode.id, hostNode.id);

                // Broadcast connect event
                this.router.broadcast(BrzPacketBuilder.graphEvent(true, router.hostNode, hostNode));

                // Merge their graph into ours
                BrzGraph otherGraph = this.graph.mergeGraph(query.graph);

                // Send a BrzProfileRequest packet to all newly merged nodes
                BreezeAPI.getInstance().requestProfileImages(otherGraph);
            }
        }

        // Respond to graph queries
        if (packet.type == BrzPacket.BrzPacketType.GRAPH_EVENT) {

            BrzGraphEvent ge = packet.graphEvent();

            // Don't accept events involving yourself
            if (ge.node1.id.equals(router.hostNode.id) || ge.node2.id.equals(router.hostNode.id))
                return;

            // If it's a new connection
            if (ge.type == BrzGraphEvent.BrzGEType.CONNECT) {
                graph.addVertex(ge.node1);
                graph.addVertex(ge.node2);
                graph.addEdge(ge.node1.id, ge.node2.id);

                // If it's a disconnect
            } else {
                graph.removeEdge(ge.node1.id, ge.node2.id);
            }

        }
    }

    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.GRAPH_EVENT || type == BrzPacket.BrzPacketType.GRAPH_QUERY;
    }

}
