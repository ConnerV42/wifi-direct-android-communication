package com.breeze.state;

import android.content.Context;
import android.util.Log;

import com.breeze.EventEmitter;
import com.breeze.application.BreezeAPI;
import com.breeze.streams.BrzLiveAudioConsumer;
import com.breeze.streams.BrzLiveAudioProducer;
import com.breeze.datatypes.BrzNode;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzChat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class BrzStateStore extends EventEmitter {

    private static BrzStateStore instance = new BrzStateStore();

    public static BrzStateStore getStore() {
        return instance;
    }

    //
    // Title
    //

    private String title = "";

    public void setTitle(String title) {
        this.title = title;
        this.emit("title", this.title);
    }

    //
    // Host node
    //

    private BrzNode hostNode = null;

    public BrzNode getHostNode() {
        return this.hostNode;
    }

    public void setHostNode(BrzNode hostNode) {
        this.hostNode = hostNode;
        this.emit("hostNode", this.hostNode);
    }

    //
    //  Chats
    //

    private HashMap<String, BrzChat> chats = new HashMap<>();

    public List<BrzChat> getAllChats() {
        return new ArrayList<>(this.chats.values());
    }


    public List<BrzChat> getAllChatsNoPublicThread()
    {
         List<BrzChat> privChats = new ArrayList<>();
         for(BrzChat c : this.chats.values())
         {
             if(!(c.id.equals("PUBLIC_THREAD"))) {
                 privChats.add(c);
             }
         }
        return privChats;
    }


    public BrzChat getChat(String chatId) {
        return this.chats.get(chatId);
    }

    public void addChat(BrzChat chat) {
        this.chats.put(chat.id, chat);

        this.emit("allChats", this.getAllChats());
        this.emit("chat" + chat.id, chat);
    }

    public void addAllChats(List<BrzChat> chats) {
        for (BrzChat chat : chats) {
            this.chats.put(chat.id, chat);
            this.emit("chat" + chat.id, chat);
        }

        this.emit("allChats", this.getAllChats());
    }

    public void removeChat(String chatId) {
        this.chats.remove(chatId);

        this.emit("allChats", this.getAllChats());
        this.emit("chat" + chatId, null);
    }

    //
    //  Messages
    //

    private HashMap<String, List<BrzMessage>> messages = new HashMap<>();

    public List<BrzMessage> getMessages(String chatId) {
        return this.messages.get(chatId);
    }

    public List<BrzMessage> getPublicMessages(){
        return this.messages.get("PUBLIC_THREAD");
    }

    public void addMessage(BrzMessage msg) {
        List<BrzMessage> messages = this.messages.get(msg.chatId);
        if (messages == null) {
            messages = new ArrayList<>();
            this.messages.put(msg.chatId, messages);
        }
        messages.add(msg);

        this.emit("messages" + msg.chatId, this.getMessages(msg.chatId));
        this.emit("messages");
    }

    public void addAllMessages(List<BrzMessage> newMessages) {
        Set<String> chatIdsToUpdate = new HashSet<>();
        for (BrzMessage msg : newMessages) {
            List<BrzMessage> messages = this.messages.get(msg.chatId);
            if (messages == null) {
                messages = new ArrayList<>();
                this.messages.put(msg.chatId, messages);
            }
            messages.add(msg);
            chatIdsToUpdate.add(msg.chatId);
        }

        for (String chatId : chatIdsToUpdate)
            this.emit("messages" + chatId, this.getMessages(chatId));

        this.emit("messages");
    }

    //
    // Audio Producers
    //

    private HashMap<String, BrzLiveAudioProducer> audioProducerHashMap = new HashMap<>();

    public void addAudioProducer(BrzLiveAudioProducer prod){
        this.audioProducerHashMap.put(prod.getPayloadId() + prod.getProducerEndpointID(), prod);
        this.emit("audioproducers");
        Log.i("BRZSTATESTORE", "BrzLiveAudioProducer added to state with key: " + prod.getPayloadId()  + prod.getProducerEndpointID());
    }

    public BrzLiveAudioProducer getAudioProducer(String payloadAndEndpointID){
        return this.audioProducerHashMap.get(payloadAndEndpointID);
    }

    public void deleteAudioProducer(String payloadAndEndpointID){
        this.audioProducerHashMap.remove(payloadAndEndpointID);
        this.emit("audioproducersdelete");
    }

    public boolean checkIfProducerExists(String payloadAndEndpointID){
        return this.audioProducerHashMap.containsKey(payloadAndEndpointID);
    }

    //
    // Audio Consumers
    //

    private HashMap<String, BrzLiveAudioConsumer> audioConsumerHashMap = new HashMap<>();
    public BrzLiveAudioConsumer getConsumer(String producerPayloadAndEndpointID){
        if(this.audioConsumerHashMap == null || !this.audioConsumerHashMap.containsKey(producerPayloadAndEndpointID)){
            return null;
        }
        return this.audioConsumerHashMap.get(producerPayloadAndEndpointID);
    }
    public boolean addAudioConsumer(BrzLiveAudioConsumer consumer){
        if(this.audioConsumerHashMap == null || consumer == null || this.audioConsumerHashMap.containsKey(consumer.getProducerPayloadID() + consumer.getProducerEndpointId())){
            return false;
        }
        this.audioConsumerHashMap.put(consumer.getProducerPayloadID() + consumer.getProducerEndpointId(), consumer);
        this.emit("audioconsumers");
        Log.i("BRZSTATESTORE", "BrzLiveAudioConsumer added with key: " + consumer.getProducerPayloadID() + consumer.getProducerEndpointId());
        return true;
    }
    public boolean consumerReady(String producerPayloadAndEndpointID){
        if(this.audioConsumerHashMap == null || !this.audioConsumerHashMap.containsKey(producerPayloadAndEndpointID)){
            return false;
        }
        try {
            return this.audioConsumerHashMap.get(producerPayloadAndEndpointID).isReadyForConsume();
        } catch(Exception e){
            return false;
        }
    }
    public boolean deleteConsumer(String producerPayloadAndEndpointID){
        if(this.audioConsumerHashMap == null || !this.audioConsumerHashMap.containsKey(producerPayloadAndEndpointID)){
            return false;
        }
        this.emit("audioconsumersdelete");
        this.audioConsumerHashMap.remove(producerPayloadAndEndpointID);
        return true;
    }
    public boolean checkIfConsumerExists(String producerPayloadAndEndpointID){
        return this.audioConsumerHashMap.containsKey(producerPayloadAndEndpointID);
    }
}
