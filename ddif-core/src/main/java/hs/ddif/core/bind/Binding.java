package hs.ddif.core.bind;

import java.lang.reflect.Type;

/**
 * Bindings are used to resolve values that can be injected into a new object.  Bindings
 * can have a required key, meaning that the Binding cannot be resolved properly
 * without it.  Required Bindings play an important role when ensuring that all
 * dependencies an injectable has can be resolved at runtime.
 */
public interface Binding {

  /**
   * Returns the Key this binding requires to resolve properly.  If <code>null</code>
   * this means the binding can supply a valid value at all times.  For example, a
   * Collection Binding can always return an empty collection if there are no valid
   * candidates for it.
   *
   * @return the Key this binding requires to resolve properly, or <code>null</code> if none are required
   */
  Key getRequiredKey();

  /**
   * Returns the {@link Type} of the binding.
   *
   * @return the {@link Type} of the binding, never null
   */
  Type getType();

  /**
   * Returns <code>true</code> if the binding is parameterized, otherwise <code>false</code>
   *
   * @return <code>true</code> if the binding is parameterized, otherwise <code>false</code>
   */
  boolean isParameter();

  /**
   * Returns whether this binding is for a Provider member or parameter (not whether or not
   * the value is *supplied* by a Provider).
   *
   * @return <code>true</code> if the injection site is a Provider, otherwise <code>false</code>
   */
  boolean isProvider();
}