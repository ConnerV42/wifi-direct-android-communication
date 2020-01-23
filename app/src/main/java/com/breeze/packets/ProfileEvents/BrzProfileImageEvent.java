package com.breeze.packets.ProfileEvents;

import android.util.Log;

import com.breeze.packets.BrzSerializable;
import org.json.JSONObject;

public class BrzProfileImageEvent implements BrzSerializable {

    public String from = "";
    public String nodeId = "";
    public boolean request = true;

    public BrzProfileImageEvent(String from, String nodeId, boolean request) {
        this.from = from;
        this.nodeId = nodeId;
        this.request = request;
    }
    public BrzProfileImageEvent(String json) {
        this.fromJSON(json);
    }


    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.from = jObj.getString("from");
            this.nodeId = jObj.getString("nodeId");
            this.request = jObj.getBoolean("request");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "BrzProfileImageEvent", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("from", this.from);
            jObj.put("nodeId", this.nodeId);
            jObj.put("request", this.request);
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzProfileImageEvent", e);
        }

        return jObj.toString();
    }
}
