package hs.ddif.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Scope;
import javax.inject.Singleton;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Identifies a type that the injector only instantiates as needed if no longer referenced.<p>
 *
 * Functions similar to {@link Singleton} except that a weak singleton makes it possible to
 * unload classes that make use of it without having to destroy the injector it was associated
 * with.<p>
 *
 * As weak singletons can be garbage collected if not referenced (even if not referenced for
 * just a short period during application or plugin initialization), extra care should be
 * taken with initialization code that should only run once (constructors, {@code javax.annotation.PostConstruct}
 * annotated methods).<p>
 *
 * Code which is used to just trigger initialization without assigning the result should be
 * used with great care as the instance might be immediately garbage collected and the
 * initialization may be repeated if the class is injected elsewhere.<p>
 *
 * Donot do this to trigger some initalization while discarding the resulting instance:<pre>
 *   injector.getInstance(SomeClass.class);</pre>
 *
 * Instead, assign the result to a variable until all initialization is finished or do
 * the initialization of the class differently.
 *
 * @see javax.inject.Scope @Scope
 */
@Scope
@Documented
@Retention(RUNTIME)
public @interface WeakSingleton {
}
