package com.breeze.datatypes;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BrzChat implements BrzSerializable {

    public String id = UUID.randomUUID().toString();
    public String name = "";

    public List<String> nodes = new ArrayList<>();
    public boolean isGroup = false;

    public BrzChat() {
    }

    public BrzChat(String json) {
        this.fromJSON(json);
    }

    public BrzChat(String name, String nodeId) {
        this.name = name;
        this.nodes.add(nodeId);
        this.isGroup = false;
    }


    public BrzChat(String name, List<String> nodes) {
        this.name = name;
        this.nodes = nodes;
        this.isGroup = true;
    }

    public void nodesFromJson(String nodeStr) {
        try {
            JSONArray nodeArr = new JSONArray(nodeStr);
            this.nodes = new ArrayList<>();
            for (int i = 0; i < nodeArr.length(); i++) this.nodes.add(nodeArr.getString(i));
        } catch (Exception e) {
            Log.e("DESERIALIZATION ERROR", "BrzChat", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("id", this.id);
            json.put("name", this.name);
            json.put("nodes", new JSONArray(this.nodes));
            json.put("isGroup", this.isGroup);
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzChat", e);
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);

            this.id = jObj.getString("id");
            this.name = jObj.getString("name");

            nodesFromJson(jObj.getString("nodes"));

            this.isGroup = jObj.getBoolean("isGroup");
        } catch (Exception e) {
            Log.e("DESERIALIZATION ERROR", "BrzChat", e);
        }
    }
}
