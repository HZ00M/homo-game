package com.homo.game.activity.core.node;

import com.homo.game.activity.core.Node;

/**
 * 组合节点的输入边界代理
 *
 * 用编辑器编辑的节点都是组合节点
 * 本节点是此组合节点和其他节点交互的代理，因此本节点（SideCar）的父节点一定是另一组组合节点的边界代理(另一个SideCar)
 * 其他外部节点给组合节点推送事件，会转发到本代理节点，组合节点内的节点可以再本代理订阅外部事件(sub)和响应外部请求(reply)
 */
public class InSideCarNode extends Node {
    public static String ID = "In";
    /**
     * 本组合节点的父节点
     */
    public Node owner;
}
