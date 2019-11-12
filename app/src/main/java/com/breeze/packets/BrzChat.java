package com.breeze.packets;

import android.util.Log;

import org.json.JSONObject;

public class BrzChat implements BrzSerializable{

    public String id = "";
    public String name = "";

    public BrzChat() {}
    public BrzChat(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("id", this.id);
            json.put("name", this.name);
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
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
