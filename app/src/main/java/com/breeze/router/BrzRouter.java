package com.breeze.router;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.packets.BrzFileInfo;
import com.breeze.packets.BrzMessage;
import com.breeze.graph.BrzGraph;
import com.breeze.graph.BrzNode;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.packets.BrzUser;
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

import java.io.File;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BrzRouter {
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private ConnectionsClient connectionsClient;

    private BrzPacketBuffer buffer = new BrzPacketBuffer();

    private List<String> connectedEndpoints = new ArrayList<>();
    private Map<String, String> endpointUUIDs = new HashMap<>();

    private Map<String, String> seenPackets = new HashMap<>();
    private Map<Long, Payload> pendingPayloads = new HashMap<>();

    private Map<Long, Payload> pendingFilePayloads = new HashMap<>();
    private Map<Long, Payload> completedFilePayloads = new HashMap<>();
    private Map<Long, BrzPacket> fileInfoPackets = new HashMap<>();

    private boolean running = false;
    private String pkgName = "";
    public final String id = UUID.randomUUID().toString();
    private BrzGraph graph;
    private BrzUser user;

    private static BrzRouter instance;

    private BrzRouter(ConnectionsClient cc, String pkgName) {
        this.connectionsClient = cc;
        this.pkgName = pkgName;
        this.graph = BrzGraph.getInstance();

        Log.i("ENDPOINT", "Endpoint UUID is " + id);

        // When we log in, add our own node to the graph
        BrzStateStore.getStore().getUser(brzUser -> {
            if (brzUser == null) return;
            user = brzUser;

            Log.i("ENDPOINT_USER", brzUser.toJSON());

            BrzNode hostNode = new BrzNode(id, "", "", brzUser);
            this.graph.addVertex(hostNode);

            BrzStateStore.getStore().setUser(this.id, this.user);

            this.start();
        });
    }

    public static BrzRouter getInstance(ConnectionsClient cc, String pkgName) {
        if (instance == null) instance = new BrzRouter(cc, pkgName);
        return instance;
    }

    public static BrzRouter getInstance() {
        return instance;
    }

    public void broadcast(BrzPacket packet) {
        packet.to = "BROADCAST";
        Payload p = stringToPayload(packet.toJSON());

        for (String id : connectedEndpoints) {
            connectionsClient.sendPayload(id, p);
        }
    }

    public void broadcast(BrzPacket packet, String ignoreEndpoint) {
        packet.to = "BROADCAST";
        Payload p = stringToPayload(packet.toJSON());

        for (String id : connectedEndpoints) {
            if (!id.equals(ignoreEndpoint)) connectionsClient.sendPayload(id, p);
        }
    }

    public void sendFilePayload(Payload filePayload, BrzPacket packet) { // called in MessagesView, and BrzRouter in handleFilePayload
        if(filePayload == null || packet == null) return;

        BrzFileInfo fileInfo = packet.fileInfo();

        String nextHopUUID = graph.nextHop(this.id, fileInfo.destinationUUID);

        if (nextHopUUID == null) {
            Log.i("ENDPOINT_ERR", "Failed to find path to " + fileInfo.destinationUUID);
        }
        BrzNode nextHopNode = this.graph.getVertex(nextHopUUID);

        connectionsClient.sendPayload(nextHopNode.endpointId, filePayload);

        // FileInfoPackets arrive at each hop along the way, to guide file payload along the network
        packet.to = nextHopUUID;
        send(packet);
    }

    public void send(BrzPacket packet) {
        forwardPacket(packet);
        buffer.addPacket(packet, 5000, 5, this::forwardPacket);
    }

    private void forwardPacket(BrzPacket packet) {
        Payload p = stringToPayload(packet.toJSON());

        String nextHopUUID = graph.nextHop(this.id, packet.to);
        if (nextHopUUID == null) {
            Log.i("ENDPOINT_ERR", "Failed to find path to " + packet.to);
//            Log.i("ENDPOINT_ERR", this.graph.toJSON());
        }
        BrzNode nextHopNode = this.graph.getVertex(nextHopUUID);

        if (
                nextHopNode != null &&
                        !nextHopNode.endpointId.equals("") &&
                        this.connectedEndpoints.contains(nextHopNode.endpointId)
        ) connectionsClient.sendPayload(nextHopNode.endpointId, p);
    }

    public void start() {
        if (running || this.user == null) return;
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
        connectionsClient.startAdvertising(id,
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

    private Payload stringToPayload(String str) {
        return Payload.fromStream(new ByteArrayInputStream(str.getBytes()));
    }

    private String payloadToString(Payload p) {
        InputStream stream = p.asStream().asInputStream();
        Scanner sc = new Scanner(stream);
        StringBuffer sb = new StringBuffer();
        while (sc.hasNext()) {
            sb.append(sc.nextLine());
        }
        sc.close();
        try {
            stream.close();
        } catch (Exception e) {
        }
        return sb.toString();
    }

    private void handlePacket(BrzPacket packet, String fromEndpointId) {

        // Acknowledge packets we receive
        if (packet.type != BrzPacket.BrzPacketType.ACK) {
            BrzPacket ack = BrzPacketBuilder.ack(packet, this.endpointUUIDs.get(fromEndpointId));
            connectionsClient.sendPayload(fromEndpointId, stringToPayload(ack.toJSON()));

            // Remove acknowledged packets from the buffer
        } else {
            this.buffer.removePacket(packet.id);
        }

        Log.i("ENDPOINT", "Got a packet " + packet.type);

        // Respond to graph queries
        if (packet.type == BrzPacket.BrzPacketType.GRAPH_QUERY) {
            BrzGraphQuery query = packet.graphQuery();
            Log.i("ENDPOINT", "Got a graph query " + query.toJSON());

            // Respond to graph query
            if (query.type == BrzGraphQuery.BrzGQType.REQUEST) {
                BrzPacket resPacket = BrzPacketBuilder.graphResponse(this.graph, this.graph.getVertex(id), query.from);
                connectionsClient.sendPayload(fromEndpointId, stringToPayload(resPacket.toJSON()));
                Log.i("ENDPOINT", "Respond to graph query");
            }

            // Update graph
            else {
                Log.i("ENDPOINT", "Merging graph information");

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
                for (BrzNode n : this.graph) {
                    if (!n.id.equals(id))
                        BrzStateStore.getStore().setUser(n.id, n.user);
                }
            }
        }

        // Respond to graph queries
        if (packet.type == BrzPacket.BrzPacketType.GRAPH_EVENT) {

            BrzGraphEvent ge = packet.graphEvent();

            // Don't accept events involving yourself
            if (ge.node1.id.equals(id) || ge.node2.id.equals(id)) return;

            // If it's a new connection
            if (ge.type == BrzGraphEvent.BrzGEType.CONNECT) {
                graph.addVertex(ge.node1);
                graph.addVertex(ge.node2);
                graph.addEdge(ge.node1.id, ge.node2.id);

                // TEMP: Add users from each node to store
                for (BrzNode n : this.graph) {
                    if (!n.id.equals(id))
                        BrzStateStore.getStore().setUser(n.id, n.user);
                }

                // If it's a disconnect
            } else {
                graph.removeEdge(ge.node1.id, ge.node2.id);
            }

            if (seenPackets.get(packet.id) != null) return;
            seenPackets.put(packet.id, "");

            broadcast(packet, fromEndpointId);
        }

        // forward packets that aren't for us onwards
        else if (!packet.to.equals(this.id)) {
            send(packet);
            Log.i("ENDPOINT", "Relaying message from " + fromEndpointId + " to " + packet.to);
        }

        // If we got a message that is for us
        else if (packet.type == BrzPacket.BrzPacketType.MESSAGE) {
            BrzMessage message = packet.message();
            BrzStateStore.getStore().addMessage(message.from, message);
        }

        // If we got a fileName packet that is for us
        else if (packet.type == BrzPacket.BrzPacketType.FILE_INFO) {
            BrzFileInfo fileInfoPkt = packet.fileInfo();
            long id = Long.parseLong(fileInfoPkt.filePayloadId);
            fileInfoPackets.put(id, packet);
            handleFilePayload(id);
        }
    }

    private void handleFilePayload(long payloadId) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        Payload filePayload = completedFilePayloads.get(payloadId);
        BrzPacket packet = fileInfoPackets.get(payloadId);
        if (filePayload != null && packet != null) {
            completedFilePayloads.remove(payloadId);
            fileInfoPackets.remove(payloadId);

            BrzFileInfo fileInfo = packet.fileInfo();

            // Send off to sendFilePayload, if this is not the destination uuid
            if(!this.id.equals(fileInfo.destinationUUID)) {
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

    // Callback for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, Payload payload) {
                    if (payload.getType() != Payload.Type.FILE) {
                        pendingPayloads.put(payload.getId(), payload);
                    }
                    else {
                        pendingFilePayloads.put(payload.getId(), payload);
                    }

                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        Payload payload = pendingPayloads.get(update.getPayloadId());
                        pendingPayloads.remove(update.getPayloadId());

                        if (payload == null) {
                            payload = pendingFilePayloads.get(update.getPayloadId());
                            if(payload != null) {
                                pendingFilePayloads.remove(update.getPayloadId());
                                completedFilePayloads.put(update.getPayloadId(), payload);
                            } else {
                                return;
                            }
                        }

                        if(payload.getType() == Payload.Type.FILE) {
                            Log.i("ENDPOINT", "Got a new file payload");
                            handleFilePayload(update.getPayloadId());
                        } else {
                            Log.i("ENDPOINT", "Got a new raw packet!");

                            String json = payloadToString(payload);
                            BrzPacket packet = new BrzPacket(json);
                            handlePacket(packet, endpointId);
                        }
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

                        // Do handshake query
                        BrzPacket reqPacket = BrzPacketBuilder.graphQuery(endpointUUID, id);
                        connectionsClient.sendPayload(endpointId, stringToPayload(reqPacket.toJSON()));
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    Log.i("ENDPOINT", "Endpoint disconnected!");

                    // Remove edge from our graph
                    connectedEndpoints.remove(endpointId);
                    String endpointUUID = endpointUUIDs.get(endpointId);
                    graph.removeEdge(id, endpointUUID);

                    BrzStateStore.getStore().removeChat(endpointUUID);

                    // Broadcast disconnect event
                    BrzNode node1 = graph.getVertex(id);
                    BrzNode node2 = graph.getVertex(endpointUUIDs.get(endpointId));
                    if (node1 != null && node2 != null)
                        broadcast(BrzPacketBuilder.graphEvent(false, node1, node2));
                }
            };
}
