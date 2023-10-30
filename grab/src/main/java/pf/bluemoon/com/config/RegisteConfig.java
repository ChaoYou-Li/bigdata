package pf.bluemoon.com.config;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-18 17:32
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public interface RegisteConfig {

    void register(Class<?> clazz);

    Class<?> get(String keyType);
}
