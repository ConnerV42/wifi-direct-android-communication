package com.breeze.packets;

import android.util.Log;
import org.json.JSONObject;

public class BrzPacket implements BrzSerializable {

    enum BrzPacketType {
        MESSAGE, CONN_UPDATE
    }

    public String to = "BROADCAST";
    public BrzPacketType type = BrzPacketType.MESSAGE;
    private String body = "";

    public BrzPacket(BrzSerializable body) {
        this.body = body.toJSON();
    }
    public BrzPacket(String json) {
        this.fromJSON(json);
    }


    public BrzBodyMessage message() {
        return new BrzBodyMessage(this.body);
    }

    @Override
    public String toJSON() {

        JSONObject json = new JSONObject();

        try {
            json.put("to", this.to);
            json.put("type", this.type);
            json.put("body", this.body);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", e.toString());
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);

            this.to = jObj.getString("to");
            this.type = BrzPacketType.valueOf(jObj.getString("type"));
            this.body = jObj.getString("body");

        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
