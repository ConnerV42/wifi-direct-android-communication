package com.breeze.router;

import android.util.Log;

import com.breeze.packets.BrzMessage;
import com.breeze.graph.BrzGraph;
import com.breeze.graph.BrzNode;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
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
    private Map<String, String> endpointNames = new HashMap<>();


    private boolean running = false;
    private String pkgName = "";
    public final String id = UUID.randomUUID().toString();
    private BrzGraph graph;


    private boolean waitingForGraph = false;
    private static BrzRouter instance;

    private BrzRouter(ConnectionsClient cc, String pkgName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;
        this.graph = BrzGraph.getInstance();

        this.graph.addVertex(new BrzNode(id, "", "localhost", ""));

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
        //BrzMessage message = packet.message();
        //message.userName = "You";

        //BrzStateStore store = BrzStateStore.getStore();

        //for(String id : connectedNodes) {
        //    packet.to = id;
        //    store.addMessage(id, message);
        //    this.send(packet);
        //}
    }

    public void send(BrzPacket packet) {
        Payload p = Payload.fromBytes(packet.toJSON().getBytes());
        BrzNode toNode = this.graph.getVertex(packet.to);
        if(toNode != null && !toNode.endpointId.equals("")) {
            connectionsClient.sendPayload(toNode.endpointId, p);

            buffer.addPacket(packet, 5000, 5, packResend -> {
                connectionsClient.sendPayload(toNode.endpointId, p);
            });

        }
    }
    private void sendAck(BrzPacket packet) {
        Payload p = Payload.fromBytes(packet.toJSON().getBytes());
        BrzNode toNode = this.graph.getVertex(packet.to);
        if(toNode != null && !toNode.endpointId.equals("")) {
            connectionsClient.sendPayload(toNode.endpointId, p);
        }
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

        if(packet.type != BrzPacket.BrzPacketType.ACK) {
            BrzPacket ack = BrzPacketBuilder.ack(packet, this.endpointNames.get(fromEndpointId));
            sendAck(ack);
        } else {
            this.buffer.removePacket(packet.id);
        }

        if(packet.type == BrzPacket.BrzPacketType.MESSAGE) {
            if(packet.to.equals(this.id)) {
                BrzMessage message = packet.message();
                BrzStateStore store = BrzStateStore.getStore();
                store.addMessage(message.from, message);
            } else {
                // forward the packet to next node
            }
        }

        else if(packet.type == BrzPacket.BrzPacketType.GRAPH_QUERY) {
            BrzGraphQuery query = packet.graphQuery();

            // Respond to graph query
            if(query.type == BrzGraphQuery.BrzGQType.REQUEST) {
                BrzPacket resPacket = BrzPacketBuilder.graphResponse(this.graph, query.from);
                send(resPacket);
            }
            // Update graph
            else if(waitingForGraph && !query.graph.equals("")) {
                waitingForGraph = false;
                this.graph.fromJSON(query.graph);
            }
        }
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

                    handlePacket(packet, endpointId);
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
                    if(info.getServiceId().equals(pkgName))
                        connectionsClient.requestConnection(id, endpointId, connectionLifecycleCallback);
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
                    endpointNames.put(endpointId, connectionInfo.getEndpointName());
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        String endpointName = endpointNames.get(endpointId);
                        graph.addVertex(new BrzNode(endpointName, endpointId, "other_host", ""));
                        graph.addEdge(id, endpointName);

                        if(connectedEndpoints.size() == 0) {
                            waitingForGraph = true;
                            BrzPacket reqPacket = BrzPacketBuilder.graphQuery(endpointName, id);
                            send(reqPacket);
                        }
                        connectedEndpoints.add(endpointId);

                        BrzStateStore store = BrzStateStore.getStore();
                        store.addChat(new BrzChat(endpointName, endpointName));
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    connectedEndpoints.remove(endpointId);

                    String endpointName = endpointNames.get(endpointId);
                    graph.removeEdge(id, endpointName);
                    graph.removeVertex(endpointName);

                    BrzStateStore store = BrzStateStore.getStore();
                    store.removeChat(endpointName);
                }
            };
}
