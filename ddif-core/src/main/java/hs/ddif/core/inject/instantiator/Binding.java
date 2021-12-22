package hs.ddif.core.inject.instantiator;

import hs.ddif.core.scope.OutOfScopeException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

/**
 * Bindings represent targets where values can be injected into an instance. This
 * can be a field or one of the parameters of a method or constructor.
 */
public interface Binding {

  /**
   * Returns the {@link Key} of this binding.
   *
   * @return the {@link Key} of this binding, never null
   */
  Key getKey();

  /**
   * Returns the target {@link AccessibleObject} for the binding.
   *
   * @return the target @link AccessibleObject} for the binding, never null
   */
  AccessibleObject getAccessibleObject();

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
   * Returns <code>true</code> if the binding is to be supplied as parameter, otherwise <code>false</code>
   *
   * @return <code>true</code> if the binding is to be supplied as parameter, otherwise <code>false</code>
   */
  boolean isParameter();

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