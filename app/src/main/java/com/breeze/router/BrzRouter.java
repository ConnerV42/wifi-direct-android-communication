package com.breeze.router;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.breeze.packets.BrzMessage;
import com.breeze.graph.BrzGraph;
import com.breeze.graph.BrzNode;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.packets.graph.BrzGraphEvent;
import com.breeze.packets.graph.BrzGraphQuery;
import com.breeze.state.BrzStateStore;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BrzRouter {
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private ConnectionsClient connectionsClient;

    private BrzPacketBuffer buffer = new BrzPacketBuffer();

    private List<String> connectedEndpoints = new ArrayList<>();
    private Map<String, String> endpointUUIDs = new HashMap<>();

    private Map<String, String> seenPackets = new HashMap<>();


    private boolean running = false;
    private String pkgName = "";
    public final String id = UUID.randomUUID().toString();
    private BrzGraph graph;

    Context ctx;

    private static BrzRouter instance;

    private BrzRouter(ConnectionsClient cc, String pkgName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;
        this.graph = BrzGraph.getInstance();

        // When we log in, add our own node to the graph
        BrzStateStore.getStore().getUser(brzUser -> {
            if(brzUser == null) return;
            BrzNode hostNode = new BrzNode(id, "", "", brzUser);
            this.graph.addVertex(hostNode);
        });


        // Begin discovery!
        this.start();
    }

    public static BrzRouter getInstance(ConnectionsClient cc, String pkgName, Context ctx) {
        if (instance == null) instance = new BrzRouter(cc, pkgName);
        instance.ctx = ctx;
        return instance;
    }

    public static BrzRouter getInstance() {
        return instance;
    }

    public void broadcast(BrzPacket packet) {
        packet.to = "BROADCAST";
        Payload p = Payload.fromBytes(packet.toJSON().getBytes());

        for(String id : connectedEndpoints) {
            connectionsClient.sendPayload(id, p);
        }
    }

    public void broadcast(BrzPacket packet, String ignoreEndpoint) {
        packet.to = "BROADCAST";
        Payload p = Payload.fromBytes(packet.toJSON().getBytes());

        for(String id : connectedEndpoints) {
            if(!id.equals(ignoreEndpoint)) connectionsClient.sendPayload(id, p);
        }
    }

    public void send(BrzPacket packet) {
        forwardPacket(packet);
        buffer.addPacket(packet, 5000, 5, this::forwardPacket);
    }

    private void forwardPacket(BrzPacket packet) {
        Payload p = Payload.fromBytes(packet.toJSON().getBytes());

        String nextHopUUID = graph.nextHop(this.id, packet.to);
        BrzNode nextHopNode = this.graph.getVertex(nextHopUUID);

        if (
                nextHopNode != null &&
                        !nextHopNode.endpointId.equals("") &&
                        this.connectedEndpoints.contains(nextHopNode.endpointId)
        ) connectionsClient.sendPayload(nextHopNode.endpointId, p);
    }

    public void start() {
        if (running) return;
        running = true;

        startAdvertising();
        startDiscovery();
    }

    public void stop() {
        running = false;

        connectionsClient.stopAllEndpoints();
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
    }

    private void startAdvertising() {
        connectionsClient.startAdvertising(id,
                pkgName, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    private void startDiscovery() {
        connectionsClient.startDiscovery(
                pkgName, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    private void handlePacket(BrzPacket packet, String fromEndpointId) {

        // Acknowledge packets we receive
        if (packet.type != BrzPacket.BrzPacketType.ACK) {
            BrzPacket ack = BrzPacketBuilder.ack(packet, this.endpointUUIDs.get(fromEndpointId));
            forwardPacket(ack);
            // Remove acknowledged packets from the buffer
        } else {
            this.buffer.removePacket(packet.id);
        }

        // Respond to graph queries
        if (packet.type == BrzPacket.BrzPacketType.GRAPH_QUERY) {
            BrzGraphQuery query = packet.graphQuery();

            // Respond to graph query
            if (query.type == BrzGraphQuery.BrzGQType.REQUEST) {
                BrzPacket resPacket = BrzPacketBuilder.graphResponse(this.graph, this.graph.getVertex(id), query.from);
                send(resPacket);
            }

            // Update graph
            else if (!query.graph.equals("") && !query.hostNode.equals("")) {

                // Add the newly connected node
                BrzNode hostNode = new BrzNode(query.hostNode);
                hostNode.endpointId = fromEndpointId;
                graph.setVertex(hostNode);
                graph.addEdge(id, hostNode.id);

                // Broadcast connect event
                broadcast(BrzPacketBuilder.graphEvent(true, graph.getVertex(id), hostNode));

                // Merge their graph into ours
                this.graph.mergeGraph(query.graph);

                // TEMP: Create a chat for each graph node
                for(BrzNode n : this.graph) {
                    BrzStateStore.getStore().addChat(new BrzChat(n.id, n.user.name));
                }
            }
        }

        // Respond to graph queries
        if (packet.type == BrzPacket.BrzPacketType.GRAPH_EVENT) {

            BrzGraphEvent ge = packet.graphEvent();
            if (ge.type == BrzGraphEvent.BrzGEType.CONNECT) {
                graph.addVertex(ge.node1);
                graph.addVertex(ge.node2);
                graph.addEdge(ge.node1.id, ge.node2.id);
            } else {
                graph.removeEdge(ge.node1.id, ge.node2.id);
            }

            if(seenPackets.get(packet.id) != null) return;
            seenPackets.put(packet.id, "");

            broadcast(packet, fromEndpointId);
        }

        // forward packets that aren't for us onwards
        else if (!packet.to.equals(this.id)) {
            send(packet);
            Toast.makeText(ctx, "Forwarded packet", Toast.LENGTH_LONG).show();
        }

        // If we got a message that is for us
        else if (packet.type == BrzPacket.BrzPacketType.MESSAGE) {
            BrzMessage message = packet.message();
            BrzStateStore.getStore().addMessage(message.from, message);
        }

    }

    // Callback for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, Payload payload) {
                    byte[] payloadBody = payload.asBytes();
                    if (payloadBody == null) return;

                    String json = new String(payloadBody, UTF_8);
                    BrzPacket packet = new BrzPacket(json);

                    handlePacket(packet, endpointId);
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                    }
                }
            };

    // Callback for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(@NonNull String endpointId, DiscoveredEndpointInfo info) {
                    if (info.getServiceId().equals(pkgName) && connectedEndpoints.size() < 2)
                        connectionsClient.requestConnection(id, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {
                }
            };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String endpointId, ConnectionInfo connectionInfo) {
                    endpointUUIDs.put(endpointId, connectionInfo.getEndpointName());
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        connectedEndpoints.add(endpointId);
                        String endpointUUID = endpointUUIDs.get(endpointId);

                        // Do handshake query
                        BrzPacket reqPacket = BrzPacketBuilder.graphQuery(endpointUUID, id);
                        send(reqPacket);
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    // Remove edge from our graph
                    connectedEndpoints.remove(endpointId);
                    String endpointUUID = endpointUUIDs.get(endpointId);
                    graph.removeEdge(id, endpointUUID);

                    // Broadcast disconnect event
                    String name = endpointUUIDs.get(endpointId);
                    broadcast(BrzPacketBuilder.graphEvent(false, graph.getVertex(id), graph.getVertex(name)));
                }
            };
}
