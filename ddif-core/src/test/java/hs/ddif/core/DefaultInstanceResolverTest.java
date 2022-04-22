package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.api.InstanceResolver;
import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.instantiation.InstanceCreationException;
import hs.ddif.api.instantiation.MultipleInstancesException;
import hs.ddif.api.instantiation.NoSuchInstanceException;
import hs.ddif.api.util.Annotations;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.core.inject.store.InstantiatorBindingMap;
import hs.ddif.core.inject.store.UnresolvableDependencyException;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.scope.AbstractScopeResolver;
import hs.ddif.spi.scope.OutOfScopeException;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
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
    public Class<? extends Annotation> getAnnotationClass() {
      return TestScoped.class;
    }

    @Override
    public String getCurrentScope() {
      return currentScope;
    }
  };

  private final ScopeResolverManager scopeResolverManager = ScopeResolverManagers.create(scopeResolver);
  private final InjectableFactories injectableFactories = new InjectableFactories(scopeResolverManager);
  private final TypeExtensionStore typeExtensionStore = injectableFactories.getTypeExtensionStore();
  private final InstantiatorFactory instantiatorFactory = new DefaultInstantiatorFactory(typeExtensionStore);
  private final InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
  private final InjectableStore store = new InjectableStore(instantiatorBindingMap, InjectableFactories.PROXY_STRATEGY);
  private final InstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap, InjectableFactories.PROXY_STRATEGY);
  private final ClassInjectableFactory classInjectableFactory = injectableFactories.forClass();
  private final MethodInjectableFactory methodInjectableFactory = injectableFactories.forMethod();
  private final FieldInjectableFactory fieldInjectableFactory = injectableFactories.forField();
  private final InstanceInjectableFactory instanceInjectableFactory = injectableFactories.forInstance();

  private String currentScope;

  @Nested
  class WhenStoreIsEmpty {
    private final DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(false, List.of(), instantiatorFactory, classInjectableFactory, methodInjectableFactory, fieldInjectableFactory);
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);

    @Test
    void shouldThrowExceptionWhenGettingSingleInstance() {
      assertThatThrownBy(() -> instanceResolver.getInstance(A.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasNoCause();
    }

    @Test
    void shouldReturnEmptySetWhenGettingMultipleInstances() throws InstanceCreationException {
      assertThat(instanceResolver.getInstances(A.class)).isEmpty();
    }
  }

  @Nested
  class WhenStoreNotEmpty {
    private final DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(false, List.of(), instantiatorFactory, classInjectableFactory, methodInjectableFactory, fieldInjectableFactory);
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);

    {
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
    void getInstancesShouldReturnInstancesOfKnownTypes() throws InstanceCreationException {
      assertThat(instanceResolver.getInstances(String.class)).hasSize(2);
    }

    @Test
    void shouldFollowScopeRules() throws Exception {
      assertFalse(instanceResolver.getInstance(A.class).equals(instanceResolver.getInstance(A.class)));
      assertTrue(instanceResolver.getInstance(B.class).equals(instanceResolver.getInstance(B.class)));
      assertTrue(instanceResolver.getInstance(C.class).equals(instanceResolver.getInstance(C.class)));
    }

    @Test
    void shouldThrowOutOfScopeExceptionWhenScopeNotActive() {
      assertThatThrownBy(() -> instanceResolver.getInstance(D.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
        .hasMessage("[class hs.ddif.core.DefaultInstanceResolverTest$D] could not be created")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(OutOfScopeException.class)
        .hasMessage("Scope not active: interface hs.ddif.core.DefaultInstanceResolverTest$TestScoped for: Injectable[hs.ddif.core.DefaultInstanceResolverTest$D]")
        .hasNoCause();
    }

    @Test
    void shouldThrowExceptionWhenNotSingular() {
      assertThatThrownBy(() -> instanceResolver.getInstance(String.class))
        .isExactlyInstanceOf(MultipleInstancesException.class)
        .hasNoCause();
    }

    @Test
    void getInstancesShouldRetrieveScopedInstancesOnlyWhenActive() throws InstanceCreationException {
      assertThat(instanceResolver.getInstances(E.class)).hasSize(1);

      currentScope = "Active!";

      assertThat(instanceResolver.getInstances(E.class)).hasSize(2);
    }

    @Test
    void getInstancesShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instanceResolver.getInstances(H.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
        .hasMessage("Method [hs.ddif.core.DefaultInstanceResolverTest$H hs.ddif.core.DefaultInstanceResolverTest$B.createH()] call failed")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(InvocationTargetException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(RuntimeException.class)
        .hasMessage("can't create H")
        .hasNoCause();
    }

    @Test
    void getInstancesShouldRetrieveSingletons() throws InstanceCreationException {
      assertThat(instanceResolver.getInstances(B.class))
        .hasSize(1)
        .containsExactlyInAnyOrderElementsOf(instanceResolver.getInstances(B.class));
    }

    @Test
    void getInstancesShouldIgnoreNullInstancesFromProducers() throws InstanceCreationException {
      assertThat(instanceResolver.getInstances(I.class)).isEmpty();
    }

    @Test
    void getInstanceShouldRejectNullInstancesFromProducers() {
      assertThatThrownBy(() -> instanceResolver.getInstance(I.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasMessage("No such instance: [hs.ddif.core.DefaultInstanceResolverTest$I]")
        .hasNoCause();
    }

    @Test
    void getInstanceShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instanceResolver.getInstance(H.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
        .hasMessage("Method [hs.ddif.core.DefaultInstanceResolverTest$H hs.ddif.core.DefaultInstanceResolverTest$B.createH()] call failed")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(InvocationTargetException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(RuntimeException.class)
        .hasMessage("can't create H")
        .hasNoCause();
    }
  }

  @Nested
  class WhenStoreEmptyAndAutoDiscoveryIsActive {
    private final DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(true, List.of(), instantiatorFactory, classInjectableFactory, methodInjectableFactory, fieldInjectableFactory);
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);

    @Test
    void getInstancesShouldNeverDiscoverTypes() throws InstanceCreationException {
      assertThat(instanceResolver.getInstances(A.class)).isEmpty();
      assertThat(instanceResolver.getInstances(B.class)).isEmpty();
      assertThat(instanceResolver.getInstances(C.class)).isEmpty();
    }

    @Test
    void getInstanceShouldDiscoverNewTypes() throws Exception {
      assertNotNull(instanceResolver.getInstance(A.class));
    }

    @Test
    void getInstanceShouldNotDiscoverTypesWithQualifiers() {
      assertThatThrownBy(() -> instanceResolver.getInstance(A.class, Red.class))
        .isExactlyInstanceOf(AutoDiscoveryException.class)
        .hasMessage("Unable to instantiate [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.DefaultInstanceResolverTest$A]")
        .hasNoSuppressedExceptions()
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(DefinitionException.class)
        .hasMessage("[class hs.ddif.core.DefaultInstanceResolverTest$A] is missing the required qualifiers: [@hs.ddif.core.test.qualifiers.Red()]")
        .hasNoSuppressedExceptions()
        .hasNoCause();
    }

    @Test
    void getInstanceShouldNotRegisterUnresolvableDependencies() {
      assertThatThrownBy(() -> instanceResolver.getInstance(L.class))
        .isExactlyInstanceOf(AutoDiscoveryException.class)
        .hasMessage("[hs.ddif.core.DefaultInstanceResolverTest$L] and the discovered types [Injectable[hs.ddif.core.DefaultInstanceResolverTest$K], Injectable[hs.ddif.core.DefaultInstanceResolverTest$L]] could not be registered\n"
          + "    -> [hs.ddif.core.DefaultInstanceResolverTest$M] required by [hs.ddif.core.DefaultInstanceResolverTest$L], via Field [hs.ddif.core.DefaultInstanceResolverTest$M hs.ddif.core.DefaultInstanceResolverTest$L.m], is not registered and cannot be discovered (reason: [class hs.ddif.core.DefaultInstanceResolverTest$M] could not be bound because [class hs.ddif.core.DefaultInstanceResolverTest$M] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)")
        .hasNoSuppressedExceptions()
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(UnresolvableDependencyException.class)
        .hasMessage("Missing dependency [hs.ddif.core.DefaultInstanceResolverTest$M] required for Field [hs.ddif.core.DefaultInstanceResolverTest$M hs.ddif.core.DefaultInstanceResolverTest$L.m]")
        .hasNoSuppressedExceptions()
        .hasNoCause();
    }

    @Test
    void getInstanceShouldThrowExceptionWhenDiscoveryFails() {
      assertThatThrownBy(() -> instanceResolver.getInstance(J.class))
        .isExactlyInstanceOf(AutoDiscoveryException.class)
        .hasMessage("Unable to instantiate [hs.ddif.core.DefaultInstanceResolverTest$J]")
        .hasNoSuppressedExceptions()
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(DefinitionException.class)
        .hasMessage("[class hs.ddif.core.DefaultInstanceResolverTest$J] cannot be abstract")
        .hasNoSuppressedExceptions()
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
