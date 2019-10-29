package com.breeze.graph;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class BrzGraph {

    private static BrzGraph instance;
    private String localCodeName;
    private Map<BrzVertex, List<BrzVertex>> adjVertices;

    private BrzGraph(String localCodeName) {
        this.adjVertices = new HashMap<>(); // Implicit for: HashMap<Vertex, List<Vertex>>
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
        adjVertices.putIfAbsent(new BrzVertex(codeName), new ArrayList<>());
    }

    public void removeVertex(String codeName) {
        BrzVertex v = new BrzVertex(codeName);
        adjVertices.values().stream().forEach(e -> e.remove(v));
        adjVertices.remove(new BrzVertex(codeName));
    }

    public void addEdge(String codeName1, String codeName2) {
        BrzVertex v1 = new BrzVertex(codeName1);
        BrzVertex v2 = new BrzVertex(codeName2);

        adjVertices.get(v1).add(v2);
        adjVertices.get(v2).add(v1);
    }

    public void removeEdge(String codeName1, String codeName2) {
        BrzVertex v1 = new BrzVertex(codeName1);
        BrzVertex v2 = new BrzVertex(codeName2);

        List<BrzVertex> edgeV1 = adjVertices.get(v1);
        List<BrzVertex> edgeV2 = adjVertices.get(v2);

        if (edgeV1 != null)
            edgeV1.remove(v2);
        if (edgeV2 != null)
            edgeV2.remove(v1);
    }

    List<BrzVertex> getAdjVertices(String codeName) {
        return adjVertices.get(new BrzVertex(codeName));
    }
}
