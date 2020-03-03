package com.breeze.router;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.EventEmitter;
import com.breeze.application.BreezeAPI;
import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.handlers.BrzAliasNameUpdateHandler;
import com.breeze.router.handlers.BrzFileInfoPktHandler;
import com.breeze.router.handlers.BrzGraphHandler;
import com.breeze.router.handlers.BrzHandshakeHandler;
import com.breeze.router.handlers.BrzLiveAudioHandler;
import com.breeze.router.handlers.BrzMessageHandler;
import com.breeze.router.handlers.BrzMessageReceiptHandler;
import com.breeze.router.handlers.BrzProfileHandler;
import com.breeze.router.handlers.BrzPublicMessageHandler;
import com.breeze.router.handlers.BrzRouterHandler;
import com.breeze.router.handlers.BrzRouterStreamHandler;
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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class BrzRouter extends EventEmitter {

    // Single instance

    private static BrzRouter instance;

    public static BrzRouter initialize(Context ctx, String pkgName) {
        if (instance == null)
            instance = new BrzRouter(Nearby.getConnectionsClient(ctx), pkgName);
        return instance;
    }

    public static BrzRouter getInstance() {
        return instance;
    }

    // --------------------------------------------------------------------------//

    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private ConnectionsClient connectionsClient;
    private String pkgName;

    private BrzPayloadBuffer payloadBuffer = new BrzPayloadBuffer();
    private BrzStreamBuffer streamBuffer = new BrzStreamBuffer();

    private List<BrzRouterHandler> handlers = new ArrayList<>();

    private Map<String, String> endpointUUIDs = new HashMap<>();
    private Map<String, String> endpointIDs = new HashMap<>();

    private BreezeAPI api;
    private BrzGraph graph;
    public BrzNode hostNode = null;

    private BrzRouter(ConnectionsClient cc, String pkgName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;

        this.api = BreezeAPI.getInstance();
        this.graph = api.getGraph();

        // Initalize handlers
        this.handlers.add(new BrzGraphHandler(this));
        this.handlers.add(new BrzMessageHandler(this));
        this.handlers.add(new BrzHandshakeHandler(this));
        this.handlers.add(new BrzFileInfoPktHandler(this));
        this.handlers.add(new BrzMessageReceiptHandler());
        this.handlers.add(new BrzProfileHandler(this));
        this.handlers.add(new BrzPublicMessageHandler(this));
        this.handlers.add(new BrzAliasNameUpdateHandler(this));
        this.handlers.add(new BrzLiveAudioHandler());
    }

    //
    //
    // Public access packet sending handlers
    //
    //

    public void broadcast(BrzPacket packet) {
        this.broadcast(packet, "");
    }

    public void broadcast(BrzPacket packet, String ignoreEndpoint) {
        packet.to = "BROADCAST";
        packet.broadcast = true;
        Payload p = BrzPayloadBuffer.getStreamPayload(packet.toJSON());

        for (String id : endpointIDs.values()) {
            if (!id.equals(ignoreEndpoint))
                connectionsClient.sendPayload(id, p);
        }
    }

    public void send(BrzPacket packet) {
        // Encrypt the packet first
        try {
            api.encryption.encryptPacket(packet);
            forwardPacket(packet, true);
        } catch (Exception e) {
            Log.i("ENDPOINT", "Failed to encrypt and send packet");
        }
    }

    public void sendStream(BrzPacket packet, InputStream stream) {
        if (packet == null || stream == null || !packet.hasStream())
            throw new RuntimeException("Invalid input");

        this.sendStream(packet, Payload.fromStream(stream));
    }

    private void sendStream(long payloadId) {
        BrzPacket packet = this.streamBuffer.getPacket(payloadId);
        InputStream stream = this.streamBuffer.getStream(payloadId);

        if (packet == null || stream == null)
            throw new RuntimeException("The buffer does not have this stream");

        this.sendStream(packet, Payload.fromStream(stream));
        this.streamBuffer.removeStream(payloadId);
    }

    public void sendStream(BrzPacket packet, Payload streamPayload) {
        packet.stream.filePayloadId = streamPayload.getId();

        // If the host is connected directly with the destination, shortcut it
        String endpointID = endpointIDs.get(packet.to);
        if (endpointID != null) {
            connectionsClient.sendPayload(endpointID, BrzPayloadBuffer.getStreamPayload(packet.toJSON()));
            connectionsClient.sendPayload(endpointID, streamPayload);
            return;
        }

        // Get the next hop from the graph
        String nextHopUUID = graph.nextHop(this.hostNode.id, packet.to);
        if (nextHopUUID == null) {
            Log.i("ENDPOINT_ERR", "Failed to find path to " + packet.to);
            return;
        }
        BrzNode nextHopNode = this.graph.getVertex(nextHopUUID);

        if (nextHopNode != null && !nextHopNode.endpointId.equals("")
                && endpointIDs.values().contains(nextHopNode.endpointId)) {
            connectionsClient.sendPayload(nextHopNode.endpointId, BrzPayloadBuffer.getStreamPayload(packet.toJSON()));
            connectionsClient.sendPayload(nextHopNode.endpointId, streamPayload);
        }
    }

    private void forwardPacket(BrzPacket packet, Boolean addToBuffer) {

        Consumer<Payload> sendPayload = payload -> {

            // If the host is connected directly with the destination, shortcut it
            String endpointID = endpointIDs.get(packet.to);
            if (endpointID != null) {
                connectionsClient.sendPayload(endpointID, payload);
                return;
            }

            // Get the next hop from the graph
            String nextHopUUID = graph.nextHop(this.hostNode.id, packet.to);
            if (nextHopUUID == null) {
                Log.i("ENDPOINT_ERR", "Failed to find path to " + packet.to);
                return;
            }
            BrzNode nextHopNode = this.graph.getVertex(nextHopUUID);

            if (nextHopNode != null && !nextHopNode.endpointId.equals("")
                    && endpointIDs.values().contains(nextHopNode.endpointId))
                connectionsClient.sendPayload(nextHopNode.endpointId, payload);
        };

        Payload p = BrzPayloadBuffer.getStreamPayload(packet.toJSON());
        if (addToBuffer)
            payloadBuffer.addOutgoing(p, 5000, 5, sendPayload);
        else
            sendPayload.accept(p);
    }

    public void handleFilePayload(long payloadId) {
        if (!streamBuffer.isStreamPayload(payloadId))
            throw new RuntimeException("Payload ID entered was not in the stream buffer");

        BrzPacket packet = this.streamBuffer.getPacket(payloadId);
        InputStream stream = this.streamBuffer.getStream(payloadId);

        if (packet == null || stream == null)
            throw new RuntimeException("The buffer does not have this stream");

        // Packet is for the host
        if (packet.to.equals(hostNode.id)) {
            for (BrzRouterHandler handler : this.handlers)
                if (handler instanceof BrzRouterStreamHandler && handler.handles(packet.type))
                    ((BrzRouterStreamHandler) handler).handleStream(packet, stream);

            this.streamBuffer.removeStream(payloadId);
        }

        // Packet is for someone else
        else {
            this.sendStream(payloadId);
        }
    }

//
//
// Lifecycle
//
//

    public void start(BrzNode hostNode) {
        if (hostNode == null)
            return;

        this.hostNode = hostNode;
        this.graph.setVertex(hostNode);

        this.start();
    }

    public void start() {
        if (this.hostNode == null)
            return;

        Log.i("ENDPOINT", "Starting advertising and discovery");

        startAdvertising();
    }

    public void stop() {
        Log.i("ENDPOINT", "Stopping nearby completely");

        connectionsClient.stopAllEndpoints();
        connectionsClient.stopAdvertising();

        this.stopDiscovery();
    }

    private void startAdvertising() {
        connectionsClient.startAdvertising(this.hostNode.id, pkgName, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build()).addOnSuccessListener(e -> {
            Log.i("ENDPOINT", "Advertising successfully!");
        }).addOnFailureListener(e -> {
            Log.i("ENDPOINT", "Advertising failed!", e);
        });
    }

    public Boolean isDiscovering = false;

    public void startDiscovery() {
        connectionsClient.startDiscovery(pkgName, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build()).addOnSuccessListener(e -> {
            Log.i("ENDPOINT", "Discovering successfully!");
            isDiscovering = true;
        }).addOnFailureListener(e -> {
            Log.i("ENDPOINT", "Discovering failed!", e);
            isDiscovering = false;
        });
    }

    public void stopDiscovery() {
        connectionsClient.stopDiscovery();
        isDiscovering = false;
        Log.i("ENDPOINT", "Stopped discovering successfully!");
    }

    private LinkedList<String> discoveredEndpoints = new LinkedList<>();
    public void scan() {
        if(isDiscovering) return;

        startDiscovery();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stopDiscovery();

                // Initiate connection with the endpoints discovered during the scan
                for (String endpointId : discoveredEndpoints) {
                    connectionsClient.requestConnection(hostNode.id, endpointId, connectionLifecycleCallback);
                }
                discoveredEndpoints = new LinkedList<>();
            }
        }, 10 * 1000);
    }


    private void handlePacket(BrzPacket packet, String fromEndpointId) {
        Log.i("ENDPOINT", "Got a packet " + packet.type);

        // Continue the broadcast if the packet has not been seen before
        if (packet.broadcast && !this.payloadBuffer.broadcastSeen(packet.id)) {
            boolean handled = false;

            for (BrzRouterHandler handler : this.handlers)
                if (handler.handles(packet.type)) {
                    boolean handlerDidHandle = handler.handle(packet, fromEndpointId);
                    handled = handlerDidHandle || handled;
                }

            if (!handled) {
                this.payloadBuffer.addBroadcast(packet.id);
                broadcast(packet, fromEndpointId);
            }
        }

        // If the packet is not a valid broadcast
        // and it is for the host node
        else if (packet.to.equals(this.hostNode.id)) {
            List<BrzNode> blockedNodes = BreezeAPI.getInstance().state.getAllBlockedNodes();
            for(BrzNode node : blockedNodes){
                if(node.endpointId.equals(fromEndpointId)){
                    Log.i("ENDPOINT", "Received a payload from a blocked node, not opening");
                    return;
                }
            }
            // Decrypt the packet unless it's not an encryptable type
            if (packet.type != BrzPacket.BrzPacketType.GRAPH_QUERY) {
                api.encryption.decryptPacket(packet);
            }

            // Then pass it to a handler
            for (BrzRouterHandler handler : this.handlers)
                if (handler.handles(packet.type))
                    handler.handle(packet, fromEndpointId);
        }

        // If the packet is to be forwarded
        else {
            this.forwardPacket(packet, true);
        }
    }

//
//
// Connections client callbacks
//
//

    // Callback for receiving payloads
    private final PayloadCallback payloadCallback = new PayloadCallback() {

        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            Log.i("ENDPOINT", "Received a payload");
            // This is a raw stream, not a packet
            if (streamBuffer.isStreamPayload(payload.getId())) {
                streamBuffer.addStream(payload);
                handleFilePayload(payload.getId());
                return;
            }

            // Otherwise it's a normal packet
            BrzPacket packet = new BrzPacket(BrzPayloadBuffer.getStreamString(payload));

            // This is a packet with a stream attached
            if (packet.hasStream())
                streamBuffer.addPacket(packet);

                // Or just a normal packet
            else handlePacket(packet, endpointId);
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
            if (update.getStatus() != PayloadTransferUpdate.Status.SUCCESS)
                return;

            Long payloadId = update.getPayloadId();
            if (payloadBuffer.getOutgoing(payloadId) != null)
                payloadBuffer.removeOutgoing(payloadId);
        }
    };

    // Callback for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, DiscoveredEndpointInfo info) {
            if (info.getServiceId().equals(pkgName)) {
                Log.i("ENDPOINT", "Endpoint found");

                // If the endpoint is not already connected, initiate a connection
                if (endpointUUIDs.get(endpointId) == null) {
                    discoveredEndpoints.add(endpointId);
                    emit("endpointFound", endpointId);
                }

            }
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.i("ENDPOINT", "Endpoint lost callback fired");
            emit("endpointDisconnected", endpointId);
            discoveredEndpoints.remove(endpointId);
        }
    };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {

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
                emit("endpointConnected", endpointId);

                // Update uuid -> endpoinId map
                String endpointUUID = endpointUUIDs.get(endpointId);
                endpointIDs.put(endpointUUID, endpointId);

                // Send graph to newly connected node
                BrzPacket graphPacket = BrzPacketBuilder.graphResponse(graph, hostNode, endpointUUID);
                connectionsClient.sendPayload(endpointId, BrzPayloadBuffer.getStreamPayload(graphPacket.toJSON()));
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Log.i("ENDPOINT", "Endpoint disconnected!");
            emit("endpointDisconnected", endpointId);

            String endpointUUID = endpointUUIDs.get(endpointId);
            endpointUUIDs.remove(endpointId);
            endpointIDs.remove(endpointUUID);

            BrzNode lostNode = graph.getVertex(endpointUUID);
            if (hostNode != null && lostNode != null) {
                // removes the vertex and any associated edges
                graph.removeEdge(endpointUUID, hostNode.id);
                graph.removeDisconnected(api.hostNode.id);

                // Broadcast disconnect event
                broadcast(BrzPacketBuilder.graphEvent(false, hostNode, lostNode), endpointId);
            }
        }
    };
}
