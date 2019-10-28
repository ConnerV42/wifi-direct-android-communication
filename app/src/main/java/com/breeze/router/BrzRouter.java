package com.breeze.router;

import android.util.Log;

import com.breeze.CodenameGenerator;
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

    // Our randomly generated unique name for advertising
    private final String codeName = CodenameGenerator.generate();

    private static BrzRouter instance;

    private BrzRouter(ConnectionsClient cc, String pkgName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;

        // Begin discovery!
        this.start();
    }

    public static BrzRouter getInstance(ConnectionsClient cc, String pkgName) {
        if(instance == null) instance = new BrzRouter(cc, pkgName);
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
                        //logs.append("Sent message to: " + endpointId + "\n");
                        //display message to UI
                    }
                }
            };

    // Callback for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    //logs.append("onEndpointFound: endpointId " + endpointId + " found, connecting\n");
                    connectionsClient.requestConnection(codeName, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    //logs.append("onEndpointLost: endpointId " + endpointId + " disconnected\n");
                }
            };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    //logs.append("onConnectionInitiated: accepting connection with endpointId " + endpointId + "\n");
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {

                        BrzStateStore store = BrzStateStore.getStore();
                        store.addChat(new BrzChat(endpointId, endpointId));

                        //logs.append("onConnectionResult: connection successful with endpointId " + endpointId + "\n");
                        connectedNodes.add(endpointId);
                    } else {
                        //logs.append("onConnectionResult: connection failed with endpointId " + endpointId + "\n");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    //logs.append("onDisconnected: disconnected from endpointId " + endpointId + "\n");
                    connectedNodes.remove(endpointId);
                }
            };

}
