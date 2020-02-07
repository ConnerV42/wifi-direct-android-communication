package com.breeze.datatypes;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

import java.util.UUID;

public class BrzNode implements BrzSerializable {

    public String id = "";
    public String endpointId = "";
    public String publicKey = "";

    public String name = "";
    public String alias = "";

    public BrzNode(String id, String endpointId, String publicKey, String name, String alias) {
        this.id = id;
        this.endpointId = endpointId;
        this.publicKey = publicKey;
        this.name = name;
        this.alias = alias;
    }

    public BrzNode(String json) {
        this.fromJSON(json);
    }

    public BrzNode() {
    }

    public void generateID() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("endpointId", endpointId);
            json.put("publicKey", publicKey);

            json.put("name", name);
            json.put("alias", alias);

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

            this.name = jObj.getString("name");
            this.alias = jObj.getString("alias");

        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
