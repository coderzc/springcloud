package com.zc.dal.plugin.encryption.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring bean获取类
 */
@Component("springBeanUtils")
public class SpringBeanUtil implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(SpringBeanUtil.class);
    private static ApplicationContext context;

    public void setApplicationContext(ApplicationContext context)
            throws BeansException {
        this.context = context;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> objClass) {
        ApplicationContext context = getContext();
        if (null != context) {
            return (T) context.getBean(objClass);
        }
        return null;
    }

    public static Object getBean(String beanName) {
//        logger.info("SpringBeanUtil.getBean:{}", beanName);
        ApplicationContext context = getContext();
        if (null != context) {
            return context.getBean(beanName);
        }
        return null;
    }

    public static void printAllBeans() {
        logger.info("start printAllBeans");
        ApplicationContext context = getContext();
        if (context != null) {
            String[] beans = context.getBeanDefinitionNames();
            for (
                    String beanName : beans) {
                Class<?> beanType = context.getType(beanName);
                System.out.println("BeanName:" + beanName);
                System.out.println("Bean type：" + beanType);
                System.out.println("Bean package：" + beanType.getPackage());
                System.out.println("Bean：" + context.getBean(
                        beanName));
            }
        } else {
            logger.info("printAllBeans context is null!");
        }
    }
}
