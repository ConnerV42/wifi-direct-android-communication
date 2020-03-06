package com.breeze.packets.ChatEvents;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzChatNameAndImageEvent implements BrzSerializable {

    public String from = "";
    public String name = "";
    public boolean request = true;

    public BrzChatNameAndImageEvent(String from, String name, boolean request) {
        this.from = from;
        this.name = name;
        this.request = request;
    }
    public BrzChatNameAndImageEvent(String json) {
        this.fromJSON(json);
    }


    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.from = jObj.getString("from");
            this.name = jObj.getString("nodeId");
            this.request = jObj.getBoolean("request");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "BrzChatNameAndImageEvent", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("from", this.from);
            jObj.put("nodeId", this.name);
            jObj.put("request", this.request);
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzChatNameAndImageEvent", e);
        }

        return jObj.toString();
    }
}
