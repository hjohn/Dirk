package hs.ddif.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * When used in combination with the {@code Inject} annotation indicates that
 * the field or method is optional and can be left {@code null} if no injection
 * candidate is available.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, FIELD, PARAMETER})
public @interface Opt {

}
