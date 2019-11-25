package com.breeze.packets.ChatEvents;

import android.util.Log;

import com.breeze.datatypes.BrzChat;
import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzChatHandshake implements BrzSerializable {

    public String from = "";
    public BrzChat chat = null;
    public String publicKey = "";
    public String privateKey = "";

    public BrzChatHandshake(String json) {
        this.fromJSON(json);
    }

    public BrzChatHandshake(String from, BrzChat chat, String publicKey, String privateKey) {
        this.from = from;
        this.chat = chat;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);

            JSONObject chat = jObj.getJSONObject("chat");
            this.chat = new BrzChat(chat.toString());

            this.privateKey = jObj.getString("privateKey");
            this.publicKey = jObj.getString("publicKey");
            this.from = jObj.getString("from");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "BrzChatHandshake", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("privateKey", this.privateKey);
            jObj.put("publicKey", this.publicKey);
            jObj.put("from", this.from);
            jObj.put("chat", new JSONObject(this.chat.toJSON()));
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzChatHandshake", e);
        }

        return jObj.toString();
    }


}
