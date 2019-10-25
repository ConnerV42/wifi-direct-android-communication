package com.breeze.state;

public class BrzStateChangeEvent {
    public String path;
    public Object value;

    public BrzStateChangeEvent(String path, Object value)  {
        this.path = path;
        this.value = value;
    }
}
