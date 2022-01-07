package hs.ddif.core.inject.instantiator;

import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.store.Key;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * Bindings represent targets where values can be injected into an instance. This
 * can be a field or one of the parameters of a method or constructor.
 * <p>
 * Bindings differ in target, time of resolution and the number of matches allowed.
 *
 * <h2>Target</h2>
 * The target of a binding is determined by the {@link AccessibleObject} and the given {@link Parameter}, it can be:
 * <ul>
 * <li>A constructor. The parameter indicates which constructor parameter is the target.</li>
 * <li>A method. The parameter indicates which method parameter is the target.</li>
 * <li>A field. Parameter is {@code null}.</li>
 * <li>An owner class. In order to access non-static methods and fields the owner class is required as a binding. Both the parameter and the accessible object are {@code null} in this case.</li>
 * </ul>
 * <h2>Time of Resolution</h2>
 * A binding can be resolved immediately (when an object is constructed and injected) or at some later point in time (or never) if its value
 * is accessed through a {@link javax.inject.Provider}. This is indicated by the {@code direct} flag. Bindings which are not resolved immediately
 * can be used to break circular dependencies and access dependencies in a smaller scope. They can also be acquired multiple times serving as a factory.
 * <h2>Allowed matches</h2>
 * A standard binding which supplies a dependency normally requires the dependency to be available exactly once. An optional binding allows for a
 * dependency to be unavailable or available exactly once. A collection binding has no restrictions.
 * <table border="1">
 * <tr><th>Collection?</th><th>Optional?</th><th>Allowed matches</th></tr>
 * <tr><td>{@code false}</td><td>{@code false}</td><td>1</td>
 * <tr><td>{@code false}</td><td>{@code true}</td><td>0..1</td>
 * <tr><td>{@code true}</td><td>{@code true}/{@code false}</td><td>0 or more</td>
 * </table>
 */
public interface Binding {

  /**
   * Returns the {@link Key} of this binding.
   *
   * @return the {@link Key} of this binding, never null
   */
  Key getKey();

  /**
   * Returns the target {@link AccessibleObject} for the binding. This is null
   * when the binding refers to the declaring class which is required to access a
   * non-static field or method.
   *
   * @return the target @link AccessibleObject} for the binding, can be null
   */
  AccessibleObject getAccessibleObject();

  /**
   * Returns the {@link Parameter} when the {@link AccessibleObject} is a
   * constructor or a method. Returns @{code null} for fields.
   *
   * @return a {@link Parameter}, can be null
   */
  Parameter getParameter();

  /**
   * Returns the {@link Type} of the binding.
   *
   * @return the {@link Type} of the binding, never null
   */
  default Type getType() {
    return getKey().getType();
  }

  /**
   * Returns {@code true} when the target for this binding is a collection type. The {@link Key}
   * represents the type of the elements in the collection.
   *
   * @return {@code true} when the target for this binding is a collection type, otherwise {@code false}
   */
  boolean isCollection();

  /**
   * Returns {@code true} when the target for this binding is to be immediately resolved to a target.
   * When a binding is not direct, resolution is delayed until the target is accessed, for example, using
   * a {@code Provider}.
   *
   * @return {@code true} when the target for this binding is to be immediately resolved to a target, otherwise {@code false}
   */
  boolean isDirect();

  /**
   * Returns {@code true} when the target for this binding also allows leaving it unbound. For fields this
   * means the default value will be left unaltered, while for methods and constructors {@code null} will
   * be supplied if no source is available to satisfy the binding.
   *
   * @return {@code true} when the target for this binding also allows leaving it unbound, otherwise {@code false}
   */
  boolean isOptional();

  /**
   * Returns the current value of this binding.<p>
   *
   * If <code>null</code> is returned, any default value should be left intact (only relevant
   * for field injection).  Whether <code>null</code> can be returned (as opposed to a {@link InstanceCreationFailure})
   * is determined by the presence of a <code>Nullable</code> annotation at the injection site.<p>
   *
   * Bindings determine how and when they return <code>null</code>.  For a List or Set binding for
   * example, a binding could return an empty list or set or <code>null</code> depending on the
   * presence of the <code>Nullable</code> annotation.  Some examples:
   *
   * <ul>
   * <li>
   *   <code>@Inject private List&lt;Employee&gt; employees;</code><br>
   *   Will always result in a <code>List</code> being injected, although it might be empty.  There
   *   is no required key.  This will also overwrite any default value the field might have.<br><br>
   * </li>
   * <li>
   *   <code>@Inject @Named("config.delay") private Long delay = 15L;</code><br>
   *   Requires the presence of a <code>Long</code> named "config.delay", and will fail if it is not.
   *   The default value of the field is irrelevant -- it always gets overwritten.<br><br>
   * </li>
   * <li>
   *   <code>@Inject @Nullable private List&lt;Employee&gt; employees;</code><br>
   *   Will only inject a value if the <code>List</code> is not empty, otherwise will leave the default value as is.<br><br>
   * </li>
   * <li>
   *   <code>@Inject @Nullable @Named("config.delay") private Long delay = 15L;</code><br>
   *   Will only inject <code>delay</code> if a suitable <code>Long</code> is available, otherwise will leave it as is.
   * </li>
   * </ul>
   *
   * @param instantiator an {@link Instantiator} for creating further dependencies, cannot be null
   * @return the current value of this binding, or {@code null} when binding unavailable and its optional
   * @throws OutOfScopeException when out of scope
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  Object getValue(Instantiator instantiator) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance, OutOfScopeException;
}