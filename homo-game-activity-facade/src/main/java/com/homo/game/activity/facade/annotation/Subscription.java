package com.homo.game.activity.facade.annotation;

import com.homo.game.activity.facade.event.EventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscription {
    /**
     * 订阅端点
     */
    String value() ;

    /**
     * 处理优先级
     */
    int order() default Integer.MAX_VALUE;
}
