package hs.ddif.core.inject.instantiator;

import hs.ddif.core.scope.OutOfScopeException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

/**
 * Bindings represent points where values can be injected into a new object. This
 * can be a field or one of the parameters of a method or constructor. When a
 * binding is required, for example for a field that must be injected, the binding
 * will have a required key. Required Bindings play an important role when ensuring
 * that all dependencies an injectable has can be resolved at runtime.
 */
public interface Binding {

  /**
   * Returns a {@link Key} this binding requires; the key refers to an injectable
   * which must be available for injection. When the binding can always supply a
   * valid value, this returns <code>null</code>. This is the case for example for
   * bindings that create a collection of matching injectables as the collection
   * can be empty if there were no matches.
   *
   * @return a {@link Key} this binding requires to resolve properly, or <code>null</code> when none are required
   */
  Key getRequiredKey();

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
  Type getType();

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