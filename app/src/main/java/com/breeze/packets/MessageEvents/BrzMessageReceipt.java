package com.breeze.packets.MessageEvents;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzMessageReceipt implements BrzSerializable {

    public static enum ReceiptType {DELIVERED, READ}

    public String from = "";
    public String chatId = "";
    public String messageId = "";
    public ReceiptType type = null;

    public BrzMessageReceipt() {
    }

    public BrzMessageReceipt(String json) {
        this.fromJSON(json);
    }

    public BrzMessageReceipt(String from, String chatId, ReceiptType type) {
        this.from = from;
        this.chatId = chatId;
        this.type = type;
    }


    @Override
    public String toJSON() {
        JSONObject jobj = new JSONObject();
        try {
            jobj.put("from", this.from);
            jobj.put("chatId", this.chatId);
            jobj.put("messageId", this.messageId);
            jobj.put("type", this.type);
        } catch (Exception e) {
            Log.e("BrzMessageReceipt", "Serialization error", e);
        }

        return jobj.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jobj = new JSONObject(json);
            this.from = jobj.getString("from");
            this.chatId = jobj.getString("chatId");
            this.messageId = jobj.getString("messageId");
            this.type = ReceiptType.valueOf(jobj.getString("type"));
        } catch (Exception e) {
            Log.e("BrzMessageReceipt", "Deserialization error", e);
        }
    }
}
