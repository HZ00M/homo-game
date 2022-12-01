package com.homo.game.activity.core.point;

import com.homo.core.utils.delegate.Delegate2PVoid;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.facade.event.Event;
import com.homo.game.activity.facade.event.EventType;

public class EventPointProxy extends Delegate2PVoid<NodeData, Event> {
    public String eventId;
    public EventPointProxy(String eventId){
        this.eventId = eventId;
    }
}
