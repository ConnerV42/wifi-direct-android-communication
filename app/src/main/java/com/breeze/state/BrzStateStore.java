package com.breeze.state;

import com.breeze.packets.BrzMessage;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class BrzStateStore {

    private static BrzStateStore instance = new BrzStateStore();
    public static BrzStateStore getStore() {
        return instance;
    }

    //
    //
    //  Title
    //
    //

    private String title = "";
    private List<Consumer<String>> titleListeners = new ArrayList<>();

    public void getTitle(Consumer<String> callback) {
        this.titleListeners.add(callback);
        callback.accept(this.title);
    }
    public String getTitle() { return this.title; }

    public void setTitle(String title) {
        this.title = title;
        for(Consumer<String> c : this.titleListeners) c.accept(this.title);
    }

    //
    //
    //  User
    //
    //

    private BrzUser user = null;
    private List<Consumer<BrzUser>> userListeners = new ArrayList<>();

    public void getUser(Consumer<BrzUser> callback) {
        this.userListeners.add(callback);
        callback.accept(this.user);
    }
    public BrzUser getUser() { return this.user; }

    public void setUser(BrzUser user) {
        this.user = user;
        for(Consumer<BrzUser> c : this.userListeners) c.accept(this.user);
    }

    //
    //
    //  Chats
    //
    //

    private HashMap<String, BrzChat> chats = new HashMap<>();
    private List<Consumer<List<BrzChat>>> chatListListeners = new ArrayList<>();
    private HashMap<String, List<Consumer<BrzChat>>> chatListeners = new HashMap<>();

    public void getAllChats(Consumer<List<BrzChat>> callback) {
        chatListListeners.add(callback);
        callback.accept(new ArrayList<>(this.chats.values()));
    }
    public List<BrzChat> getAllChats() {
        return new ArrayList<>(this.chats.values());
    }

    public void getChat(String chatId, Consumer<BrzChat> callback) {
        List<Consumer<BrzChat>> listeners =  this.chatListeners.get(chatId);
        if(listeners == null) {
            listeners = new ArrayList<>();
            this.chatListeners.put(chatId, listeners);
        }
        listeners.add(callback);
        callback.accept(this.chats.get(chatId));
    }
    public BrzChat getChat(String chatId) {
        return this.chats.get(chatId);
    }

    public void addChat(BrzChat chat) {
        this.chats.put(chat.id, chat);

        List<BrzChat> allChats = new ArrayList<>(this.chats.values());
        for(Consumer<List<BrzChat>> c : this.chatListListeners) c.accept(allChats);

        List<Consumer<BrzChat>> cl = this.chatListeners.get(chat.id);
        if(cl != null) for(Consumer<BrzChat> c : cl) c.accept(chat);
    }
    public void removeChat(String chatId) {
        this.chats.remove(chatId);

        List<BrzChat> allChats = new ArrayList<>(this.chats.values());
        for(Consumer<List<BrzChat>> c : this.chatListListeners) c.accept(allChats);

        List<Consumer<BrzChat>> cl = this.chatListeners.get(chatId);
        if(cl != null) for(Consumer<BrzChat> c : cl) c.accept(null);
    }

    //
    //
    //  Messages
    //
    //

    private HashMap<String, List<BrzMessage>> messages = new HashMap<>();
    private HashMap<String, List<Consumer<List<BrzMessage>>>> mlisteners = new HashMap<>();

    public void getMessages(String chatId, Consumer<List<BrzMessage>> callback) {
        List<Consumer<List<BrzMessage>>> listeners =  this.mlisteners.get(chatId);
        if(listeners == null) {
            listeners = new ArrayList<>();
            this.mlisteners.put(chatId, listeners);
        }
        listeners.add(callback);
        callback.accept(this.messages.get(chatId));
    }
    public List<BrzMessage> getMessages(String chatId) {
        return this.messages.get(chatId);
    }

    public void addMessage(String chatId, BrzMessage msg) {
        List<BrzMessage> messages = this.messages.get(chatId);
        if(messages == null) {
            messages = new ArrayList<>();
            this.messages.put(chatId, messages);
        }
        messages.add(msg);

        List<Consumer<List<BrzMessage>>> cl = this.mlisteners.get(chatId);
        if(cl != null) for(Consumer<List<BrzMessage>> c : cl) c.accept(messages);
    }



}
