package com.breeze.packets.graph;

import android.util.Log;

import com.breeze.graph.BrzGraph;
import com.breeze.packets.BrzSerializable;
import org.json.JSONObject;

public class BrzGraphQuery implements BrzSerializable {

    public enum BrzGQType {
        REQUEST,
        RESPONSE
    }

    public BrzGQType type = BrzGQType.REQUEST;
    public String from = "";
    public String graph = "";

    public BrzGraphQuery () {}
    public BrzGraphQuery (String json) { this.fromJSON(json); }
    public BrzGraphQuery (Boolean isRequest, String from) {
        this.type = BrzGQType.REQUEST;
        this.from = from;
    }
    public BrzGraphQuery (Boolean isRequest, String from, String graphJSON) {
        this.type = BrzGQType.RESPONSE;
        this.from = from;
        this.graph = graphJSON;
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.from = jObj.getString("from");
            this.type = BrzGQType.valueOf(jObj.getString("type"));
            this.graph = jObj.getString("graph");
            if(this.graph == null) this.graph = "";
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("from", this.from);
            jObj.put("type", this.type);
            if(!this.graph.equals("")) jObj.put("graph", this.graph);
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }

        return jObj.toString();
    }
}
