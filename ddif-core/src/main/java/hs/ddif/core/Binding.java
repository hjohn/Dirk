package hs.ddif.core;

/**
 * Bindings are used to resolve values that can be injected into a new object.  Bindings
 * can have a required key, meaning that the Binding cannot be resolved properly
 * without it.  Required Bindings play an important role when ensuring that all
 * dependencies an injectable has can be resolved at runtime.
 */
public interface Binding {

  /**
   * Returns the current value of this binding.
   *
   * @param injector an injector for resolving dependencies
   * @return the current value of this binding
   */
  Object getValue(Injector injector);

  /**
   * Returns the Keys this binding requires to resolve properly.  If there are
   * none (empty array) this means the binding can supply a valid value at all
   * times.  For example, a Collection Binding can always return an empty
   * collection if there are no valid candidates for it.
   *
   * @return the Keys this binding requires to resolve properly, or an empty array if none are required
   */
  Key[] getRequiredKeys();

  /**
   * Returns whether or not this binding is optional.
   *
   * @return whether or not this binding is optional
   */
  boolean isOptional();
}