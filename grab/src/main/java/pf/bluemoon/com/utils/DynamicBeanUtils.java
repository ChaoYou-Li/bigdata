package pf.bluemoon.com.utils;

import org.springframework.cglib.beans.BeanGenerator;
import org.springframework.cglib.beans.BeanMap;

import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-17 18:10
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class DynamicBeanUtils {

    private Object target;
    private BeanMap beanMap;

    public DynamicBeanUtils(Class superclass, Map<String, Class> propertyMap) {
        this.target = generateBean(superclass, propertyMap);
        this.beanMap = BeanMap.create(this.target);
    }

    public void setValue(String property, Object value) {
        beanMap.put(property, value);
    }

    public Object getValue(String property) {
        return beanMap.get(property);
    }

    public Object getTarget() {
        return this.target;
    }
    /**
     * 根据属性生成对象
     *
     */
    private Object generateBean(Class superclass, Map<String, Class> propertyMap) {
        BeanGenerator generator = new BeanGenerator();
        if (null != superclass) {
            generator.setSuperclass(superclass);
        }
        BeanGenerator.addProperties(generator, propertyMap);
        return generator.create();
    }
}
