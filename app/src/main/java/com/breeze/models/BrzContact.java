package com.breeze.models;

import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.packets.BrzSerializable;
import org.json.JSONObject;


public class BrzContact implements BrzSerializable
{

    private int id;
    private String name;
    private String alias;
    private String signature;

    public BrzContact()
    {

    }

    public BrzContact(int id, String name, String alias, String signature) {
        this.id = id;
        this.name = name;
        this.alias = alias;
        this.signature = signature;
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

    @Override
    public String toString() {
        return "BrzContact{" +
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
