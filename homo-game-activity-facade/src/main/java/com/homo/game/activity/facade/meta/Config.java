package com.homo.game.activity.facade.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识变量为配置参数
 * 在变量上添加此注解，会将此变量暴露给编辑器配置此变量的值
 * 支持字符串和整形
 * 支持默认值
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Config {
    String value() default "";
}
