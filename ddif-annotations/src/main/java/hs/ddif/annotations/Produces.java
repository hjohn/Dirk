package hs.ddif.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;
import javax.inject.Scope;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates a producer method or field.<p>
 *
 * A producer method or field can have any access, and can be static or non-static.<p>
 *
 * Producer members can be annotated with {@link Qualifier}s, and can be annotated with a
 * {@link Scope}.<p>
 *
 * A producer's (result) type is registered with the injector.  The field is read or method is called when
 * an instance of that type is needed.  Any method parameters will be required dependencies and are
 * automatically supplied when called.
 */
@Retention(RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Produces {
}
