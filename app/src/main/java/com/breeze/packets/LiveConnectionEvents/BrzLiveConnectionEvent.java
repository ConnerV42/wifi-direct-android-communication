package com.breeze.packets.LiveConnectionEvents;

import com.breeze.packets.BrzSerializable;

public class BrzLiveConnectionEvent implements BrzSerializable {
    public enum LiveConnectionEventType { DECLINED, ENDED, EXCEPTION }

    private LiveConnectionEventType type;

    public BrzLiveConnectionEvent(LiveConnectionEventType type) {
        this.type = type;
    }

    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public void fromJSON(String json) {

    }

    public LiveConnectionEventType getType() {
        return type;
    }

    public void setType(LiveConnectionEventType type) {
        this.type = type;
    }
}
