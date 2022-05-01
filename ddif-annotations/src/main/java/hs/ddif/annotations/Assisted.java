package hs.ddif.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a type with a single abstract method to be automatically created by the
 * assisted type registration extension. To qualify the type must be:
 *
 * <ul>
 * <li>annotated with the {@link Assisted} annotation</li>
 * <li>an abstract class or interface with a single abstract method (a SAM type)</li>
 * </ul>
 *
 * Furthermore, the single abstract method must have:<ul>
 * <li>a concrete, non void, non primitive return type</li>
 * <li>no unresolvable type variables</li>
 * <li>arguments exactly matching the arguments in the returned type, both their types and names</li>
 * </ul>
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Assisted {

}
