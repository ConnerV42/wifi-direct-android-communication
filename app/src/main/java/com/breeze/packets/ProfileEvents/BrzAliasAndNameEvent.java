package com.breeze.packets.ProfileEvents;

import android.util.Log;

import com.breeze.packets.BrzSerializable;
import com.breeze.packets.GraphEvents.BrzGraphQuery;

import org.json.JSONObject;

public class BrzAliasAndNameEvent implements BrzSerializable{
    public String from = "";
    public String name = "";
    public String alias = "";

    public BrzAliasAndNameEvent (String from, String name, String alias) {
        this.from = from;
        this.name = name;
        this.alias = alias;
    }

    public BrzAliasAndNameEvent(String json) {
        this.fromJSON(json);
    }
    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.from = jObj.getString("from");
            this.name = jObj.getString("name");
            this.alias = jObj.getString("alias");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "BrzAliasAndNameEvent", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("from", this.from);
            jObj.put("nodeId", this.name);
            jObj.put("request", this.alias);
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzAliasAndNameEvent", e);
        }

        return jObj.toString();
    }



}
