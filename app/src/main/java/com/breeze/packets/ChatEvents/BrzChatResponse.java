package com.breeze.packets.ChatEvents;

import android.util.Log;

import com.breeze.datatypes.BrzChat;
import com.breeze.packets.BrzSerializable;
import org.json.JSONObject;

public class BrzChatResponse implements BrzSerializable {

    // If it's a response
    public String from = "";
    public String chatId = "";
    public Boolean accepted = false;

    public BrzChatResponse(String json) {
        this.fromJSON(json);
    }
    public BrzChatResponse(String from, String chatId, Boolean accepted) {
        this.from = from;
        this.chatId = chatId;
        this.accepted = accepted;
    }


    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.accepted = jObj.getBoolean("accepted");
            this.chatId = jObj.getString("chatId");
            this.from = jObj.getString("from");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "BrzChatReponse", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("accepted", this.accepted);
            jObj.put("chatId", this.chatId);
            jObj.put("from", this.from);
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzChatReponse", e);
        }

        return jObj.toString();
    }


}
