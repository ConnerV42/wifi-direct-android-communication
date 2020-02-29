package com.breeze.router.handlers;

import android.util.Log;

import com.breeze.application.BreezeAPI;
import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.packets.GraphEvents.BrzGraphEvent;
import com.breeze.packets.GraphEvents.BrzGraphQuery;
import com.breeze.packets.GraphEvents.BrzGraphUpdateBroadcast;
import com.breeze.router.BrzRouter;

public class BrzGraphHandler implements BrzRouterHandler {

    private BrzRouter router;
    private BreezeAPI api = BreezeAPI.getInstance();

    public BrzGraphHandler(BrzRouter router) {
        this.router = router;
    }

    @Override
    public boolean handle(BrzPacket packet, String fromEndpointId) {
        if (!this.handles(packet.type))
            throw new RuntimeException("This handler does not handle packets of type " + packet.type);

        BrzGraph graph = api.getGraph();

        // Respond to graph queries
        if (packet.type == BrzPacket.BrzPacketType.GRAPH_QUERY) {
            BrzGraphQuery query = packet.graphQuery();
            Log.i("ENDPOINT", "Got a graph query " + query.toJSON());

            // Respond to graph query
            if (query.type == BrzGraphQuery.BrzGQType.REQUEST) {
                BrzPacket resPacket = BrzPacketBuilder.graphResponse(graph, router.hostNode, query.from);
                this.router.send(resPacket);
                Log.i("ENDPOINT", "Responed to graph query");
            }

            // Update graph
            else {
                Log.i("ENDPOINT", "Merging graph information");

                // Add the newly connected node
                BrzNode connectingNode = new BrzNode(query.hostNode);
                connectingNode.endpointId = fromEndpointId;
                graph.setVertex(connectingNode);
                graph.addEdge(router.hostNode.id, connectingNode.id);

                // Generate a difference graph
                BrzGraph diff = graph.diff(new BrzGraph(query.graph), router.hostNode, connectingNode);

                // Load our graph with new information from the other node
                graph.mergeGraph(query.graph);

                // Finally, broadcast the diff to previously connected nodes
                router.broadcast(BrzPacketBuilder.graphUpdateBroadcast(diff));
            }
        }


        if (packet.type == BrzPacket.BrzPacketType.GRAPH_EVENT) {

            BrzGraphEvent ge = packet.graphEvent();

            // Don't accept events involving yourself
            if (ge.node1.id.equals(router.hostNode.id) || ge.node2.id.equals(router.hostNode.id))
                return true;

            // If it's a new connection
            if (ge.type == BrzGraphEvent.BrzGEType.CONNECT) {
                graph.addVertex(ge.node1);
                graph.addVertex(ge.node2);
                graph.addEdge(ge.node1.id, ge.node2.id);

                // If it's a disconnect
            } else {
                graph.removeEdge(ge.node1.id, ge.node2.id);
                graph.removeDisconnected(router.hostNode.id);
            }

            // Continue broadcasting
            return false;
        }

        if (packet.type == BrzPacket.BrzPacketType.GRAPH_UPDATE) {
            BrzGraphUpdateBroadcast gu = packet.graphUpdate();
            graph.mergeGraph(gu.diff);

            // Continue broadcasting
            return false;
        }

        return true;
    }

    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.GRAPH_EVENT || type == BrzPacket.BrzPacketType.GRAPH_QUERY || type == BrzPacket.BrzPacketType.GRAPH_UPDATE;
    }

}
