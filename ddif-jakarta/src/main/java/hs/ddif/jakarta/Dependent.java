package hs.ddif.jakarta;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Scope;

/**
 * Default scope for unscoped objects.
 *
 * <p>Note: although this specific scope annotation is not required for this injector framework, it
 * is a requirement that a default dependent scope annotation is provided. Another annotation would
 * do just as well as long as it is specified during configuration of the injector.
 */
@Scope
@Documented
@Retention(RUNTIME)
public @interface Dependent {

}
