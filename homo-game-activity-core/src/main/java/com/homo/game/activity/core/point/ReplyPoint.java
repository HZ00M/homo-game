package com.homo.game.activity.core.point;

import com.homo.core.utils.delegate.Delegate2PR;
import com.homo.game.activity.core.Point;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.facade.event.Event;

public class ReplyPoint<R> extends Delegate2PR<NodeData, Event,R> {
    public Point point;
    public String replyPointName;
    public ReplyPoint(Point point,String replyPointName){
        this.point = point;
        this.replyPointName = replyPointName;
    }
}
