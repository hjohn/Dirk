package hs.ddif.core;

import java.lang.reflect.Type;

/**
 * Bindings are used to resolve values that can be injected into a new object.  Bindings
 * can have a required key, meaning that the Binding cannot be resolved properly
 * without it.  Required Bindings play an important role when ensuring that all
 * dependencies an injectable has can be resolved at runtime.
 */
public interface Binding {

  /**
   * Returns the current value of this binding.<p>
   *
   * If <code>null</code> is returned, any default value should be left intact (only relevant
   * for field injection).  Whether <code>null</code> can be returned (as opposed to a {@link NoSuchBeanException})
   * is determined by the presence of a <code>Nullable</code> annotation at the injection site.<p>
   *
   * Bindings determine how and when they return <code>null</code>.  For a List or Set binding for
   * example, a binding could return an empty list or set or <code>null</code> depending on the
   * presence of the <code>Nullable</code> annotation.  Some examples:<p>
   *
   * <ul>
   * <li>
   *   <code>@Inject private List&lt;Employee&gt; employees;</code><br>
   *   Will always result in a <code>List</code> being injected, although it might be empty.  There
   *   is no required key.  This will also overwrite any default value the field might have.<br><br>
   * </li>
   * <li>
   *   <code>@Inject @Named("config.delay") private Long delay = 15L;</code><br>
   *   Requires the presence of a <code>Long</code> named "config.delay", and will fail if it is not.  The default value of the field is irrelevant -- it always gets overwritten.<br><br>
   * </li>
   * <li>
   *   <code>@Inject @Nullable private List&lt;Employee&gt; employees;</code><br>
   *   Will only inject a value if the <code>List</code> is not empty, otherwise will leave the default value as is.<br><br>
   * </li>
   * <li>
   *   <code>@Inject @Nullable @Named("config.delay") private Long delay = 15L;</code><br>
   *   Will only inject <code>delay</code> if a suitable <code>Long</code> is available, otherwise will leave it as-is.
   * </li>
   * </ul>
   *
   * @param injector an injector for resolving dependencies
   * @return the current value of this binding
   */
  Object getValue(Injector injector);

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