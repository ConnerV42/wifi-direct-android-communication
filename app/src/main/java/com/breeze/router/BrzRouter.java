package com.breeze.router;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.handlers.BrzGraphHandler;
import com.breeze.router.handlers.BrzMessageHandler;
import com.breeze.router.handlers.BrzRouterHandler;
import com.breeze.state.BrzStateStore;
import com.google.android.gms.nearby.Nearby;
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
import java.util.function.Consumer;

public class BrzRouter {

    // Single instance

    private static BrzRouter instance;

    public static BrzRouter initialize(Context ctx, String pkgName) {
        if (instance == null) instance = new BrzRouter(Nearby.getConnectionsClient(ctx), pkgName);
        return instance;
    }

    public static BrzRouter getInstance() {
        return instance;
    }

    //--------------------------------------------------------------------------//

    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private ConnectionsClient connectionsClient;
    private String pkgName;
    private boolean running = false;

    private BrzPayloadBuffer payloadBuffer = new BrzPayloadBuffer();
    private List<BrzRouterHandler> handlers = new ArrayList<>();

    private List<String> connectedEndpoints = new ArrayList<>();
    private Map<String, String> endpointUUIDs = new HashMap<>();

    private BrzGraph graph;
    public BrzNode hostNode = null;


    private BrzRouter(ConnectionsClient cc, String pkgName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;
        this.graph = BrzGraph.getInstance();

        // Initalize handlers
        this.handlers.add(new BrzGraphHandler(this));
        this.handlers.add(new BrzMessageHandler(this));
    }

    //
    //
    //      Public access packet sending handlers
    //
    //

    public void broadcast(BrzPacket packet) {
        this.broadcast(packet, "");
    }

    public void broadcast(BrzPacket packet, String ignoreEndpoint) {
        packet.to = "BROADCAST";
        Payload p = payloadBuffer.getStreamPayload(packet.toJSON());

        for (String id : connectedEndpoints) {
            if (!id.equals(ignoreEndpoint)) connectionsClient.sendPayload(id, p);
        }
    }

    public void send(BrzPacket packet) {
        forwardPacket(packet, true);
    }

    public void sendToEndpoint(BrzPacket packet, String endpointId) {
        connectionsClient.sendPayload(endpointId, payloadBuffer.getStreamPayload(packet.toJSON()));
    }

    private void forwardPacket(BrzPacket packet, Boolean addToBuffer) {

        Consumer<Payload> sendPayload = payload -> {
            String nextHopUUID = graph.nextHop(this.hostNode.id, packet.to);
            if (nextHopUUID == null) {
                Log.i("ENDPOINT_ERR", "Failed to find path to " + packet.to);
                return;
            }
            BrzNode nextHopNode = this.graph.getVertex(nextHopUUID);

            if (
                    nextHopNode != null &&
                            !nextHopNode.endpointId.equals("") &&
                            this.connectedEndpoints.contains(nextHopNode.endpointId)
            ) connectionsClient.sendPayload(nextHopNode.endpointId, payload);
        };

        Payload p = payloadBuffer.getStreamPayload(packet.toJSON());
        if (addToBuffer) payloadBuffer.addOutgoing(p, 5000, 5, sendPayload);
        else sendPayload.accept(p);
    }


    //
    //
    //      Lifecycle
    //
    //


    public void start(BrzNode hostNode) {
        if(hostNode == null) return;

        this.hostNode = hostNode;
        this.graph.setVertex(hostNode);

        this.start();
    }

    public void start() {
        if (running || this.hostNode == null) return;
        running = true;

        Log.i("ENDPOINT", "Starting advertising and discovery");

        startAdvertising();
        startDiscovery();
    }

    public void stop() {
        running = false;
        Log.i("ENDPOINT", "Stopping nearby completely");

        connectionsClient.stopAllEndpoints();
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
    }

    private void startAdvertising() {
        connectionsClient.startAdvertising(this.hostNode.id,
                pkgName, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener(e -> {
                    Log.i("ENDPOINT", "Advertising successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.i("ENDPOINT", "Advertising failed!", e);
                });
    }

    private void startDiscovery() {
        connectionsClient.startDiscovery(
                pkgName, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener(e -> {
                    Log.i("ENDPOINT", "Discovering successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.i("ENDPOINT", "Discovering failed!", e);
                });
    }


    private void handlePacket(BrzPacket packet, String fromEndpointId) {
        Log.i("ENDPOINT", "Got a packet " + packet.type);

        // Continue the broadcast if the packet has not been seen before
        if (packet.to.equals("BROADCAST") && !this.payloadBuffer.broadcastSeen(packet.id)) {
            this.payloadBuffer.addBroadcast(packet.id);
            broadcast(packet, fromEndpointId);
        }

        for (BrzRouterHandler handler : this.handlers)
            if (handler.handles(packet.type)) handler.handle(packet, fromEndpointId);

    }


    //
    //
    //      Connections client callbacks
    //
    //


    // Callback for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, Payload payload) {
                    payloadBuffer.addIncoming(payload);
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
                    if (update.getStatus() != PayloadTransferUpdate.Status.SUCCESS) return;
                    Long payloadId = update.getPayloadId();

                    // Incomming packet was a success!
                    if (payloadBuffer.isIncomming(payloadId)) {
                        Log.i("ENDPOINT", "Recieved a payload successfully");

                        Payload p = payloadBuffer.popIncoming(payloadId);
                        BrzPacket packet = new BrzPacket(payloadBuffer.getStreamString(p));
                        handlePacket(packet, endpointId);
                    }

                    // Outgoing packet was a success!
                    else {
                        payloadBuffer.removeOutgoing(payloadId);
                    }
                }
            };

    // Callback for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(@NonNull String endpointId, DiscoveredEndpointInfo info) {
                    Log.i("ENDPOINT", "Endpoint found");
                    if (info.getServiceId().equals(pkgName))
                        connectionsClient.requestConnection(hostNode.id, endpointId, connectionLifecycleCallback);
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
                    Log.i("ENDPOINT", "Endpoint initiated connection");

                    endpointUUIDs.put(endpointId, connectionInfo.getEndpointName());
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.i("ENDPOINT", "Endpoint connected");

                        connectedEndpoints.add(endpointId);
                        String endpointUUID = endpointUUIDs.get(endpointId);

                        // Send graph to newly connected node
                        BrzPacket graphPacket = BrzPacketBuilder.graphResponse(graph, hostNode, endpointUUID);
                        connectionsClient.sendPayload(endpointId, payloadBuffer.getStreamPayload(graphPacket.toJSON()));
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    Log.i("ENDPOINT", "Endpoint disconnected!");

                    // Remove edge from our graph
                    connectedEndpoints.remove(endpointId);
                    String endpointUUID = endpointUUIDs.get(endpointId);
                    graph.removeEdge(hostNode.id, endpointUUID);

                    BrzStateStore.getStore().removeChat(endpointUUID);

                    // Broadcast disconnect event
                    BrzNode node1 = hostNode;
                    BrzNode node2 = graph.getVertex(endpointUUIDs.get(endpointId));
                    if (node1 != null && node2 != null)
                        broadcast(BrzPacketBuilder.graphEvent(false, node1, node2));
                }
            };
}
