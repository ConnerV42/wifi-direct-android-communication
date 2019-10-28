package com.breeze.state;

import com.breeze.packets.BrzBodyMessage;
import com.breeze.packets.BrzChat;

import java.util.ArrayList;
import java.util.HashMap;

public class BrzStateStore {

    private HashMap<String, BrzChat> chats = new HashMap<>();
    private HashMap<String, ArrayList<BrzBodyMessage>> messages = new HashMap<>();

    private HashMap<String, ArrayList<BrzStateObserver>> mlisteners = new HashMap<>();
    private HashMap<String, ArrayList<BrzStateObserver>> clisteners = new HashMap<>();

    private static BrzStateStore instance = new BrzStateStore();
    public static BrzStateStore getStore() {
        return instance;
    }

    public void getAllChats(BrzStateObserver listener) {
        ArrayList<BrzStateObserver> listeners =  this.clisteners.get("all");
        if(listeners == null) listeners = new ArrayList<>();
        listeners.add(listener);
        this.clisteners.put("all", listeners);
        listener.stateChange(new ArrayList(this.chats.values()));
    }
    public ArrayList<BrzChat> getAllChats() {
        return new ArrayList(this.chats.values());
    }

    public void getChat(BrzStateObserver listener, String chatId) {
        ArrayList<BrzStateObserver> listeners =  this.clisteners.get(chatId);
        if(listeners == null) listeners = new ArrayList<>();
        listeners.add(listener);
        this.clisteners.put(chatId, listeners);

        ArrayList<BrzChat> val = new ArrayList<BrzChat>();
        val.add(this.chats.get(chatId));
        listener.stateChange(val);
    }
    public BrzChat getChat(String chatId) {
        return this.chats.get(chatId);
    }

    public void getMessages(BrzStateObserver listener, String chatId) {
        ArrayList<BrzStateObserver> listeners =  this.mlisteners.get(chatId);
        if(listeners == null) listeners = new ArrayList<>();
        listeners.add(listener);
        this.mlisteners.put(chatId, listeners);

        listener.stateChange(this.messages.get(chatId));
    }
    public ArrayList<BrzBodyMessage> getMessages(String chatId) {
        return this.messages.get(chatId);
    }

    public void addMessage(String chatId, BrzBodyMessage msg) {
        ArrayList<BrzBodyMessage> messages = this.messages.get(chatId);
        if(messages == null) messages = new ArrayList<>();
        messages.add(msg);
        this.messages.put(chatId, messages);

        ArrayList<BrzStateObserver> mListeners = this.mlisteners.get(chatId);
        if(mListeners != null)
        for(BrzStateObserver o : mListeners)
            o.stateChange(messages);
    }

    public void addChat(BrzChat chat) {
        this.chats.put(chat.id, chat);

        ArrayList<BrzStateObserver> allListeners = this.clisteners.get("all");
        if(allListeners != null)
        for(BrzStateObserver o : allListeners)
            o.stateChange(new ArrayList(this.chats.values()));

        ArrayList<BrzStateObserver> chatListeners = this.clisteners.get(chat.id);
        ArrayList<BrzChat> val = new ArrayList<BrzChat>();
        val.add(chat);

        if(chatListeners != null)
        for(BrzStateObserver o : chatListeners)
            o.stateChange(val);
    }
}
