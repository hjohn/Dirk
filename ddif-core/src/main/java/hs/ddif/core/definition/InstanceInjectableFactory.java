package hs.ddif.core.definition;

import hs.ddif.core.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Constructs {@link Injectable}s for a given object instance.
 */
public class InstanceInjectableFactory {
  private final InjectableFactory factory;
  private final ScopeResolver scopeResolver;

  /**
   * Constructs a new instance.
   *
   * @param factory a {@link InjectableFactory}, cannot be {@code null}
   * @param scopeResolver a {@link ScopeResolver}, cannot be {@code null}
   */
  public InstanceInjectableFactory(InjectableFactory factory, ScopeResolver scopeResolver) {
    this.factory = factory;
    this.scopeResolver = scopeResolver;
  }

  /**
   * Creates a new {@link Injectable}.
   *
   * @param instance an instance, cannot be {@code null}
   * @param qualifiers an array of qualifier {@link Annotation}s
   * @return a new {@link Injectable}, never {@code null}
   */
  public Injectable create(Object instance, Annotation... qualifiers) {
    if(instance == null) {
      throw new IllegalArgumentException("instance cannot be null");
    }

    try {
      return factory.create(
        new QualifiedType(instance.getClass(), Set.of(qualifiers)),
        List.of(),
        scopeResolver,
        instance,
        injections -> instance
      );
    }
    catch(BadQualifiedTypeException e) {
      throw new DefinitionException(e);
    }
  }
}
