package com.homo.game.activity.core.point;

import com.homo.core.utils.delegate.Delegate2PVoid;
import com.homo.game.activity.core.Node;
import com.homo.game.activity.core.compoment.Component;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.core.Point;
import com.homo.game.activity.facade.event.Event;

public class PubPointProxy<T extends Event> extends Delegate2PVoid<NodeData,T> {
    public Point point;
    public String pubPointName;

    public PubPointProxy(Point point, String pubPointName){
        this.point = point;
        this.pubPointName = pubPointName;
    }

    public void publish(T event){
        Node node;
        if (point instanceof Component){
            node = ((Component)point).getNode();
        }else {
            node = (Node) point;
        }
        node.publish(pubPointName,event);
    }
}
