package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Instantiator;

public interface ResolvableBinding extends Binding {

  /**
   * Returns the current value of this binding.<p>
   *
   * If <code>null</code> is returned, any default value should be left intact (only relevant
   * for field injection).  Whether <code>null</code> can be returned (as opposed to a {@link BeanResolutionException})
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
   * @param instantiator an {@link Instantiator} for resolving dependencies
   * @return the current value of this binding, can be null
   * @throws BeanResolutionException when a bean could not be found
   */
  Object getValue(Instantiator instantiator) throws BeanResolutionException;
}