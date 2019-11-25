package com.breeze.graph;

import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzSerializable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class BrzGraph implements BrzSerializable, Iterable<BrzNode> {

    private Map<String, Set<String>> adjList = new HashMap<>();
    private Map<String, BrzNode> vertexList = new HashMap<>();

    private BrzGraph() {
    }

    // Singleton control

    private static BrzGraph instance = new BrzGraph();

    public static BrzGraph getInstance() {
        return instance;
    }

    public static BrzGraph replaceInstance(String json) {
        instance.fromJSON(json);
        return instance;
    }

    // Graph manipulation methods

    public List<String> bfs(String currentUUID, String destinationUUID) {

        HashMap<String, Boolean> nodeVisited = new HashMap<String, Boolean>();
        vertexList.keySet().forEach(id -> nodeVisited.put(id, false));

        // List that holds the path through which a node has been reached
        List<String> pathToNode = new ArrayList<>();
        pathToNode.add(currentUUID);

        Queue<List<String>> queue = new LinkedList<>();
        queue.add(pathToNode);

        while (!queue.isEmpty()) {
            pathToNode = queue.poll();
            String currId = pathToNode.get(pathToNode.size() - 1);

            if (currId.equals(destinationUUID))
                return pathToNode;

            for (String neighbor : getNeighbors(currId)) {
                Boolean visited = nodeVisited.get(neighbor);
                if (!neighbor.equals(currId) && (visited == null || !visited)) {
                    nodeVisited.put(neighbor, true);

                    // Create new collection representing the path to the next node
                    List<String> pathToNextNode = new ArrayList<>(pathToNode);
                    pathToNextNode.add(neighbor);
                    queue.add(pathToNextNode);
                }
            }
        }
        return null;
    }

    public String nextHop(String currentUUID, String destinationUUID) {
        List<String> path = this.bfs(currentUUID, destinationUUID);
        if (path == null) return null;
        if(path.size() < 2) return destinationUUID;
        return path.get(1);
    }

    public int getSize() {
        return vertexList.size();
    }

    public BrzNode getVertex(String id) {
        return vertexList.get(id);
    }

    public void addVertex(BrzNode node) {
        vertexList.putIfAbsent(node.id, node);
        adjList.putIfAbsent(node.id, new HashSet<>());
    }

    public void setVertex(BrzNode node) {
        vertexList.put(node.id, node);
        adjList.putIfAbsent(node.id, new HashSet<>());
    }

    public void removeVertex(String id) {
        adjList.values().forEach(e -> e.remove(id));
        adjList.remove(id);
        vertexList.remove(id);
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
        return new ArrayList<>(adjList.get(id));
    }

    public void mergeGraph(String graphJSON) {
        BrzGraph otherGraph = new BrzGraph();
        otherGraph.fromJSON(graphJSON);

        // Merge vertex lists
        for (BrzNode n : otherGraph.vertexList.values()) {
            BrzNode myVersion = this.vertexList.get(n.id);

            // Merge only new information into our node
            if (myVersion != null) {
                if (
                        (myVersion.endpointId == null || myVersion.endpointId.isEmpty()) &&
                                (n.endpointId != null && !n.endpointId.isEmpty())
                ) myVersion.endpointId = n.endpointId;

                if (
                        (myVersion.publicKey == null || myVersion.publicKey.isEmpty()) &&
                                (n.publicKey != null && !n.publicKey.isEmpty())
                ) myVersion.publicKey = n.publicKey;

                if (
                        (myVersion.name == null || myVersion.name.isEmpty()) &&
                                (n.name != null && !n.name.isEmpty())
                ) myVersion.name = n.name;

                if (
                        (myVersion.alias == null || myVersion.alias.isEmpty()) &&
                                (n.alias != null && !n.alias.isEmpty())
                ) myVersion.alias = n.alias;

            } else {
                this.addVertex(n);
            }
        }

        // Add missing edges
        for (String nodeId : otherGraph.adjList.keySet()) {
            adjList.putIfAbsent(nodeId, new HashSet<>());
            for (String edgeStr : otherGraph.adjList.get(nodeId)) {
                adjList.get(nodeId).add(edgeStr);
            }
        }
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
}
