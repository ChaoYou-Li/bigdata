package pf.bluemoon.com.utils;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-17 18:11
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ReflectUtils {
    private static Logger logger = LoggerFactory.getLogger(ReflectUtils.class);

    public static Object getObject(Object dest, Map<String, Object> addProperties) {
        PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
        PropertyDescriptor[] descriptors = propertyUtilsBean.getPropertyDescriptors(dest);
        Map<String, Class> propertyMap = new HashMap<>();
        for (PropertyDescriptor d : descriptors) {
            if (!"class".equalsIgnoreCase(d.getName())) {
                propertyMap.put(d.getName(), d.getPropertyType());
            }
        }
        addProperties.forEach((k, v) -> {
            String sclass = v.getClass().toString();
            if(sclass.equals("class java.util.Date")) {//对日期进行处理
                propertyMap.put(k, Long.class);
            }else {
                propertyMap.put(k, v.getClass());
            }

        });
        DynamicBeanUtils dynamicBean = new DynamicBeanUtils(dest.getClass(), propertyMap);
        propertyMap.forEach((k, v) -> {
            try {
                if (!addProperties.containsKey(k)) {
                    dynamicBean.setValue(k, propertyUtilsBean.getNestedProperty(dest, k));
                }
            } catch (Exception e) {
                logger.error("动态添加字段出错", e);
            }
        });
        addProperties.forEach((k, v) -> {
            try {
                String sclass = v.getClass().toString();
                if(sclass.equals("class java.util.Date")) {//动态添加的字段为date类型需要进行处理
                    Date date = (Date) v;
                    dynamicBean.setValue(k, date.getTime());
                }else {
                    dynamicBean.setValue(k, v);
                }
            } catch (Exception e) {
                logger.error("动态添加字段值出错", e);
            }
        });
        Object obj = dynamicBean.getTarget();
        return obj;
    }

    public static Object getValueByField(Object obj, String fieldName) throws IllegalAccessException {
        if (null == obj || null == fieldName){
            return null;
        }
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(fieldName)){
                return field.get(obj);
            }
        }
        return null;
    }
}
