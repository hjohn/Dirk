package hs.ddif.spi.instantiation;

import hs.ddif.api.instantiation.InstanceCreationException;
import hs.ddif.api.instantiation.Key;
import hs.ddif.api.instantiation.MultipleInstancesException;

import java.util.List;

/**
 * Context used to create instances for given {@link Key}s.
 */
public interface InstantiationContext {

  /**
   * Creates the instance associated with the given {@link Key}. If the key given
   * matches multiple instances a {@link MultipleInstancesException} exception is thrown.
   * Returns {@code null} if nothing matched.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key}, cannot be {@code null}
   * @return an instance or {@code null} if there were no matches
   * @throws InstanceCreationException when the creation of the instance failed
   * @throws MultipleInstancesException when the key matched multiple potential instances
   */
  <T> T create(Key key) throws InstanceCreationException, MultipleInstancesException;

  /**
   * Creates all instances for all known types associated with the given {@link Key}.
   * Scoped types which scope is currently inactive are excluded.
   *
   * @param <T> the type of the instances
   * @param key a {@link Key}, cannot be {@code null}
   * @return a list of instances, never {@code null} and never contains {@code null}
   * @throws InstanceCreationException when the creation of an instance failed
   */
  <T> List<T> createAll(Key key) throws InstanceCreationException;

  /**
   * <p>
   * Determines if there is no bean that matches the required type and qualifiers and is eligible for injection into the class
   * into which the parent <code>Instance</code> was injected.
   * </p>
   *
   * @return <code>true</code> if there is no bean that matches the required type and qualifiers and is eligible for injection
   *         into the class into which the parent <code>Instance</code> was injected, or <code>false</code> otherwise.
   */
  boolean isUnsatisfied(Key key);

  /**
   * <p>
   * Determines if there is more than one bean that matches the required type and qualifiers and is eligible for injection
   * into the class into which the parent <code>Instance</code> was injected.
   * </p>
   *
   * @return <code>true</code> if there is more than one bean that matches the required type and qualifiers and is eligible for
   *         injection into the class into which the parent <code>Instance</code> was injected, or <code>false</code> otherwise.
   */
  boolean isAmbiguous(Key key);

  /**
   * <p>
   * Determines if there is exactly one bean that matches the required type and qualifiers and is eligible for injection
   * into the class into which the parent <code>Instance</code> was injected.
   * </p>
   *
   * @since 2.0
   * @return <code>true</code> if there is exactly one bean that matches the required type and qualifiers and is eligible for
   *         injection into the class into which the parent <code>Instance</code> was injected, or <code>false</code> otherwise.
   */
  boolean isResolvable(Key key);
}

