package com.breeze.router;

import android.util.Log;

import com.breeze.CodenameGenerator;
import com.breeze.graph.BrzGraph;
import com.breeze.packets.BrzBodyMessage;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzPacket;
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

import static java.nio.charset.StandardCharsets.UTF_8;

public class BrzRouter {

    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private ConnectionsClient connectionsClient;
    private ArrayList<String> connectedNodes = new ArrayList<String>();

    private boolean running = false;
    private String pkgName = "";
    private String codeName = "";
    private BrzGraph graph;

    private static BrzRouter instance;

    private BrzRouter(ConnectionsClient cc, String pkgName, String codeName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;
        this.codeName = codeName;
        this.graph = BrzGraph.getInstance(codeName);

        // Begin discovery!
        this.start();
    }

    public static BrzRouter getInstance(ConnectionsClient cc, String pkgName, String codeName) {
        if(instance == null) instance = new BrzRouter(cc, pkgName, codeName);
        return instance;
    }

    public static BrzRouter getInstance() {
        return instance;
    }

    public void broadcast(BrzPacket packet) {

        BrzBodyMessage message = packet.message();
        message.userName = "You";

        BrzStateStore store = BrzStateStore.getStore();

        for(String id : connectedNodes) {
            packet.to = id;
            store.addMessage(id, message);
            this.send(packet);
        }
    }

    public void send(BrzPacket packet) {
        Payload p = Payload.fromBytes(packet.toJSON().getBytes());

        BrzStateStore store = BrzStateStore.getStore();
        BrzBodyMessage message = packet.message();
        message.userName = "You";
        store.addMessage(packet.to, message);

        Log.i("Yeet", packet.to + " " + message.message);

        connectionsClient.sendPayload(packet.to, p);
    }

    public void start() {
        if(running) return;
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
        connectionsClient.startAdvertising(codeName,
                pkgName, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    private void startDiscovery() {
        connectionsClient.startDiscovery(
                pkgName, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }


    // Callback for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    byte[] payloadBody = payload.asBytes();
                    if(payloadBody == null) return;

                    String json = new String(payloadBody, UTF_8);
                    BrzPacket packet = new BrzPacket(json);
                    BrzBodyMessage message = packet.message();

                    Log.i("Yeet", endpointId + " " + message.message);

                    BrzStateStore store = BrzStateStore.getStore();
                    store.addMessage(endpointId, message);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if(update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                    }
                }
            };

    // Callback for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    connectionsClient.requestConnection(codeName, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                }
            };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        BrzStateStore store = BrzStateStore.getStore();
                        store.addChat(new BrzChat(endpointId, endpointId));

                        connectedNodes.add(endpointId);
                        graph.addVertex(endpointId);
                        graph.addEdge(codeName, endpointId);
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    BrzStateStore store = BrzStateStore.getStore();
                    store.removeChat(endpointId);

                    connectedNodes.remove(endpointId);
                    graph.removeEdge(codeName, endpointId);
                    graph.removeVertex(endpointId);
                }
            };

}
