package com.homo.game.activity.facade.event;

public interface Event {

    String getId();
    default EventType getType() {
        return EventType.any;
    }
}
