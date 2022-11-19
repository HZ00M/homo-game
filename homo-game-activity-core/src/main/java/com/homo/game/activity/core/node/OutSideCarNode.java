package com.homo.game.activity.core.node;

import com.homo.game.activity.core.Node;

/**
 * 组合节点的输出边界代理
 *
 * 用编辑器编辑的节点都是组合节点
 * 本节点是此组合节点和其他节点交互的代理，因此本节点（SideCar）的父节点一定是另一组组合节点的边界代理(另一个SideCar)
 * 组合节点可以通过该代理节点向外部发布事件（publish）和请求(ask)
 *
 */
public class OutSideCarNode extends Node {
    public static String ID = "Out";
    /**
     * 本组合节点的父节点
     */
    public Node owner;
}
