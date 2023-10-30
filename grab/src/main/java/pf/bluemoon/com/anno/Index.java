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
 * @Description 普通索引，只是为了方便检索
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Index {
    public static final String KEY_TYPE = "text_idx";
    String name() default "";
}
