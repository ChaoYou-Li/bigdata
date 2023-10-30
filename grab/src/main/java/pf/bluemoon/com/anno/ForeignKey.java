package pf.bluemoon.com.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-21 16:53
 * @Modified by
 * @Version 1.0.0
 * @Description 外键
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface ForeignKey {
    public static final String KEY_TYPE = "foreign_key_idx";
    String name() default "";
}
