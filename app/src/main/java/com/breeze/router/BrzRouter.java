package com.breeze.router;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.EventEmitter;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.handlers.BrzFileInfoPktHandler;
import com.breeze.router.handlers.BrzGraphHandler;
import com.breeze.router.handlers.BrzHandshakeHandler;
import com.breeze.router.handlers.BrzMessageHandler;
import com.breeze.router.handlers.BrzMessageReceiptHandler;
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private List<BrzRouterHandler> handlers = new ArrayList<>();

    private Map<String, String> endpointUUIDs = new HashMap<>();
    private Map<String, String> endpointIDs = new HashMap<>();

    public Map<Long, Payload> pendingFilePayloads = new HashMap<>();
    public Map<Long, Payload> completedFilePayloads = new HashMap<>();
    public Map<Long, BrzPacket> fileInfoPackets = new HashMap<>();

    private BrzGraph graph;
    public BrzNode hostNode = null;

    private BrzRouter(ConnectionsClient cc, String pkgName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;
        this.graph = BrzGraph.getInstance();

        // Initalize handlers
        this.handlers.add(new BrzGraphHandler(this));
        this.handlers.add(new BrzMessageHandler(this));
        this.handlers.add(new BrzHandshakeHandler(this));
        this.handlers.add(new BrzFileInfoPktHandler(this));
        this.handlers.add(new BrzMessageReceiptHandler());
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
        Payload p = payloadBuffer.getStreamPayload(packet.toJSON());

        for (String id : endpointIDs.values()) {
            if (!id.equals(ignoreEndpoint))
                connectionsClient.sendPayload(id, p);
        }
    }

    public void send(BrzPacket packet) {
        // Encrypt the packet first
        BreezeAPI api = BreezeAPI.getInstance();
        try {
            api.encryption.encryptPacket(packet);
            forwardPacket(packet, true);
        } catch (Exception e) {
            Log.i("ENDPOINT", "Failed to encrypt and send packet");
        }
    }

    public void sendFilePayload(Payload filePayload, BrzPacket packet) {
        if (filePayload == null || packet == null)
            return;

        BrzFileInfo fileInfoPacket = packet.fileInfoPacket();

        String nextHopUUID = graph.nextHop(this.hostNode.id, fileInfoPacket.destinationUUID);

        if (nextHopUUID == null) {
            Log.i("ENDPOINT_ERROR", "Failed to find path to " + fileInfoPacket.destinationUUID);
        }
        BrzNode nextHopNode = this.graph.getVertex(nextHopUUID);

        connectionsClient.sendPayload(nextHopNode.endpointId, filePayload);

        // FileInfoPackets arrive at each hop along the way, to guide file payloads
        // throughout network traversal
        packet.to = nextHopUUID;
        send(packet);
    }

    public void sendToEndpoint(BrzPacket packet, String endpointId) {
        connectionsClient.sendPayload(endpointId, payloadBuffer.getStreamPayload(packet.toJSON()));
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

        Payload p = payloadBuffer.getStreamPayload(packet.toJSON());
        if (addToBuffer)
            payloadBuffer.addOutgoing(p, 5000, 5, sendPayload);
        else
            sendPayload.accept(p);
    }

    public void handleFilePayload(long payloadId) {
        // BYTES and FILE could be received in any order, so we call when either the
        // BYTES or the FILE
        // payload is completely received. The file payload is considered complete only
        // when both have
        // been received.
        Payload filePayload = completedFilePayloads.get(payloadId);
        BrzPacket packet = fileInfoPackets.get(payloadId);
        if (filePayload != null && packet != null) {
            completedFilePayloads.remove(payloadId);
            fileInfoPackets.remove(payloadId);

            BrzFileInfo fileInfo = packet.fileInfoPacket();

            // Send off to sendFilePayload, if this is not the destination uuid
            if (!hostNode.id.equals(fileInfo.destinationUUID)) {
                sendFilePayload(filePayload, packet);
            } else {
                // Get the received file (which will be in the Downloads folder)
                File payloadFile = filePayload.asFile().asJavaFile();

                // Rename the file.
                payloadFile.renameTo(new File(payloadFile.getParentFile(), fileInfo.fileName));

                Log.i("ENDPOINT", "Received and Saved Payload file to downloads folder");
            }
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
        // startDiscovery();
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
        });
    }

    public void stopDiscovery() {
        connectionsClient.stopDiscovery();
        isDiscovering = false;
        Log.i("ENDPOINT", "Stopped discovering successfully!");
    }

    private void handlePacket(BrzPacket packet, String fromEndpointId) {
        Log.i("ENDPOINT", "Got a packet " + packet.type);

        // Continue the broadcast if the packet has not been seen before
        if (packet.broadcast && !this.payloadBuffer.broadcastSeen(packet.id)) {

            this.payloadBuffer.addBroadcast(packet.id);
            broadcast(packet, fromEndpointId);

            for (BrzRouterHandler handler : this.handlers)
                if (handler.handles(packet.type))
                    handler.handle(packet, fromEndpointId);

        }

        // If the packet is not a valid broadcast
        // and it is for the host node
        else if (packet.to.equals(this.hostNode.id)) {

            // Decrypt the packet unless it's not an encryptable type
            if (packet.type != BrzPacket.BrzPacketType.GRAPH_QUERY) {
                BreezeAPI api = BreezeAPI.getInstance();
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
        public void onPayloadReceived(@NonNull String endpointId, Payload payload) {
            if (payload.getType() == Payload.Type.FILE) {
                pendingFilePayloads.put(payload.getId(), payload);
            } else {
                payloadBuffer.addIncoming(payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
            if (update.getStatus() != PayloadTransferUpdate.Status.SUCCESS)
                return;
            Long payloadId = update.getPayloadId();

            // Incomming packet was a success!
            if (payloadBuffer.isIncomming(payloadId)) {
                Log.i("ENDPOINT", "Received a payload");
                Payload payload = payloadBuffer.popIncoming(payloadId);

                BrzPacket packet = new BrzPacket(payloadBuffer.getStreamString(payload));
                handlePacket(packet, endpointId);
            } else if (pendingFilePayloads.containsKey(payloadId)) { // If it's a File Payload
                Payload payload = pendingFilePayloads.get(payloadId);
                if (payload != null) {
                    pendingFilePayloads.remove(payloadId);
                    completedFilePayloads.put(payloadId, payload);
                    handleFilePayload(payloadId);
                }
            }

            // Outgoing packet was a success!
            else {
                payloadBuffer.removeOutgoing(payloadId);
            }
        }
    };

    // Callback for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, DiscoveredEndpointInfo info) {
            if (info.getServiceId().equals(pkgName)) {
                Log.i("ENDPOINT", "Endpoint found");
                connectionsClient.requestConnection(hostNode.id, endpointId, connectionLifecycleCallback);
                emit("endpointFound", endpointId);
            }
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.i("ENDPOINT", "Endpoint lost callback fired");
            emit("endpointDisconnected", endpointId);
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

                String endpointUUID = endpointUUIDs.get(endpointId);
                endpointIDs.put(endpointUUID, endpointId);

                // Send graph to newly connected node
                BrzPacket graphPacket = BrzPacketBuilder.graphResponse(graph, hostNode, endpointUUID);
                connectionsClient.sendPayload(endpointId, payloadBuffer.getStreamPayload(graphPacket.toJSON()));
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Log.i("ENDPOINT", "Endpoint disconnected!");
            emit("endpointDisconnected", endpointId);

            BrzNode lostNode = graph.getVertex(endpointUUIDs.get(endpointId));
            if (hostNode != null && lostNode != null) {
                String endpointUUID = endpointUUIDs.get(endpointId);
                endpointIDs.remove(endpointUUID);
                // graph.removeEdge(hostNode.id, endpointUUID);

                // removes the vertex and any associated edges
                graph.removeVertex(endpointUUID);

                BrzStateStore.getStore().removeChat(endpointUUID);

                // Broadcast disconnect event
                broadcast(BrzPacketBuilder.graphEvent(false, hostNode, lostNode));
            }
        }
    };
}
