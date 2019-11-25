package com.breeze.dbmodels;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class DBBrzProfile implements BrzSerializable
{

    private int id;
    private String name;
    private String alias;
    private String signature;

    private String lastTalkedTo;
    private boolean friend;
    private boolean blocked;

    public DBBrzProfile(int id, String name, String alias, String signature) {
        this.id = id;
        this.name = name;
        this.alias = alias;
        this.signature = signature;
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String isoDate = df.format(new Date());
        this.friend = false;
        this.blocked = false;
        this.lastTalkedTo = isoDate;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getLastTalkedTo() {
        return lastTalkedTo;
    }

    public void setLastTalkedTo(String lastTalkedTo) {
        this.lastTalkedTo = lastTalkedTo;
    }

    public boolean isFriend() {
        return friend;
    }

    public void setFriend(boolean friend) {
        this.friend = friend;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    @Override
    public String toString() {
        return "DBBrzProfile{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", alias='" + alias + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("name", this.name);
            json.put("alias", this.alias);
            json.put("signature", this.signature);
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
            this.name = jObj.getString("name");
            this.alias = jObj.getString("alias");
            this.signature = jObj.getString("signature");

        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
