package hs.ddif.core;

import hs.ddif.core.bind.Parameter;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Inject;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to indicate a the given factory class (producer) should be constructed which is responsible for producing instances
 * of this class.<p>
 *
 * If the producer has a single abstract method (either in an interface or in an abstract class)
 * which has the correct signature:
 * <ul>
 * <li>return type must match the annotated class</li>
 * <li>the number of parameters of the abstract method matches the number of {@link Parameter} and {@link Inject} annotated injections in the annotated class</li>
 * <li>the types of the parameters match with the {@link Parameter} injections</li>
 * <li>the names of the parameters match with the {@link Parameter} injections; note that these may need to be explicitely annotated with {@link Parameter} if names cannot be determined by reflection</li>
 * </ul>
 *
 * then an implementation will be provided for the abstract method to produce the instances.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Producer {

  /**
   * The producer class to use.
   *
   * @return the producer class to use, cannot be null
   */
  Class<?> value();
}
