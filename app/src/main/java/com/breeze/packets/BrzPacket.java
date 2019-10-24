package com.breeze.packets;

import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

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

            String body64 = Base64.encodeToString(this.body.getBytes(), Base64.DEFAULT);
            json.put("body", body64);
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

            String body64 = jObj.getString("body");
            byte[] bodyBytes = Base64.decode(body64, Base64.DEFAULT);
            this.body = new String(bodyBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
