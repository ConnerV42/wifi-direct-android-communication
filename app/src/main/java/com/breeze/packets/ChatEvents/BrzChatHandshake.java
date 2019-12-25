package com.breeze.packets.ChatEvents;

import android.util.Log;

import com.breeze.datatypes.BrzChat;
import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzChatHandshake implements BrzSerializable {

    public String from = "";
    public BrzChat chat = null;
    public String secretKey = "";

    public BrzChatHandshake(String json) {
        this.fromJSON(json);
    }

    public BrzChatHandshake(String from, BrzChat chat) {
        this.from = from;
        this.chat = chat;
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);

            JSONObject chat = jObj.getJSONObject("chat");
            this.chat = new BrzChat(chat.toString());

            this.secretKey = jObj.getString("secretKey");
            this.from = jObj.getString("from");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "BrzChatHandshake", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("secretKey", this.secretKey);
            jObj.put("from", this.from);
            jObj.put("chat", new JSONObject(this.chat.toJSON()));
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzChatHandshake", e);
        }

        return jObj.toString();
    }


}
