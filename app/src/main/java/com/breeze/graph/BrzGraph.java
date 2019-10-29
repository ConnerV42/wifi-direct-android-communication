package com.breeze.graph;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class BrzGraph {

    private static BrzGraph instance;
    private String localCodeName;
    private Map<String, List<String>> adjVertices;

    private BrzGraph(String localCodeName) {
        this.adjVertices = new HashMap<>(); // Implicit for: HashMap<String, List<String>>
        this.localCodeName = localCodeName;
        this.addVertex(localCodeName);
    }

    public static BrzGraph getInstance(String localCodeName) {
        if(instance == null) instance = new BrzGraph(localCodeName);
        return instance;
    }

    public static BrzGraph getInstance() {
        return instance;
    }

    public int getSize() { // vertices count
        return adjVertices.size();
    }

    public void addVertex(String codeName) {
        adjVertices.putIfAbsent(codeName, new ArrayList<>());
    }

    public void removeVertex(String codeName) {
        adjVertices.values().stream().forEach(e -> e.remove(codeName));
        adjVertices.remove(codeName);
    }

    public void addEdge(String codeName1, String codeName2) {
        adjVertices.get(codeName1).add(codeName2);
        adjVertices.get(codeName2).add(codeName1);
    }

    public void removeEdge(String codeName1, String codeName2) {
        List<String> edgeV1 = adjVertices.get(codeName1);
        List<String> edgeV2 = adjVertices.get(codeName2);

        if (edgeV1 != null)
            edgeV1.remove(codeName2);
        if (edgeV2 != null)
            edgeV2.remove(codeName1);
    }

    List<String> getAdjVertices(String codeName) {
        return adjVertices.get(codeName);
    }
}
