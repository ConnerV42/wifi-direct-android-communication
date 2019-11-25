package com.breeze.packets.GraphEvents;

import android.util.Log;

import com.breeze.packets.BrzSerializable;
import org.json.JSONObject;

public class BrzGraphQuery implements BrzSerializable {

    public enum BrzGQType {
        REQUEST,
        RESPONSE
    }

    public BrzGQType type = BrzGQType.REQUEST;
    public String from = "";

    // If it's a response
    public String graph = "";
    public String hostNode = "";

    public BrzGraphQuery () {}
    public BrzGraphQuery (String json) { this.fromJSON(json); }
    public BrzGraphQuery (Boolean isRequest, String from) {
        this.type = BrzGQType.REQUEST;
        this.from = from;
    }
    public BrzGraphQuery (Boolean isRequest, String from, String graphJSON, String hostNodeJSON) {
        this.type = BrzGQType.RESPONSE;
        this.from = from;
        this.graph = graphJSON;
        this.hostNode = hostNodeJSON;
    }



    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.from = jObj.getString("from");
            this.type = BrzGQType.valueOf(jObj.getString("type"));

            try {
                this.graph = jObj.getString("graph");
            } catch (Exception e) {
                this.graph = "";
            }

            try {
                this.hostNode = jObj.getString("hostNode");
            } catch (Exception e) {
                this.hostNode = "";
            }

        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "err", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("from", this.from);
            jObj.put("type", this.type);
            if(!this.graph.equals("")) jObj.put("graph", this.graph);
            if(!this.hostNode.equals("")) jObj.put("hostNode", this.hostNode);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", "err", e);
        }

        return jObj.toString();
    }
}
