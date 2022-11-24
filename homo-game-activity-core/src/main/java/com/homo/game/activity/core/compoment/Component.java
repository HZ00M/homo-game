package com.homo.game.activity.core.compoment;

import com.homo.game.activity.core.Node;
import com.homo.game.activity.core.Owner;
import com.homo.game.activity.core.data.NodeData;
import com.homo.game.activity.core.Point;

/**
 * 组件接口，表示该节点可以被绑定到其他节点上充当插件
 * （组件的功能合节点基本相同，节点可通过绑定不同组件拥有不同组件的能力）
 */
public interface Component extends Point {
    /**
     * 返回被绑定的宿主节点
     */
    Node getNode();

    /**
     * 设置宿主节点，在节点初始化时被调用
     */
    void setNode(Node node);

    /**
     * 通过名字获取Component,该节点可以是自己
     *
     * @param componentName 组件名（默认是简单类名）
     */
    default Component getComponent(String componentName) {
        return getNode().getComponent(componentName);
    }

    /**
     * 通过类型获取Component,该节点可以是自己
     *
     * @param componentClass 组件类型
     */
    default <T extends Component> T getComponent(Class<T> componentClass) {
        return getNode().getComponent(componentClass);
    }

    /**
     * 宿主节点创建后，初始化时被调用，可被子类重写
     * 程序启动时，宿主节点调用完onInitConfig后，才会调用此方法
     */
    default void onInitConfig() {
    }

    @Override
    default String getAddress() {
        return getNode().getAddress() + "_" + Node.getTypeName(this.getClass());
    }

    /**
     * 获取自身数据
     */
    default NodeData getSelfData(Owner owner) {
        return owner.getNodeData(getAddress());
    }
}
