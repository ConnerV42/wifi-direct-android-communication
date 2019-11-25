package com.breeze.packets;

import android.util.Log;

import org.json.JSONObject;

public class BrzFileInfo implements BrzSerializable {

    public String filePayloadId = "";
    public String fileName = "";
    public String from = "";
    public String destinationUUID = "";
    public String userName = "";

    public Long datestamp = (long) 0;

    public BrzFileInfo() {}
    public BrzFileInfo(String json) {
        this.fromJSON(json);
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("filePayloadId", this.filePayloadId);
            json.put("fileName", this.fileName);
            json.put("from", this.from);
            json.put("destinationUUID", this.destinationUUID);
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

            this.filePayloadId = jObj.getString("filePayloadId");
            this.fileName = jObj.getString("fileName");
            this.from = jObj.getString("from");
            this.userName = jObj.getString("userName");
            this.datestamp = jObj.getLong("datestamp");

        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
