package com.homo.game.activity.core.compoment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 绑定组件
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BindComponent {
    /**
     * 需要绑定的节点列表
     */
    Class<? extends Component>[] value();
}
