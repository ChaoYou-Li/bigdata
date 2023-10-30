package pf.bluemoon.com.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-18 17:38
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class IdxKeyRegisteConfig implements RegisteConfig{

    private Map<String, Class<?>> config = new HashMap<>();

    @Override
    public void register(Class<?> clazz) {
        config.put(clazz.getSimpleName().toLowerCase(), clazz);
    }

    @Override
    public Class<?> get(String keyType) {
        return config.get(keyType);
    }
}
