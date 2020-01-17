package com.breeze.packets.ProfileEvents;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzProfileResponse implements BrzSerializable {

    public String from = "";

    public BrzProfileResponse(String from, boolean isJson) {
        if (isJson)
            this.fromJSON(from);
        else
            this.from = from;
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.from = jObj.getString("from");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "BrzProfileResponse", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("from", this.from);
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzProfileResponse", e);
        }

        return jObj.toString();
    }
}
