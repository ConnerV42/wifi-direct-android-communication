package com.breeze.graph;

public class BrzVertex {
    private String codeName;

    public BrzVertex(String codeName) {
        this.codeName = codeName;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String getVertexCodeName() {
        return codeName;
    }

}
