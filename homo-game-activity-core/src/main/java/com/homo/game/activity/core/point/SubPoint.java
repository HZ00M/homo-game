package com.homo.game.activity.core.point;

import com.homo.core.utils.delegate.Delegate2PVoid;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.facade.event.Event;
import com.homo.game.activity.facade.event.EventType;

public class SubPoint extends Delegate2PVoid<NodeData, Event> {
    public String msgId;
    public EventType eventType;

    public SubPoint(String msgId,EventType eventType){
        this.msgId = msgId;
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getMsgId() {
        return msgId;
    }
}
