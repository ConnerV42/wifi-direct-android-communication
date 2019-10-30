package com.breeze.graph;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzNode implements BrzSerializable {
    public String id = "";
    public String name = "";
    public String publicKey = "";

    public BrzNode(String id, String name, String publicKey) {
        this.id = id;
        this.name = name;
        this.publicKey = publicKey;
    }

    public BrzNode(String json) {
        this.fromJSON(json);
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
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
            this.name = jObj.getString("name");
            this.publicKey = jObj.getString("publicKey");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
