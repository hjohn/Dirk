package hs.ddif.core.config.standard;

import hs.ddif.annotations.Produces;
import hs.ddif.core.api.InstanceCreationException;
import hs.ddif.core.api.InstanceResolver;
import hs.ddif.core.api.MultipleInstancesException;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.config.discovery.DiscoveryFailure;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.definition.InjectableFactories;
import hs.ddif.core.definition.InstanceInjectableFactory;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.inject.store.InjectableStore;
import hs.ddif.core.inject.store.InstantiatorBindingMap;
import hs.ddif.core.instantiation.DefaultInstantiatorFactory;
import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.TypeExtensionStore;
import hs.ddif.core.instantiation.TypeExtensionStores;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.domain.MultipleInstances;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.scope.AbstractScopeResolver;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolverManager;
import hs.ddif.core.scope.ScopeResolverManagers;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Scope;
import javax.inject.Singleton;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultInstanceResolverTest {
  private final AbstractScopeResolver<String> scopeResolver = new AbstractScopeResolver<>() {
    @Override
    public Class<? extends Annotation> getScopeAnnotationClass() {
      return TestScoped.class;
    }

    @Override
    public String getCurrentScope() {
      return currentScope;
    }
  };

  private final TypeExtensionStore typeExtensionStore = TypeExtensionStores.create();
  private final InstantiatorFactory instantiatorFactory = new DefaultInstantiatorFactory(typeExtensionStore);
  private final InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
  private final ScopeResolverManager scopeResolverManager = ScopeResolverManagers.create(scopeResolver);
  private final InjectableStore store = new InjectableStore(instantiatorBindingMap, typeExtensionStore.getExtendedTypes());
  private final InstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap);
  private final InjectableFactories injectableFactories = new InjectableFactories(scopeResolverManager);
  private final ClassInjectableFactory classInjectableFactory = injectableFactories.forClass();
  private final MethodInjectableFactory methodInjectableFactory = injectableFactories.forMethod();
  private final InstanceInjectableFactory instanceInjectableFactory = injectableFactories.forInstance();

  private String currentScope;

  @Nested
  class WhenStoreIsEmpty {
    private final DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(false, List.of(), classInjectableFactory);
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);

    @Test
    void shouldThrowExceptionWhenGettingSingleInstance() {
      assertThatThrownBy(() -> instanceResolver.getInstance(A.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasNoCause();
    }

    @Test
    void shouldReturnEmptySetWhenGettingMultipleInstances() {
      assertThat(instanceResolver.getInstances(A.class)).isEmpty();
    }
  }

  @Nested
  class WhenStoreNotEmpty {
    private final DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(false, List.of(), classInjectableFactory);
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
      catch(NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException();
      }
    }

    @Test
    void getInstanceShouldReturnInstancesOfKnownTypes() {
      assertNotNull(instanceResolver.getInstance(A.class));
      assertNotNull(instanceResolver.getInstance(String.class, Red.class));
    }

    @Test
    void getInstancesShouldReturnInstancesOfKnownTypes() {
      assertThat(instanceResolver.getInstances(String.class)).hasSize(2);
    }

    @Test
    void shouldFollowScopeRules() {
      assertFalse(instanceResolver.getInstance(A.class).equals(instanceResolver.getInstance(A.class)));
      assertTrue(instanceResolver.getInstance(B.class).equals(instanceResolver.getInstance(B.class)));
      assertTrue(instanceResolver.getInstance(C.class).equals(instanceResolver.getInstance(C.class)));
    }

    @Test
    void shouldThrowOutOfScopeExceptionWhenScopeNotActive() {
      assertThatThrownBy(() -> instanceResolver.getInstance(D.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
        .hasMessage("[class hs.ddif.core.config.standard.DefaultInstanceResolverTest$D] could not be created")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(InstanceCreationFailure.class)
        .hasMessage("[class hs.ddif.core.config.standard.DefaultInstanceResolverTest$D] could not be created")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(OutOfScopeException.class)
        .hasMessage("Scope not active: interface hs.ddif.core.config.standard.DefaultInstanceResolverTest$TestScoped for: Injectable[hs.ddif.core.config.standard.DefaultInstanceResolverTest$D]")
        .hasNoCause();
    }

    @Test
    void shouldThrowExceptionWhenNotSingular() {
      assertThatThrownBy(() -> instanceResolver.getInstance(String.class))
        .isExactlyInstanceOf(MultipleInstancesException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(MultipleInstances.class)
        .hasNoCause();
    }

    @Test
    void getInstancesShouldRetrieveScopedInstancesOnlyWhenActive() {
      assertThat(instanceResolver.getInstances(E.class)).hasSize(1);

      currentScope = "Active!";

      assertThat(instanceResolver.getInstances(E.class)).hasSize(2);
    }

    @Test
    void getInstancesShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instanceResolver.getInstances(H.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
        .hasMessage("Method [hs.ddif.core.config.standard.DefaultInstanceResolverTest$H hs.ddif.core.config.standard.DefaultInstanceResolverTest$B.createH()] call failed")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(InstanceCreationFailure.class)
        .hasMessage("Method [hs.ddif.core.config.standard.DefaultInstanceResolverTest$H hs.ddif.core.config.standard.DefaultInstanceResolverTest$B.createH()] call failed")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(InvocationTargetException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(RuntimeException.class)
        .hasMessage("can't create H")
        .hasNoCause();
    }

    @Test
    void getInstancesShouldRetrieveSingletons() {
      assertThat(instanceResolver.getInstances(B.class))
        .hasSize(1)
        .containsExactlyInAnyOrderElementsOf(instanceResolver.getInstances(B.class));
    }

    @Test
    void getInstancesShouldIgnoreNullInstancesFromProducers() {
      assertThat(instanceResolver.getInstances(I.class)).isEmpty();
    }

    @Test
    void getInstanceShouldRejectNullInstancesFromProducers() {
      assertThatThrownBy(() -> instanceResolver.getInstance(I.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasMessage("No such instance: [hs.ddif.core.config.standard.DefaultInstanceResolverTest$I]")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasMessage("No such instance: [hs.ddif.core.config.standard.DefaultInstanceResolverTest$I]")
        .hasNoCause();
    }

    @Test
    void getInstanceShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instanceResolver.getInstance(H.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
        .hasMessage("Method [hs.ddif.core.config.standard.DefaultInstanceResolverTest$H hs.ddif.core.config.standard.DefaultInstanceResolverTest$B.createH()] call failed")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(InstanceCreationFailure.class)
        .hasMessage("Method [hs.ddif.core.config.standard.DefaultInstanceResolverTest$H hs.ddif.core.config.standard.DefaultInstanceResolverTest$B.createH()] call failed")
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
    private final DefaultDiscovererFactory discovererFactory = new DefaultDiscovererFactory(true, List.of(), classInjectableFactory);
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, discovererFactory, instantiationContext, instantiatorFactory);

    @Test
    void getInstancesShouldNeverDiscoverTypes() {
      assertThat(instanceResolver.getInstances(A.class)).isEmpty();
      assertThat(instanceResolver.getInstances(B.class)).isEmpty();
      assertThat(instanceResolver.getInstances(C.class)).isEmpty();
    }

    @Test
    void getInstanceShouldDiscoverNewTypes() {
      assertNotNull(instanceResolver.getInstance(A.class));
    }

    @Test
    void getInstanceShouldNotDiscoverTypesWithQualifiers() {
      assertThatThrownBy(() -> instanceResolver.getInstance(A.class, Red.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
        .hasMessage("[@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.standard.DefaultInstanceResolverTest$A] instantiation failed because auto discovery was unable to resolve all dependencies; found: []")
        .hasNoSuppressedExceptions()
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(DiscoveryFailure.class)
        .hasMessage("[@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.standard.DefaultInstanceResolverTest$A] instantiation failed because auto discovery was unable to resolve all dependencies; found: []")
        .hasNoSuppressedExceptions()
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(DefinitionException.class)
        .hasMessage("Exception occurred during discovery via path: [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.standard.DefaultInstanceResolverTest$A]")
        .satisfies(throwable -> {
          assertThat(throwable.getSuppressed()).hasSize(1);
          assertThat(throwable.getSuppressed()[0])
            .isExactlyInstanceOf(DefinitionException.class)
            .hasMessage("[class hs.ddif.core.config.standard.DefaultInstanceResolverTest$A] found during auto discovery is missing qualifiers required by: [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.config.standard.DefaultInstanceResolverTest$A]")
            .hasNoCause();
        })
        .hasNoCause();
    }

    @Test
    void getInstanceShouldThrowExceptionWhenDiscoveryFails() {
      assertThatThrownBy(() -> instanceResolver.getInstance(J.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
        .hasMessage("[hs.ddif.core.config.standard.DefaultInstanceResolverTest$J] instantiation failed because auto discovery was unable to resolve all dependencies; found: []")
        .hasNoSuppressedExceptions()
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(DiscoveryFailure.class)
        .hasMessage("[hs.ddif.core.config.standard.DefaultInstanceResolverTest$J] instantiation failed because auto discovery was unable to resolve all dependencies; found: []")
        .hasNoSuppressedExceptions()
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(DefinitionException.class)
        .hasMessage("Exception occurred during discovery via path: [hs.ddif.core.config.standard.DefaultInstanceResolverTest$J]")
        .satisfies(throwable -> {
          assertThat(throwable.getSuppressed()).hasSize(1);
          assertThat(throwable.getSuppressed()[0])
            .isExactlyInstanceOf(DefinitionException.class)
            .hasMessage("[class hs.ddif.core.config.standard.DefaultInstanceResolverTest$J] cannot be abstract")
            .hasNoCause();
        })
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
}
