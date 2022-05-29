package org.int4.dirk.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Scope;

/**
 * Default scope for unscoped objects.
 */
@Scope
@Documented
@Retention(RUNTIME)
public @interface Dependent {

}
