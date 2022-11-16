package com.homo.game.activity.facade.event;

public interface Event {

    String getId();
    default EvenType getType() {
        return EvenType.any;
    }
}
