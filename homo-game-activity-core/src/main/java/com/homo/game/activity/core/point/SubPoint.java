package com.homo.game.activity.core.point;

import com.homo.core.utils.delegate.Delegate2PVoid;
import com.homo.game.activity.core.Point;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.facade.event.Event;

public class SubPoint extends Delegate2PVoid<NodeData, Event> {
    public Point point;
    public String subPointName;

    public SubPoint(Point point,String subPointName){
        this.point = point;
        this.subPointName = subPointName;
    }
}
