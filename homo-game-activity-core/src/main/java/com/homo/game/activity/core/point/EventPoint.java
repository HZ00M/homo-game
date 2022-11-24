package com.homo.game.activity.core.point;

import com.homo.core.utils.delegate.Delegate2PVoid;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.facade.event.Event;
import com.homo.game.activity.facade.event.EventType;

public class EventPoint extends Delegate2PVoid<NodeData, Event> {
    public String eventId;
    public EventType eventType;
    public EventPoint(String eventId,EventType eventType){
        this.eventId = eventId;
        this.eventType = eventType;
    }
}
