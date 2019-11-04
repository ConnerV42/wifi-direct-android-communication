package com.breeze.dbmodels;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class DBBrzMessage implements BrzSerializable {

    private int id;
    private int from;
    private String username;
    private String body;
    private String datetime;
    private String encryption;

    public DBBrzMessage() {

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getId() {
        return id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }
    
    @Override
    public String toString() {
        return "DBBrzMessage{" +
                "id=" + id +
                ", from=" + from +
                ", body='" + body + '\'' +
                ", datetime=" + datetime +
                ", encryption='" + encryption + '\'' +
                '}';
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("from", this.from);
            json.put("body", this.body);
            json.put("datetime", this.datetime);
            json.put("encryption", this.encryption);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", e.toString());
        }
        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.id = Integer.parseInt(jObj.getString("id"));
            this.from = Integer.parseInt(jObj.getString("from"));
            this.body = jObj.getString("body");
            this.datetime = jObj.getString("datetime");
            this.encryption = jObj.getString("encryption");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
