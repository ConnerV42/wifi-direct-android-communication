package com.breeze.router;

import android.widget.TextView;

import com.breeze.CodenameGenerator;
import com.breeze.packets.BrzBodyMessage;
import com.breeze.packets.BrzPacket;
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

    private String pkgName = "";
    private TextView logs;

    // Our randomly generated unique name for advertising
    private final String codeName = CodenameGenerator.generate();


    public BrzRouter(ConnectionsClient cc, String pkgName, TextView logs) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;
        this.logs = logs;

        // Begin discovery!
        startAdvertising();
        startDiscovery();
    }

    public void broadcast(BrzPacket packet) {
        for(String id : connectedNodes) {
            packet.to = id;
            this.send(packet);
        }
    }

    public void send(BrzPacket packet) {
        Payload p = Payload.fromBytes(packet.toJSON().getBytes());
        connectionsClient.sendPayload(packet.to, p);
    }

    public void disconnect() {
        connectionsClient.stopAllEndpoints();
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

                    String json = new String(payload.asBytes(), UTF_8);
                    BrzPacket packet = new BrzPacket(json);
                    BrzBodyMessage message = packet.message();

                    logs.append("Received message: " + message.message + " from " + message.userName + "\n");
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if(update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        //display message to UI
                    }
                }
            };

    // Callback for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    logs.append("onEndpointFound: endpointId " + endpointId + " found, connecting\n");
                    connectionsClient.requestConnection(codeName, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    logs.append("onEndpointLost: endpointId " + endpointId + " disconnected\n");
                }
            };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    logs.append("onConnectionInitiated: accepting connection with endpointId " + endpointId + "\n");
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        logs.append("onConnectionResult: connection successful with endpointId " + endpointId + "\n");
                        connectedNodes.add(endpointId);
                    } else {
                        logs.append("onConnectionResult: connection failed with endpointId " + endpointId + "\n");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    logs.append("onDisconnected: disconnected from endpointId " + endpointId + "\n");
                    connectedNodes.remove(endpointId);
                }
            };

}
