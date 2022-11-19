package com.homo.game.activity.core.point;

import com.homo.core.utils.delegate.Delegate2PVoid;
import com.homo.game.activity.core.Node;
import com.homo.game.activity.core.compoment.Component;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.facade.Point;
import com.homo.game.activity.facade.event.Event;

public class PubPoint<T extends Event> extends Delegate2PVoid<NodeData,T> {
    Point point;
    String pubPointName;

    public PubPoint(Point point,String pubPointName){
        this.point = point;
        this.pubPointName = pubPointName;
    }

    public void publish(T event){
        Node node = null;
        if (point instanceof Component){
            node = ((Component)point).getNode();
        }else {
            node = (Node) point;
        }
        node.publish(pubPointName,event);
    }
}
