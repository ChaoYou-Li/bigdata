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
 * @Description 唯一索引标识（可以多个字段组成唯一索引，name 相同即可）
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface UniqueKey {
    public static final String KEY_TYPE = "unique_key_idx";
    String name() default "";
}
