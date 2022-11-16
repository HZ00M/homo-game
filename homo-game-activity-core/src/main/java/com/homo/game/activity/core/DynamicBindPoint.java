package com.homo.game.activity.core;

import com.homo.core.utils.delegate.BroadCasterCall;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.facade.event.Event;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;

@Log4j2
public class DynamicBindPoint extends BroadCasterCall<NodeData> {
    @Override
    protected Boolean execute(NodeData nodeData, Object... objects) throws Exception {
        Assert.isTrue(objects[1] instanceof Event, "param [1] must implement Event interface");
        Event event = (Event) objects[1];
        boolean rel = false;
        Node node = nodeData.mainNode;
        if (node != null) {
            rel = true;
            node.dispatchEvent(nodeData, event);
        }
        return rel;
    }
}
