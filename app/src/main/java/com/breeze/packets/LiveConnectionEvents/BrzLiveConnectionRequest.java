package com.breeze.packets.LiveConnectionEvents;

import android.util.Log;

import com.breeze.packets.BrzPacket;
import com.breeze.streams.BrzLiveAudioProducer;
import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

import javax.crypto.SecretKey;

public class BrzLiveConnectionRequest implements BrzSerializable {

    private String producerEndpointID;
    private long producerPayloadID;
    private String checksum;

    public BrzLiveConnectionRequest(){

    }

    public BrzLiveConnectionRequest(String json){
        this.fromJSON(json);
    }

    @Override
    public String toJSON(){
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
    public long getProducerPayloadID() {
        return producerPayloadID;
    }

    public void setProducerPayloadID(long producerPayloadID) {
        this.producerPayloadID = producerPayloadID;
    }
    public String getProducerEndpointID() {
        return producerEndpointID;
    }

    public void setProducerEndpointID(String producerEndpointID) {
        this.producerEndpointID = producerEndpointID;
    }
}
