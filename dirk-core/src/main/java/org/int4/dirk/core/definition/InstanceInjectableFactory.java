package org.int4.dirk.core.definition;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.definition.injection.Constructable;
import org.int4.dirk.core.definition.injection.Injection;

/**
 * Constructs {@link Injectable}s for a given object instance.
 */
public class InstanceInjectableFactory {
  private final InjectableFactory factory;
  private final Annotation singleton;

  /**
   * Constructs a new instance.
   *
   * @param factory a {@link InjectableFactory}, cannot be {@code null}
   * @param singleton a singleton annotation to use, cannot be {@code null}
   */
  public InstanceInjectableFactory(InjectableFactory factory, Annotation singleton) {
    this.factory = Objects.requireNonNull(factory, "factory");
    this.singleton = Objects.requireNonNull(singleton, "singleton");
  }

  /**
   * Creates a new {@link Injectable}.
   *
   * @param <T> the type of the given instance
   * @param instance an instance, cannot be {@code null}
   * @param qualifiers an array of qualifier {@link Annotation}s
   * @return a new {@link Injectable}, never {@code null}
   * @throws DefinitionException when a definition problem was encountered
   */
  public <T> Injectable<T> create(T instance, Annotation... qualifiers) throws DefinitionException {
    if(instance == null) {
      throw new IllegalArgumentException("instance cannot be null");
    }

    Annotation[] extendedQualifiers = Arrays.copyOf(qualifiers, qualifiers.length + 1);

    extendedQualifiers[extendedQualifiers.length - 1] = singleton;

    return factory.create(
      instance.getClass(),
      null,
      new FakeAnnotatedElement(instance, extendedQualifiers),
      List.of(),
      new Constructable<>() {
        @Override
        public T create(List<Injection> injections) {
          return instance;
        }

        @Override
        public void destroy(T instance) {
        }

        @Override
        public boolean needsDestroy() {
          return false;
        }
      }
    );
  }

  private static final class FakeAnnotatedElement implements AnnotatedElement {
    private final Object instance;
    private final Annotation[] qualifiers;

    FakeAnnotatedElement(Object instance, Annotation... qualifiers) {
      this.instance = instance;
      this.qualifiers = qualifiers;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
      return (T)Arrays.stream(qualifiers).filter(q -> q.annotationType().equals(annotationClass)).findFirst().orElse(null);
    }

    @Override
    public Annotation[] getAnnotations() {
      return qualifiers.clone();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
      return qualifiers.clone();
    }

    @Override
    public int hashCode() {
      return instance.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }
      if(obj == null || getClass() != obj.getClass()) {
        return false;
      }

      FakeAnnotatedElement other = (FakeAnnotatedElement)obj;

      return Objects.equals(instance, other.instance);
    }

    @Override
    public String toString() {
      return instance.toString();
    }
  }
}
