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
import com.breeze.packets.LiveConnectionEvents.BrzLiveConnectionRequest;
import com.breeze.router.handlers.BrzFileInfoPktHandler;
import com.breeze.router.handlers.BrzGraphHandler;
import com.breeze.router.handlers.BrzHandshakeHandler;
import com.breeze.router.handlers.BrzMessageHandler;
import com.breeze.router.handlers.BrzPublicMessageHandler;
import com.breeze.router.handlers.BrzMessageReceiptHandler;
import com.breeze.router.handlers.BrzProfileHandler;
import com.breeze.router.handlers.BrzRouterHandler;
import com.breeze.router.handlers.BrzRouterStreamHandler;
import com.breeze.router.handlers.LiveConnectionHandlers.BrzLiveConnectionEventHandler;
import com.breeze.router.handlers.LiveConnectionHandlers.BrzLiveConnectionReadyHandler;
import com.breeze.router.handlers.LiveConnectionHandlers.BrzLiveConnectionRequestHandler;
import com.breeze.state.BrzStateStore;
import com.breeze.streams.BrzLiveAudioConsumer;
import com.breeze.views.UserSelection.UserList;
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
import com.google.android.gms.tasks.Task;

import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;

import java.io.File;
import java.io.InputStream;
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
    private BrzStreamBuffer streamBuffer = new BrzStreamBuffer();

    private List<BrzRouterHandler> handlers = new ArrayList<>();

    private Map<String, String> endpointUUIDs = new HashMap<>();
    private Map<String, String> endpointIDs = new HashMap<>();

    public BrzGraph graph;
    public BrzNode hostNode = null;

    private BrzRouter(ConnectionsClient cc, String pkgName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;
        this.graph = BrzGraph.getInstance();

        // Initalize handlers
        this.handlers.add(new BrzGraphHandler(this));
        this.handlers.add(new BrzMessageHandler(this));
        this.handlers.add(new BrzPublicMessageHandler(this));
        this.handlers.add(new BrzHandshakeHandler(this));
        this.handlers.add(new BrzFileInfoPktHandler(this));
        this.handlers.add(new BrzMessageReceiptHandler());
        this.handlers.add(new BrzLiveConnectionEventHandler());
        this.handlers.add(new BrzLiveConnectionReadyHandler());
        this.handlers.add(new BrzLiveConnectionRequestHandler());
        this.handlers.add(new BrzProfileHandler(this));
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

    public void sendAudioStream(String to, Payload p){
        connectionsClient.sendPayload(to, p);
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

    public void sendStream(BrzPacket packet, InputStream stream) {
        if (packet == null || stream == null || !packet.hasStream())
            throw new RuntimeException("Invalid input");

        Payload streamPayload = Payload.fromStream(stream);
        packet.stream.filePayloadId = streamPayload.getId();

        this.streamBuffer.addPacketPayload(packet);
        this.streamBuffer.addStreamPayload(streamPayload);

        this.sendStream(streamPayload.getId());
    }

    private void sendStream(long payloadId) {
        BrzPacket packet = this.streamBuffer.getPacketPayload(payloadId);
        Payload streamPayload = this.streamBuffer.getStreamPayload(payloadId);

        if (packet == null || streamPayload == null)
            throw new RuntimeException("The buffer does not have this stream");

        // If the host is connected directly with the destination, shortcut it
        String endpointID = endpointIDs.get(packet.to);
        if (endpointID != null) {
            connectionsClient.sendPayload(endpointID, BrzPayloadBuffer.getStreamPayload(packet.toJSON()));
            connectionsClient.sendPayload(endpointID, streamPayload);
            this.streamBuffer.removeStream(payloadId);
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
            this.streamBuffer.removeStream(payloadId);
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

            Log.i("forwardPacket Graph State", graph.toString());

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
        BrzPacket packet = this.streamBuffer.getPacketPayload(payloadId);
        Payload streamPayload = this.streamBuffer.getStreamPayload(payloadId);

        if (packet == null || streamPayload == null)
            throw new RuntimeException("The buffer does not have this stream");

        // Packet is for the host
        if (packet.to.equals(hostNode.id)) {
            for (BrzRouterHandler handler : this.handlers)
                if (handler instanceof BrzRouterStreamHandler && handler.handles(packet.type))
                    ((BrzRouterStreamHandler) handler).handleStream(packet, streamPayload.asStream().asInputStream());

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
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            Log.i("ENDPOINT", "Received a payload");

            // This is a raw stream, not a packet
            if (streamBuffer.isStreamPayload(payload.getId())) {
                streamBuffer.addStreamPayload(payload);
                handleFilePayload(payload.getId());
                return;
            }
                

                try{
                    BreezeAPI api = BreezeAPI.getInstance();
                    
                    //Check if any audio consumers are waiting on this payload
                    if(api.state.consumerReady(payload.getId() + '_' + endpointId)){
                        BrzLiveAudioConsumer audioConsumer = api.state.getConsumer(payload.getId() + '_' + endpointId);
                        api.streams.consumeAudioProducer(audioConsumer, payload.asStream().asInputStream());
                        return;
                    }
                    
                }catch(Exception e){
                    e.printStackTrace();
                } finally {
                    // Otherwise it's a normal packet
            BrzPacket packet = new BrzPacket(BrzPayloadBuffer.getStreamString(payload));

            // This is a packet with a stream attached
            if (packet.hasStream())
                streamBuffer.addPacketPayload(packet);
                // Or just a normal packet
            else handlePacket(packet, endpointId);   
                }
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
                Log.i("ENDPOINT", info.getEndpointName());
                stopDiscovery();
                Task t = connectionsClient.requestConnection(hostNode.id, endpointId, connectionLifecycleCallback);
                Log.i("ENDPOINT", t.toString());
                Log.i("ENDPOINT", "emmitting found endpoint ");
                emit("endpointFound", endpointId);
            }
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.i("ENDPOINT", "Endpoint lost callback fired");
            connectionsClient.rejectConnection(endpointId);
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

                Log.i("Connection Event Graph State", graph.toString());

                // Send graph to newly connected node
                BrzPacket graphPacket = BrzPacketBuilder.graphResponse(graph, hostNode, endpointUUID);
                connectionsClient.sendPayload(endpointId, BrzPayloadBuffer.getStreamPayload(graphPacket.toJSON()));
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

                // removes the vertex and any associated edges
                graph.removeVertex(endpointUUID);

                Log.i("Disconnection Event Graph State", graph.toString());

                BrzStateStore.getStore().removeChat(endpointUUID);

                // Broadcast disconnect event
                broadcast(BrzPacketBuilder.graphEvent(false, hostNode, lostNode));
            }
        }
    };
}
