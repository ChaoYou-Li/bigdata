package pf.bluemoon.com.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-21 14:31
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@Component
public class SpringBeanUtils implements ApplicationContextAware, BeanDefinitionRegistryPostProcessor {

    public static ApplicationContext context;
    public static BeanDefinitionRegistry registry;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringBeanUtils.context = applicationContext;
    }

    public static Set<String> getComponentScanningPackages() {
        Set<String> packages = new LinkedHashSet<>();
        String[] names = context.getBeanDefinitionNames();
        for (String name : names) {
            BeanDefinition beanDefinition = SpringBeanUtils.registry.getBeanDefinition(name);
            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                AnnotatedBeanDefinition annotatedDefinition = (AnnotatedBeanDefinition) beanDefinition;
                addComponentScanningPackages(packages, annotatedDefinition.getMetadata());
            }
        }
        return packages;
    }

    private static void addComponentScanningPackages(Set<String> packages, AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes
                .fromMap(metadata.getAnnotationAttributes(ComponentScan.class.getName(), true));
        if (attributes != null) {
            addPackages(packages, attributes.getStringArray("value"));
            addPackages(packages, attributes.getStringArray("basePackages"));
            addClasses(packages, attributes.getStringArray("basePackageClasses"));
            if (packages.isEmpty()) {
                packages.add(ClassUtils.getPackageName(metadata.getClassName()));
            }
        }
    }

    private static void addPackages(Set<String> packages, String[] values) {
        if (values != null) {
            Collections.addAll(packages, values);
        }
    }

    private static void addClasses(Set<String> packages, String[] values) {
        if (values != null) {
            for (String value : values) {
                packages.add(ClassUtils.getPackageName(value));
            }
        }
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        SpringBeanUtils.registry = registry;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
