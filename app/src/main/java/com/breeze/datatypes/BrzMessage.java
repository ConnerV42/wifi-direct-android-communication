package com.breeze.datatypes;

import android.util.Log;
import com.breeze.encryption.BrzEncryption;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzMessage implements BrzSerializable {

    public String from = "";
    public String body = "";
    public String chatId = "";

    public boolean isStatus = false;
    public Long datestamp = (long) 0;

    public BrzMessage() {}
    public BrzMessage(String json) {
      this.fromJSON(json);
    }
    public BrzMessage(String body, boolean isStatus) {
      this.body = body;
      this.isStatus = isStatus;
    }
    public BrzMessage(String body, String from) {
      this.body = body;
      this.from = from;
      this.isStatus = false;
    }

    public String signMessageWithPrivateKey() throws Exception {
        byte[] arr = this.body.getBytes();
        byte[] ret = null;
        try {
            ret = BrzEncryption.signWithPrivateKey(arr);
        }catch(Exception e)
        {
            Log.i("private key signing error","blah");
        }
        if(ret == null)
        {
            throw new Exception("Bad signature from private key signing");
        }
        return new String(ret);
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("from", this.from);
            json.put("body", this.body);
            json.put("chatId", this.chatId);
            json.put("isStatus", this.isStatus);
            json.put("datestamp", this.datestamp);
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzMessage", e);
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);

            this.from = jObj.getString("from");
            this.body = jObj.getString("body");
            this.chatId = jObj.getString("chatId");
            this.isStatus = jObj.getBoolean("isStatus");
            this.datestamp = jObj.getLong("datestamp");

        } catch (Exception e) {
            Log.e("DESERIALIZATION ERROR", "BrzMessage", e);
        }
    }
}