package com.homo.game.activity.core.point;

import com.homo.core.utils.delegate.Delegate2PR;
import com.homo.core.utils.rector.Homo;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.core.Point;
import com.homo.game.activity.facade.event.Event;

/**
 * 请求端点
 *  如果有多个reply，则只返回第一个reply的执行结果
 */
public class AskPointProxy<R> extends Delegate2PR<NodeData, Event, Homo<R>> {
    public Point point;
    public String askPointName;
    public AskPointProxy(Point point, String askPointName){
        this.point = point;
        this.askPointName = askPointName;
    }

    public Homo<R> ask(NodeData nodeData,Event event){
        Homo<R> rel = publish(nodeData, event);
        if (rel == null){
            return Homo.result(null);
        }
        return rel.nextValue(ret->(R) ret);
    }
}
