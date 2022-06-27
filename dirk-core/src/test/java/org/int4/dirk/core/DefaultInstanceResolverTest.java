package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.int4.dirk.annotations.Produces;
import org.int4.dirk.api.InstanceResolver;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.api.definition.DependencyException;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.definition.ClassInjectableFactory;
import org.int4.dirk.core.definition.InjectionTargetExtensionStore;
import org.int4.dirk.core.definition.InstanceInjectableFactory;
import org.int4.dirk.core.definition.MethodInjectableFactory;
import org.int4.dirk.core.store.InjectableStore;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.spi.scope.AbstractScopeResolver;
import org.int4.dirk.util.Annotations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

public class DefaultInstanceResolverTest {
  private final AbstractScopeResolver<String> scopeResolver = new AbstractScopeResolver<>() {
    @Override
    public Annotation getAnnotation() {
      return Annotations.of(TestScoped.class);
    }

    @Override
    public String getCurrentScope() {
      return currentScope;
    }
  };

  private final ScopeResolverManager scopeResolverManager = ScopeResolverManagers.create(scopeResolver);
  private final InjectableFactories injectableFactories = new InjectableFactories(scopeResolverManager);
  private final InjectionTargetExtensionStore injectionTargetExtensionStore = injectableFactories.getInjectionTargetExtensionStore();
  private final InjectableStore store = new InjectableStore(InjectableFactories.PROXY_STRATEGY);
  private final InstantiationContextFactory instantiationContextFactory = new InstantiationContextFactory(InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.PROXY_STRATEGY, injectionTargetExtensionStore);
  private final ClassInjectableFactory classInjectableFactory = injectableFactories.forClass();
  private final MethodInjectableFactory methodInjectableFactory = injectableFactories.forMethod();
  private final InstanceInjectableFactory instanceInjectableFactory = injectableFactories.forInstance();

  private String currentScope;

  @Nested
  class WhenStoreIsEmpty {
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, instantiationContextFactory);

    @Test
    void shouldThrowExceptionWhenGettingSingleInstance() {
      assertThatThrownBy(() -> instanceResolver.getInstance(A.class))
        .isExactlyInstanceOf(UnsatisfiedResolutionException.class)
        .hasNoCause();
    }

    @Test
    void shouldReturnEmptySetWhenGettingMultipleInstances() throws CreationException {
      assertThat(instanceResolver.getInstances(A.class)).isEmpty();
    }
  }

  @Nested
  class WhenStoreNotEmpty {
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, instantiationContextFactory);

    @BeforeEach
    void beforeEach() throws DependencyException {
      try {
        store.putAll(List.of(
          classInjectableFactory.create(A.class),
          classInjectableFactory.create(B.class),
          classInjectableFactory.create(C.class),
          classInjectableFactory.create(D.class),
          classInjectableFactory.create(F.class),
          classInjectableFactory.create(G.class),
          instanceInjectableFactory.create("red", Annotations.of(Red.class)),
          instanceInjectableFactory.create("green", Annotations.of(Named.class, Map.of("value", "green"))),
          methodInjectableFactory.create(B.class.getDeclaredMethod("createH"), B.class),
          methodInjectableFactory.create(B.class.getDeclaredMethod("createI"), B.class),
          classInjectableFactory.create(K.class)
        ));
      }
      catch(NoSuchMethodException | SecurityException | DefinitionException e) {
        throw new IllegalStateException();
      }
    }

    @Test
    void getInstanceShouldReturnInstancesOfKnownTypes() throws Exception {
      assertNotNull(instanceResolver.getInstance(A.class));
      assertNotNull(instanceResolver.getInstance(String.class, Red.class));
    }

    @Test
    void getInstancesShouldReturnInstancesOfKnownTypes() throws CreationException {
      assertThat(instanceResolver.getInstances(String.class)).hasSize(2);
    }

    @Test
    void shouldFollowScopeRules() throws Exception {
      assertFalse(instanceResolver.getInstance(A.class).equals(instanceResolver.getInstance(A.class)));
      assertTrue(instanceResolver.getInstance(B.class).equals(instanceResolver.getInstance(B.class)));
      assertTrue(instanceResolver.getInstance(C.class).equals(instanceResolver.getInstance(C.class)));
    }

    @Test
    void shouldThrowScopeNotActiveExceptionWhenScopeNotActive() {
      assertThatThrownBy(() -> instanceResolver.getInstance(D.class))
        .isExactlyInstanceOf(ScopeNotActiveException.class)
        .hasMessage("Scope not active: @org.int4.dirk.core.DefaultInstanceResolverTest$TestScoped() for: Class [org.int4.dirk.core.DefaultInstanceResolverTest$D]")
        .hasNoCause();
    }

    @Test
    void shouldThrowExceptionWhenNotSingular() {
      assertThatThrownBy(() -> instanceResolver.getInstance(String.class))
        .isExactlyInstanceOf(AmbiguousResolutionException.class)
        .hasNoCause();
    }

    @Test
    void getInstancesShouldRetrieveScopedInstancesOnlyWhenActive() throws CreationException {
      assertThat(instanceResolver.getInstances(E.class)).hasSize(1);

      currentScope = "Active!";

      assertThat(instanceResolver.getInstances(E.class)).hasSize(2);
    }

    @Test
    void getInstancesShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instanceResolver.getInstances(H.class))
        .isExactlyInstanceOf(CreationException.class)
        .hasMessage("Method [org.int4.dirk.core.DefaultInstanceResolverTest$H org.int4.dirk.core.DefaultInstanceResolverTest$B.createH()] call failed")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(RuntimeException.class)
        .hasMessage("can't create H")
        .hasNoCause();
    }

    @Test
    void getInstancesShouldRetrieveSingletons() throws CreationException {
      assertThat(instanceResolver.getInstances(B.class))
        .hasSize(1)
        .containsExactlyInAnyOrderElementsOf(instanceResolver.getInstances(B.class));
    }

    @Test
    void getInstancesShouldIgnoreNullInstancesFromProducers() throws CreationException {
      assertThat(instanceResolver.getInstances(I.class)).isEmpty();
    }

    @Test
    void getInstanceShouldRejectNullInstancesFromProducers() {
      assertThatThrownBy(() -> instanceResolver.getInstance(I.class))
        .isExactlyInstanceOf(UnsatisfiedResolutionException.class)
        .hasMessage("No such instance: [org.int4.dirk.core.DefaultInstanceResolverTest$I]")
        .hasNoCause();
    }

    @Test
    void getInstanceShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instanceResolver.getInstance(H.class))
        .isExactlyInstanceOf(CreationException.class)
        .hasMessage("Method [org.int4.dirk.core.DefaultInstanceResolverTest$H org.int4.dirk.core.DefaultInstanceResolverTest$B.createH()] call failed")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(RuntimeException.class)
        .hasMessage("can't create H")
        .hasNoCause();
    }
  }

  @Scope
  @Retention(RUNTIME)
  public @interface TestScoped {
  }

  @Scope
  @Retention(RUNTIME)
  public @interface UnknownScoped {
  }

  public static class A {
  }

  @Singleton
  public static class B {
    @Produces
    H createH() {
      throw new RuntimeException("can't create H");
    }

    @Produces
    I createI() {
      return null;
    }
  }

  @Singleton
  public static class C {
  }

  @TestScoped
  public static class D {
  }

  public static class E {
  }

  @TestScoped
  public static class F extends E {
  }

  public static class G extends E {
  }

  public static class H {
  }

  public static class I {
  }

  public abstract static class J {
  }

  public static class K {
  }

  public static class L {
    @Inject K k;
    @Inject M m;
  }

  static class M {
  }
}
