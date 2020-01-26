package com.breeze.packets.LiveConnectionEvents;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

/**
 *  * This class will just update other phone that the associated BrzLiveAudioConsumer is
 *  * ready to consume audio
 */

public class BrzLiveConnectionReady implements BrzSerializable {

    private String producerEndpointID;
    private long producerPayloadID;
    private String checksum;

    public BrzLiveConnectionReady(){

    }

    public BrzLiveConnectionReady(String json) {
        this.fromJSON(json);
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("producerEndpointID", this.producerEndpointID);
            json.put("producerPayloadID", this.producerPayloadID);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", e.toString());
        }
        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.producerEndpointID = jObj.getString("producerEndpointID");
            this.producerPayloadID = jObj.getLong("producerPayloadID");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
    public String getProducerEndpointID() {
        return producerEndpointID;
    }

    public void setProducerEndpointID(String producerEndpointID) {
        this.producerEndpointID = producerEndpointID;
    }

    public long getProducerPayloadID() {
        return producerPayloadID;
    }

    public void setProducerPayloadID(long producerPayloadID) {
        this.producerPayloadID = producerPayloadID;
    }
}
