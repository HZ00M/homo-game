package com.homo.game.activity.facade.annotation;

import com.homo.game.activity.facade.event.EventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明在方法上表示这是一个事件处理方法
 * 方法签名 void funName(NodeData nodeData,Event event)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BindEvent {
    /**
     * 处理事件类型 默认都处理
     */
    EventType type() default EventType.any;

    /**
     *  事件类型
     */
    Class<?> value() default void.class;

    /**
     * 事件名
     */
    String msgId() default "";

    /**
     * 处理优先级 order越小优先级越高
     */
    int order() default 0;

}
