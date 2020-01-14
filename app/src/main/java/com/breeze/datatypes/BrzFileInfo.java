package com.breeze.datatypes;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzFileInfo implements BrzSerializable {

    public long filePayloadId = (long) 0;
    public String fileName = "";
    public String initialVector = "";

    public BrzFileInfo() { }

    public BrzFileInfo(long filePayloadId, String fileName, String initialVector) {
        this.filePayloadId = filePayloadId;
        this.fileName = fileName;
        this.initialVector = initialVector;
    }

    public BrzFileInfo(String json) {
        this.fromJSON(json);
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("filePayloadId", this.filePayloadId);
            json.put("fileName", this.fileName);
            json.put("initialVector", this.initialVector);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", e.toString());
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.filePayloadId = jObj.getLong("filePayloadId");
            this.initialVector = jObj.getString("initialVector");
            this.fileName = jObj.getString("fileName");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
