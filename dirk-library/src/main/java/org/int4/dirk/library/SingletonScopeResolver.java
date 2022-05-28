package org.int4.dirk.library;

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.int4.dirk.spi.scope.AbstractScopeResolver;

/**
 * Scope resolver for singleton scope.
 */
public class SingletonScopeResolver extends AbstractScopeResolver<String> {
  private final Annotation singletonAnnotation;

  /**
   * Constructs a new instance.
   *
   * @param singleton a singleton annotation to use, cannot be {@code null}
   */
  public SingletonScopeResolver(Annotation singleton) {
    this.singletonAnnotation = Objects.requireNonNull(singleton, "singleton");
  }

  @Override
  public Annotation getAnnotation() {
    return singletonAnnotation;
  }

  @Override
  protected String getCurrentScope() {
    return "singleton";
  }
}
