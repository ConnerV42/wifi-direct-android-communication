package com.breeze.application;

import android.util.Log;

import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.packets.LiveConnectionEvents.BrzLiveConnectionReady;
import com.breeze.packets.LiveConnectionEvents.BrzLiveConnectionRequest;
import com.breeze.streams.BrzLiveAudioConsumer;
import com.breeze.streams.BrzLiveAudioProducer;

import java.io.InputStream;

public class BreezeLiveStreamModule extends BreezeModule {

    public BreezeLiveStreamModule(BreezeAPI api) {
        super(api);
    }

    public void sendBrzLiveConnectionRequest(String to){
        BrzLiveAudioProducer prod = this.createAudioProducer();
        BrzPacket packet = BrzPacketBuilder.streamRequest(to, prod);
        this.api.router.send(packet);
        Log.i("BREEZELIVESTREAMMODULE", "BrzLiveConnectionRequest sent to: " + to);
    }

    public void receiveBrzLiveConnectionRequest(String from, BrzLiveConnectionRequest req){
        Log.i("BREEZELIVESTREAMMODULE", "BrzLiveConnection request received");
        BrzLiveAudioConsumer conSOOM = this.createAudioConsumer(req.getProducerEndpointID(), req.getProducerPayloadID());
        this.sendBrzLiveConnectionReady(from, conSOOM);
    }

    public void sendBrzLiveConnectionReady(String to, BrzLiveAudioConsumer conSOOM){
        if(!this.api.state.checkIfConsumerExists(conSOOM.getProducerPayloadID() + conSOOM.getProducerEndpointId())){
            Log.e("BREEZELIVESTREAMMODULE", "BrzLiveAudioConsumer doesn't exist");
            return;
        }
        this.api.state.getConsumer(conSOOM.getProducerPayloadID() + conSOOM.getProducerEndpointId());
        BrzPacket packet = BrzPacketBuilder.streamReady(to, conSOOM);
        this.api.router.send(packet);
        Log.i("BREEZELIVESTREAMMODULE", "BrzLiveConnectionReady sent to: " + to);
    }

    public void receiveBrzLiveConnectionReady(String from, BrzLiveConnectionReady ready){
        if(!this.api.state.checkIfProducerExists(ready.getProducerPayloadID() + ready.getProducerEndpointID())){
            Log.e("BREEZELIVESTREAMMODULE", "BrzLiveAudioProducer doesn't exist");
            return;
        }
        this.sendAudioProducerPayload(from, this.api.state.getAudioProducer(ready.getProducerPayloadID() + ready.getProducerEndpointID()));
        Log.i("BREEZELIVESTREAMMODULE", "Producer payload sent from device");
    }

    public BrzLiveAudioProducer createAudioProducer(){
        try {
            BrzLiveAudioProducer prod = new BrzLiveAudioProducer();
            prod.setProducerEndpointID(api.hostNode.id);
            this.api.state.addAudioProducer(prod);
            return prod;
        } catch (Exception e){
            Log.e("BREEZELIVESTREAMMODULE", "Cannot create producer");
            return null;
        }
    }

    public BrzLiveAudioConsumer createAudioConsumer(String endpointId, long payloadId){
        try {
            BrzLiveAudioConsumer conSOOOM = new BrzLiveAudioConsumer(endpointId, payloadId, null);
            this.api.state.addAudioConsumer(conSOOOM);
            conSOOOM.setReadyForConsume(true);
            return conSOOOM;
        } catch(Exception e){
            Log.e("BREEZELIVESTREAMMODULE", "Cannot create consumer");
            return null;
        }
    }

    public void sendAudioProducerPayload(String to, BrzLiveAudioProducer prod){
        if(!prod.isRecording() || prod.getPayload() == null){
            Log.e("BREEZELIVESTREAMMODULE", "BrzLiveAudioProducer not recording, cannot send payload");
            return;
        }
        this.api.router.sendAudioStream(to, prod.getPayload());
    }

    public boolean consumeAudioProducer(BrzLiveAudioConsumer consume, InputStream is){
        try {
            consume.setRawAudioInput(is);
            consume.setReadyToPlay(true);
            return true;
        } catch(Exception e){
            Log.i("BRZLIVESTREAMMODULE", "Cannot setup raw audio input for audio consumer");
            return false;
        }
    }

}
