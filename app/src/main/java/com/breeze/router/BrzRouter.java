package com.breeze.router;

import android.widget.TextView;

import com.breeze.CodenameGenerator;
import com.breeze.packets.BrzBodyMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
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


    public BrzRouter(ConnectionsClient cc, String pkgName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;

        BrzStateStore store = BrzStateStore.getStore();
        ArrayList<BrzBodyMessage> messages = (ArrayList) store.getVal("messages/messages");

        if(messages == null) {
            messages = new ArrayList<BrzBodyMessage>();
            store.setVal("messages/messages", messages);
        }

        /*for(int i = 0; i < 5; i++) {
            BrzBodyMessage message = new BrzBodyMessage();
            message.userName = "" + i;
            message.message = "yeet";
            messages.add(message);
        }
        store.setVal("messages/messages", messages);
*/
        // Begin discovery!
        this.start();
    }

    public void broadcast(BrzPacket packet) {

        BrzBodyMessage message = packet.message();
        message.userName = "You";

        BrzStateStore store = BrzStateStore.getStore();
        store.addMessage(message);

        for(String id : connectedNodes) {
            packet.to = id;
            this.send(packet);
        }
    }

    public void send(BrzPacket packet) {
        Payload p = Payload.fromBytes(packet.toJSON().getBytes());
        connectionsClient.sendPayload(packet.to, p);
    }

    public void start() {
        if(running) return;
        running = true;

        startAdvertising();
        startDiscovery();

        BrzStateStore store = BrzStateStore.getStore();
        store.addMessage(new BrzBodyMessage("Searching for Breeze nodes...", true));
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

                    BrzStateStore store = BrzStateStore.getStore();
                    store.addMessage(message);

                    //logs.append("Received message: " + message.message + " from " + message.userName + "\n");
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
                        store.addMessage(new BrzBodyMessage("Found device: " + endpointId, true));

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
