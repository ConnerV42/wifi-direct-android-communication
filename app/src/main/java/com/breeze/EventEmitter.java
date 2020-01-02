package com.breeze;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public abstract class EventEmitter {
    private HashMap<String, List<Consumer>> listenerMap = new HashMap<>();

    public void on(String event, Consumer callback) {
        List<Consumer> l = this.listenerMap.get(event);
        if (l == null) {
            l = new LinkedList<>();
            this.listenerMap.put(event, l);
        }

        l.add(callback);
    }

    public void off(String event, Consumer callback) {
        List<Consumer> l = this.listenerMap.get(event);
        if (l != null) {
            this.listenerMap.put(event, l);
            l.remove(callback);
        }
    }

    public void off(String event) {
        this.listenerMap.remove(event);
    }

    public void emit(String event, Object payload) {
        List<Consumer> l = this.listenerMap.get(event);
        if (l == null) return;

        for (Consumer c : l) c.accept(payload);
    }
}
