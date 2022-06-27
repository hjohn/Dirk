package org.int4.dirk.core.store;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.int4.dirk.annotations.Opt;
import org.int4.dirk.api.definition.DependencyException;
import org.int4.dirk.core.InjectableFactories;
import org.int4.dirk.core.definition.BadQualifiedTypeException;
import org.int4.dirk.core.definition.Binding;
import org.int4.dirk.core.definition.ExtendedScopeResolver;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.QualifiedType;
import org.int4.dirk.core.definition.injection.Injection;
import org.int4.dirk.core.util.Key;
import org.int4.dirk.core.util.Nullable;
import org.int4.dirk.spi.instantiation.TypeTrait;
import org.int4.dirk.util.Annotations;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

public class InjectorStoreConsistencyPolicyLargeGraphTest {
  private final Random rnd = new Random(4);

  @Test
  void largeGraphTest() throws BadQualifiedTypeException, DependencyException {
    InjectableStore store = new InjectableStore(InjectableFactories.PROXY_STRATEGY);

    List<Injectable<?>> knownInjectables = new ArrayList<>();
    List<Class<?>> classes = List.of(String.class, Integer.class, A.class, B.class, C.class, D.class, E.class, F.class, G.class, H.class, I.class, J.class);

    ExtendedScopeResolver scopeResolver = mock(ExtendedScopeResolver.class);

    for(int i = 0; i < 10000; i++) {
      store.checkInvariants();

      int randomBindings = Math.min(rnd.nextInt(4), knownInjectables.size());
      List<Binding> bindings = new ArrayList<>();

      for(int j = 0; j < randomBindings; j++) {
        Injectable<?> target = knownInjectables.get(rnd.nextInt(knownInjectables.size()));

        bindings.add(new SimpleBinding(new Key(target.getType(), target.getQualifiers())));
      }

      QualifiedType qualifiedType = new QualifiedType(classes.get(rnd.nextInt(classes.size())), Set.of(Annotations.of(Named.class, Map.of("value", "instance-" + i))));

      Injectable<Object> injectable = new Injectable<>() {
        @Override
        public Type getType() {
          return qualifiedType.getType();
        }

        @Override
        public Set<Type> getTypes() {
          return Set.of(qualifiedType.getType());
        }

        @Override
        public Set<Annotation> getQualifiers() {
          return qualifiedType.getQualifiers();
        }

        @Override
        public List<Binding> getBindings() {
          return bindings;
        }

        @Override
        public ExtendedScopeResolver getScopeResolver() {
          return scopeResolver;
        }

        @Override
        public Object create(List<Injection> injections) {
          return null;
        }

        @Override
        public void destroy(Object instance) {
        }

        @Override
        public boolean needsDestroy() {
          return false;
        }
      };

      store.putAll(List.of(injectable));

      knownInjectables.add(injectable);
    }
  }

  private static class SimpleBinding implements Binding {
    private final Key key;

    public SimpleBinding(Key key) {
      this.key = key;
    }

    @Override
    public Type getType() {
      return key.getType();
    }

    @Override
    public boolean isOptional() {
      return false;
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return null;
    }

    @Override
    public Parameter getParameter() {
      return null;
    }

    @Override
    public Key getElementKey() {
      return key;
    }

    @Override
    public Set<TypeTrait> getTypeTraits() {
      return Set.of();
    }

    @Override
    public <T> T associateIfAbsent(String key, Supplier<T> valueSupplier) {
      return null;
    }
  }

  interface Z {
  }

  public static class A {
  }

  public static class E extends A {
  }

  public static class B {
    @Inject Z z;  // cyclic if it is D, not if it is H or K
  }

  public static class C {
    @Inject B b;
    @Inject @Opt Provider<D> d;  // not a circular dependency, and not required
  }

  public static class D implements Z {
    @Inject C c;
  }

  public static class F {
    @Inject B b;
    @Inject C c;
    @Inject @Nullable Z z;
  }

  public static class G {
    @Inject K k;
  }

  public static class H implements Z {
  }

  public static class I {
    @Inject Z z;
    @Inject E e;
  }

  public static class J {
    @Inject Provider<Z> h;
  }

  public static class K implements Z {
    @Inject A a;
  }
}
