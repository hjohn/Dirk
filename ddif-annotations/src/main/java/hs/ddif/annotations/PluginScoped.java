package hs.ddif.annotations;

import java.lang.annotation.Retention;

import javax.inject.Scope;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Identifies a type which is only instantiated once per plugin.<p>
 *
 * Functions similar to {@link javax.inject.Singleton} and is availabe as long as a plugin is loaded.  Unlike
 * the Singleton annotation, this makes it possible to unload all classes from a plugin without having
 * to destroy the injector the type was associated with.<p>
 *
 * @see javax.inject.Scope @Scope
 */
@Scope
@Retention(RUNTIME)
public @interface PluginScoped {

}
