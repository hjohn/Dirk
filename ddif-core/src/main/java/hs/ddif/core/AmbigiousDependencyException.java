package hs.ddif.core;

import hs.ddif.core.store.Injectable;

import java.util.Collection;

/**
 * Thrown when an attempt is made to register a Bean that requires a singular dependency
 * but multiple matches are available.
 */
public class AmbigiousDependencyException extends DependencyException {

  public AmbigiousDependencyException(Class<?> beanClass, Key key, Collection<? extends Injectable> registeredBeanClasses) {
    super(key + " is provided by " + registeredBeanClasses + ", but only one expected for: " + beanClass);
  }
}
