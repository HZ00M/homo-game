package com.homo.game.persitent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InitBeanPstProcessor implements BeanPostProcessor {
    /**
     * Factory hook that allows for custom modification of new bean instances — for example, checking for marker interfaces or wrapping beans with proxies.
     * Typically, post-processors that populate beans via marker interfaces or the like will implement postProcessBeforeInitialization, while post-processors that wrap beans with proxies will normally implement postProcessAfterInitialization.
     * Registration
     * An ApplicationContext can autodetect BeanPostProcessor beans in its bean definitions and apply those post-processors to any beans subsequently created. A plain BeanFactory allows for programmatic registration of post-processors, applying them to all beans created through the bean factory.
     * Ordering
     * BeanPostProcessor beans that are autodetected in an ApplicationContext will be ordered according to org.springframework.core.PriorityOrdered and org.springframework.core.Ordered semantics. In contrast, BeanPostProcessor beans that are registered programmatically with a BeanFactory will be applied in the order of registration; any ordering semantics expressed through implementing the PriorityOrdered or Ordered interface will be ignored for programmatically registered post-processors. Furthermore, the @Order annotation is not taken into account for BeanPostProcessor beans.
     * Since:
     * 10.10.2003
     * See Also:
     * InstantiationAwareBeanPostProcessor, DestructionAwareBeanPostProcessor, ConfigurableBeanFactory.addBeanPostProcessor, BeanFactoryPostProcessor
     * Author:
     * Juergen Hoeller, Sam Brannen
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        log.info("before beanName {}",beanName);
        return bean;
    }

}
