package com.breeze.datatypes;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BrzChat implements BrzSerializable {

    public String id = "";
    public String name = "";

    public List<String> nodes = new ArrayList<>();
    public boolean isGroup = false;

    public BrzChat() {}
    public BrzChat(String id, String name, String nodeId) {
        this.id = id;
        this.name = name;
        this.nodes.add(nodeId);
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

            JSONArray nodeArr = jObj.getJSONArray("nodes");
            this.nodes = new ArrayList<>();
            for(int i = 0; i < nodeArr.length(); i++) this.nodes.add(nodeArr.getString(i));

            this.isGroup = jObj.getBoolean("isGroup");
        } catch (Exception e) {
            Log.e("DESERIALIZATION ERROR", "BrzChat", e);
        }
    }
}
