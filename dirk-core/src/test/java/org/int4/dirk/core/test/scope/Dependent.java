package org.int4.dirk.core.test.scope;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Scope;

@Scope
@Documented
@Retention(RUNTIME)
public @interface Dependent {

}
