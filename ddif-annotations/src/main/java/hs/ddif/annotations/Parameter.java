package hs.ddif.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates a field or method parameter is a required parameter to be provided
 * at runtime via a factory used for assisted injection.
 */
@Retention(RUNTIME)
@Target({FIELD, PARAMETER})
public @interface Parameter {

  /**
   * Name for factory parameters, which must match the name at the injection site.  Optional
   * if source is compiled with parameter names (compile with debug info or with -parameters
   * switch).
   *
   * @return the name of the factory parameter, or empty if parameter name should be determined via reflection
   */
  String value() default "";
}
