package pf.bluemoon.com.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-16 16:54
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface PrimryKey {
    public static final String KEY_TYPE = "primry_key_idx";
}
