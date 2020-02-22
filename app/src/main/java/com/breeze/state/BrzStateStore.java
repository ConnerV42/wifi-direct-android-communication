package com.breeze.state;

import com.breeze.EventEmitter;
import com.breeze.datatypes.BrzNode;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzChat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BrzStateStore extends EventEmitter {

    private static BrzStateStore instance = new BrzStateStore();

    public static BrzStateStore getStore() {
        return instance;
    }

    //
    // Viewing Chat ID
    //

    private String currentChat = "";

    public String getCurrentChat() {
        return this.currentChat;
    }

    public void setCurrentChat(String chatId) {
        this.currentChat = chatId;
        this.emit("currentChat", chatId);
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
    // Nodes
    //

    private HashMap<String, BrzNode> nodes = new HashMap<>();
    private HashMap<String, BrzNode> blockedNodes = new HashMap<>();

    public List<BrzNode> getAllNodes() {
        return new LinkedList<>(this.nodes.values());
    }

    public BrzNode getNode(String nodeId) {
        return this.nodes.get(nodeId);
    }

    public void setNode(BrzNode node) {
        if (node == null || node.id == null) return;
        this.nodes.put(node.id, node);
        this.emit("nodeSet", node);
    }

    public void setNodes(List<BrzNode> nodesToAdd) {
        for (BrzNode node : nodesToAdd) setNode(node);
    }

    public void blockNode(BrzNode node){
        if (node == null || node.id == null) return;
        this.blockedNodes.put(node.id, node);
        this.emit("nodeSetBlock", node);
    }

    public void unblockNode(String nodeId) {
        this.blockedNodes.remove(nodeId);
        this.emit("nodeSetUnblock");
    }

    public List<BrzNode> getAllBlockedNodes(){ return new LinkedList<>(this.blockedNodes.values()); }

    //
    //  Chats
    //

    private HashMap<String, BrzChat> chats = new HashMap<>();

    public List<BrzChat> getAllChats() {
        return new ArrayList<>(this.chats.values());
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
    //  Public Messages
    //

    private List<BrzMessage> publicMessages = new LinkedList<>();
    private final int PUBLIC_MESSAGES_MAX_SIZE = 1000;

    public List<BrzMessage> getPublicMessages() {
        return new LinkedList<>(this.publicMessages);
    }

    public void addPublicMessage(BrzMessage msg) {
        if (publicMessages.size() >= PUBLIC_MESSAGES_MAX_SIZE) {
            publicMessages.remove(0);
        }

        publicMessages.add(msg);

        this.emit("publicMessages", getPublicMessages());
    }

}
