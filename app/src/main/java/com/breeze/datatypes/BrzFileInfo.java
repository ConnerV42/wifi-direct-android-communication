package com.breeze.datatypes;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzFileInfo implements BrzSerializable {
    public String filePayloadId = "";
    public String fileName = "";
    public String fromId = "";
    public String nextId = "";
    public String destinationId = "";
    public String chatId = "";
    public Long datestamp = (long) 0;

    public BrzFileInfo() { }

    public BrzFileInfo(String fromId, String chatId, String filePayloadId, String fileName, long datestamp) {
        this.fromId = fromId;
        this.nextId = "";
        this.chatId = chatId;
        this.destinationId = "";
        this.filePayloadId = filePayloadId;
        this.fileName = fileName;
        this.datestamp = datestamp;
    }

    public BrzFileInfo(String json) {
        this.fromJSON(json);
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("fromId", this.fromId);
            json.put("nextId", this.nextId);
            json.put("chatId", this.chatId);
            json.put("destinationId", this.destinationId);
            json.put("filePayloadId", this.filePayloadId);
            json.put("fileName", this.fileName);
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

            this.fromId = jObj.getString("fromId");
            this.nextId = jObj.getString("nextId");
            this.chatId = jObj.getString("chatId");
            this.destinationId = jObj.getString("destinationId");
            this.filePayloadId = jObj.getString("filePayloadId");
            this.fileName = jObj.getString("fileName");
            this.datestamp = jObj.getLong("datestamp");

        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
