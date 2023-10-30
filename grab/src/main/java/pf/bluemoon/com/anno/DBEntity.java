package pf.bluemoon.com.anno;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
public @interface DBEntity {
    /**
     * (Optional) The name of the table.
     * <p> Defaults to the entity name.
     */
    String name() default "";
}
