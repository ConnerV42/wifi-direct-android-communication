package com.breeze.graph;

import android.util.Log;

import com.breeze.packets.BrzSerializable;
import com.breeze.packets.BrzUser;

import org.json.JSONObject;

public class BrzNode implements BrzSerializable {

    public String id = "";
    public String endpointId = "";
    public String publicKey = "";

    public BrzUser user;

    public BrzNode(String id, String endpointId, String publicKey, BrzUser user) {
        this.id = id;
        this.endpointId = endpointId;
        this.publicKey = publicKey;
        this.user = user;
    }

    public BrzNode(String json) {
        this.fromJSON(json);
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("endpointId", endpointId);
            json.put("user", new JSONObject(user.toJSON()));
            json.put("publicKey", publicKey);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", e.toString());
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.id = jObj.getString("id");
            this.endpointId = jObj.getString("endpointId");
            this.publicKey = jObj.getString("publicKey");
            JSONObject userJSON = jObj.getJSONObject("user");
            this.user = new BrzUser(userJSON.toString());
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
