package com.breeze.packets;

import android.util.Log;

import org.json.JSONObject;

public class BrzBodyMessage implements BrzSerializable {

    public String message = "";
    public String userName = "";
    public boolean isStatus = false;
    public Long datestamp = (long) 0;

    public BrzBodyMessage() {}
    public BrzBodyMessage(String json) {
      this.fromJSON(json);
    }
    public BrzBodyMessage(String message, boolean isStatus) {
      this.message = message;
      this.isStatus = isStatus;
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("message", this.message);
            json.put("userName", this.userName);
            json.put("datestamp", this.datestamp);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", e.toString());
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);

            this.message = jObj.getString("message");
            this.userName = jObj.getString("userName");
            this.datestamp = jObj.getLong("datestamp");

        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}