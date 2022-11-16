package com.homo.game.activity.core;

import com.homo.core.utils.rector.Homo;

/**
 * Owner加载接口
 * 被owner的reload调用，用来在owner被释放后重新加载
 */
public interface OwnerLoader {
    /**
     * 处理异步情况
     */
    Homo<Owner> asyncGet(Owner owner);
}
