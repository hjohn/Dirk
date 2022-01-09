package hs.ddif.core.inject.injectable;

import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

/**
 * Constructs {@link Injectable}s for a given object instance.
 */
public class InstanceInjectableFactory {
  private static final Annotation SINGLETON = Annotations.of(Singleton.class);

  private final InjectableFactory factory;

  /**
   * Constructs a new instance.
   *
   * @param factory a {@link InjectableFactory}, cannot be null
   */
  public InstanceInjectableFactory(InjectableFactory factory) {
    this.factory = factory;
  }

  /**
   * Creates a new {@link Injectable}.
   *
   * @param instance an instance, cannot be null
   * @param qualifiers an array of qualifier {@link Annotation}s
   * @return a new {@link Injectable}, never null
   */
  public Injectable create(Object instance, Annotation... qualifiers) {
    if(instance == null) {
      throw new IllegalArgumentException("instance cannot be null");
    }

    return factory.create(
      instance.getClass(),
      Set.of(qualifiers),
      List.of(),
      SINGLETON,
      instance,
      injections -> instance
    );
  }
}
