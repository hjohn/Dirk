package hs.ddif.core.scope;

import hs.ddif.core.inject.instantiator.BeanResolutionException;

import java.lang.reflect.Type;

/**
 * Thrown when a scoped bean is required without the appropriate scope being active.
 */
public class OutOfScopeException extends BeanResolutionException {

  public OutOfScopeException(Type type, String message) {
    super(type, message);
  }
}
