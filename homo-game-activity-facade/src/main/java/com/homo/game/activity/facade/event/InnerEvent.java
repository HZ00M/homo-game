package com.homo.game.activity.facade.event;

/**
 * 内部消息传递事件
 * 只在同一个行为树上传播的事件，同个行为树上的节点才能监听并订阅处理该事件
 */
public interface InnerEvent extends Event{
}
