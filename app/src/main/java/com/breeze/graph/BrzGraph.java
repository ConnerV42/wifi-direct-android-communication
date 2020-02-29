package com.breeze.graph;

import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.EventEmitter;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzSerializable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class BrzGraph extends EventEmitter implements BrzSerializable, Iterable<BrzNode> {

    private Map<String, Set<String>> adjList = new HashMap<>();
    private Map<String, BrzNode> vertexList = new HashMap<>();

    // Graph manipulation methods

    public BrzGraph() {
    }

    public BrzGraph(String json) {
        this.fromJSON(json);
    }

    public List<String> bfs(String startId, String destinationId) {
        HashMap<String, Boolean> visited = new HashMap<>();
        Queue<List<String>> queue = new LinkedList<>();

        // List that holds the path through which a node has been reached
        List<String> pathToNode = new LinkedList<>();

        visited.put(startId, true);
        queue.add(pathToNode);
        pathToNode.add(startId);

        while (!queue.isEmpty()) {
            pathToNode = queue.poll();
            String currId = pathToNode.get(pathToNode.size() - 1);

            if (currId.equals(destinationId)) return pathToNode;

            List<String> neighbors = getNeighbors(currId);
            if (neighbors == null) continue;
            for (String neighbor : neighbors) {
                if (visited.get(neighbor) == null) {
                    visited.put(neighbor, true);

                    // Create new collection representing the path to the next node
                    List<String> pathToNextNode = new LinkedList<>(pathToNode);
                    pathToNextNode.add(neighbor);
                    queue.add(pathToNextNode);
                }
            }
        }
        return null;
    }

    public HashMap<String, Boolean> getConnected(String startId) {
        HashMap<String, Boolean> visited = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        visited.put(startId, true);
        queue.add(startId);

        while (!queue.isEmpty()) {
            String currId = queue.poll();

            List<String> neighbors = getNeighbors(currId);
            if (neighbors == null) continue;

            for (String neighbor : neighbors) {
                if (visited.get(neighbor) == null) {
                    visited.put(neighbor, true);
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }

    public void removeDisconnected(String startId) {
        HashMap<String, Boolean> connected = getConnected(startId);
        for (String vertex : new LinkedList<>(this.adjList.keySet())) {
            if (connected.get(vertex) == null) {
                adjList.values().forEach(e -> e.remove(vertex));
                adjList.remove(vertex);
                vertexList.remove(vertex);
                this.emit("deleteVertex", vertex);
            }
        }
    }

    public String nextHop(String currentUUID, String destinationUUID) {
        List<String> path = this.bfs(currentUUID, destinationUUID);
        if (path == null) return null;
        if (path.size() <= 2) return destinationUUID;
        return path.get(1);
    }

    public int getSize() {
        return vertexList.size();
    }

    public BrzNode getVertex(String id) {
        return vertexList.get(id);
    }

    public Collection<BrzNode> getNodeCollection() {
        return this.vertexList.values();
    }

    public Set<String> getVertexIds() {
        return this.vertexList.keySet();
    }

    public Set<String> getEdgeIds() {
        return this.adjList.keySet();
    }

    public void addVertex(BrzNode node) {
        vertexList.putIfAbsent(node.id, node);
        adjList.putIfAbsent(node.id, new HashSet<>());
        this.emit("addVertex", node);
    }

    public void setVertex(BrzNode node) {
        vertexList.put(node.id, node);
        adjList.putIfAbsent(node.id, new HashSet<>());
        this.emit("setVertex", node);
    }

    public void removeVertex(String id) {
        adjList.values().forEach(e -> e.remove(id));
        adjList.remove(id);
        vertexList.remove(id);
        this.emit("deleteVertex", id);
    }

    public void addEdge(String id1, String id2) {
        Set<String> edges1 = adjList.get(id1);
        Set<String> edges2 = adjList.get(id2);
        if (edges1 != null) edges1.add(id2);
        if (edges2 != null) edges2.add(id1);
    }

    public void removeEdge(String id1, String id2) {
        Set<String> edges1 = adjList.get(id1);
        Set<String> edges2 = adjList.get(id2);
        if (edges1 != null) edges1.remove(id2);
        if (edges2 != null) edges2.remove(id1);
    }

    private List<String> getNeighbors(String id) {
        Set<String> neighbors = adjList.get(id);
        if (neighbors == null) return null;
        return new LinkedList<>(neighbors);
    }

    public BrzGraph diff(String graphJSON) {
        BrzGraph otherGraph = new BrzGraph();
        return diff(otherGraph);
    }

    public BrzGraph diff(BrzGraph otherGraph) {
        // First clone the other graph
        BrzGraph g = new BrzGraph(otherGraph.toJSON());

        // Remove nodes we already know about
        for (BrzNode n : vertexList.values()) {
            g.removeVertex(n.id);
        }

        // Return only the new stuff
        return g;
    }

    // A version that ensures a particular edge also exists on the diff
    public BrzGraph diff(BrzGraph otherGraph, BrzNode hostNode, BrzNode otherNode) {
        BrzGraph diff = this.diff(otherGraph);
        diff.setVertex(hostNode);
        diff.setVertex(otherNode);
        diff.addEdge(hostNode.id, otherNode.id);

        diff.removeDisconnected(hostNode.id);

        return diff;
    }

    public void mergeGraph(String graphJSON) {
        BrzGraph otherGraph = new BrzGraph(graphJSON);
        this.mergeGraph(otherGraph);
    }

    public void mergeGraph(BrzGraph otherGraph) {
        // Merge vertex lists
        for (BrzNode n : otherGraph.vertexList.values()) {
            this.addVertex(n);
        }

        // Add missing edges
        for (String nodeId : otherGraph.adjList.keySet()) {
            Set<String> currentNeighbors = adjList.get(nodeId);
            if (currentNeighbors == null) {
                currentNeighbors = new HashSet<>();
                adjList.put(nodeId, currentNeighbors);
            }

            List<String> neighbors = otherGraph.getNeighbors(nodeId);
            if (neighbors == null) continue;
            currentNeighbors.addAll(neighbors);
        }

        this.emit("graphMerge");
    }

    @NonNull
    @Override
    public Iterator<BrzNode> iterator() {
        return this.vertexList.values().iterator();
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {

            JSONObject adjMap = new JSONObject();
            for (String key : adjList.keySet()) {
                adjMap.put(key, new JSONArray(adjList.get(key)));
            }

            JSONObject vertexMap = new JSONObject();
            for (String key : vertexList.keySet()) {
                BrzNode vertex = vertexList.get(key);
                if (vertex != null)
                    vertexMap.put(key, new JSONObject(vertex.toJSON()));
            }

            json.put("adjList", adjMap);
            json.put("vertexList", vertexMap);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", "err", e);
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        adjList = new HashMap<>();
        vertexList = new HashMap<>();

        try {
            JSONObject jObj = new JSONObject(json);

            // Copy adjList from json
            JSONObject adjMap = jObj.getJSONObject("adjList");
            Iterator<String> adjKeys = adjMap.keys();
            while (adjKeys.hasNext()) {
                String key = adjKeys.next();
                Set<String> edgeList = new HashSet<>();
                JSONArray edges = adjMap.getJSONArray(key);
                for (int i = 0; i < edges.length(); i++)
                    edgeList.add(edges.getString(i));

                adjList.put(key, edgeList);
            }

            // Copy vertexList from json
            JSONObject vertexMap = jObj.getJSONObject("vertexList");
            Iterator<String> vertexKeys = vertexMap.keys();
            while (vertexKeys.hasNext()) {
                String key = vertexKeys.next();
                JSONObject vertexJSON = vertexMap.getJSONObject(key);
                vertexList.put(key, new BrzNode(vertexJSON.toString()));
            }
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "err", e);
        }
    }

    @NonNull
    public String toString() {
        String graph = "";
        for (String nodeId : getEdgeIds()) {
            graph += "Node " + nodeId + "'s Adjacent Nodes: [";

            List<String> neighbors = getNeighbors(nodeId);
            if (neighbors == null) graph += "]\n";
            else {
                for (String n : neighbors) graph += n + ", ";
                graph += "]\n";
            }
        }
        return graph;
    }

    public void logOnDevice() {
        this.log("sdcard/log.file");
        ;
    }

    private void log(String path) {
        File logFile = new File(path);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(this.toString());
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
