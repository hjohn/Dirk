package hs.ddif.core.config.standard;

import hs.ddif.annotations.Produces;
import hs.ddif.annotations.WeakSingleton;
import hs.ddif.core.api.InstanceCreationException;
import hs.ddif.core.api.InstanceResolver;
import hs.ddif.core.api.MultipleInstancesException;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.config.consistency.InjectorStoreConsistencyPolicy;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.injectable.ClassInjectableFactory;
import hs.ddif.core.inject.injectable.DefinitionException;
import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.inject.injectable.InjectableFactories;
import hs.ddif.core.inject.injectable.InstanceInjectableFactory;
import hs.ddif.core.inject.injectable.MethodInjectableFactory;
import hs.ddif.core.instantiation.DefaultInstantiationContext;
import hs.ddif.core.instantiation.InstanceFactories;
import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.InstantiatorBindingMap;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.ScopeResolverManager;
import hs.ddif.core.instantiation.ScopeResolverManagers;
import hs.ddif.core.scope.AbstractScopeResolver;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.inject.Scope;
import javax.inject.Singleton;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

  private final InstantiatorFactory instantiatorFactory = InstanceFactories.create();
  private final InstantiatorBindingMap instantiatorBindingMap = new InstantiatorBindingMap(instantiatorFactory);
  private final ScopeResolverManager scopeResolverManager = ScopeResolverManagers.create(scopeResolver);
  private final InjectorStoreConsistencyPolicy<Injectable> policy = new InjectorStoreConsistencyPolicy<>(instantiatorBindingMap, scopeResolverManager);
  private final QualifiedTypeStore<Injectable> store = new QualifiedTypeStore<>(policy);
  private final InstantiationContext instantiationContext = new DefaultInstantiationContext(store, instantiatorBindingMap, scopeResolverManager);
  private final BindingProvider bindingProvider = new BindingProvider();
  private final ClassInjectableFactory classInjectableFactory = InjectableFactories.forClass();
  private final MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, DefaultInjectable::new);
  private final InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(DefaultInjectable::new);

  private String currentScope;

  @Nested
  class WhenStoreIsEmpty {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(false, List.of(), classInjectableFactory);
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, gatherer, instantiationContext, instantiatorFactory);

    @Test
    void shouldThrowExceptionWhenGettingSingleInstance() {
      assertThatThrownBy(() -> instanceResolver.getInstance(A.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasNoCause();
    }

    @Test
    void shouldReturnEmptySetWhenGettingMultipleInstances() {
      assertThat(instanceResolver.getInstances(A.class)).isEmpty();
    }
  }

  @Nested
  class WhenStoreNotEmpty {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(false, List.of(), classInjectableFactory);
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, gatherer, instantiationContext, instantiatorFactory);

    {
      try {
        store.put(classInjectableFactory.create(A.class));
        store.put(classInjectableFactory.create(B.class));
        store.put(classInjectableFactory.create(C.class));
        store.put(classInjectableFactory.create(D.class));
        store.put(classInjectableFactory.create(F.class));
        store.put(classInjectableFactory.create(G.class));
        store.put(instanceInjectableFactory.create("red", Annotations.of(Red.class)));
        store.put(instanceInjectableFactory.create("green", Annotations.named("green")));
        store.put(methodInjectableFactory.create(B.class.getDeclaredMethod("createH"), B.class));
        store.put(methodInjectableFactory.create(B.class.getDeclaredMethod("createI"), B.class));
        store.put(classInjectableFactory.create(K.class));
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
    void weakSingletongsShouldBeGCd() {
      int hash1 = instanceResolver.getInstance(C.class).hashCode();

      System.gc();

      int hash2 = instanceResolver.getInstance(C.class).hashCode();

      assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldThrowOutOfScopeExceptionWhenScopeNotActive() {
      assertThatThrownBy(() -> instanceResolver.getInstance(D.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
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
        .hasMessage("No such instance: [class hs.ddif.core.config.standard.DefaultInstanceResolverTest$I]")
        .hasNoCause();
    }

    @Test
    void getInstanceShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instanceResolver.getInstance(H.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
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
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(true, List.of(), classInjectableFactory);
    private final InstanceResolver instanceResolver = new DefaultInstanceResolver(store, gatherer, instantiationContext, instantiatorFactory);

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
        .hasMessage("Path [@hs.ddif.core.test.qualifiers.Red() class hs.ddif.core.config.standard.DefaultInstanceResolverTest$A]: [class hs.ddif.core.config.standard.DefaultInstanceResolverTest$A] found during auto discovery is missing qualifiers required by: [@hs.ddif.core.test.qualifiers.Red() class hs.ddif.core.config.standard.DefaultInstanceResolverTest$A]")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(DefinitionException.class)
        .hasMessage("[class hs.ddif.core.config.standard.DefaultInstanceResolverTest$A] found during auto discovery is missing qualifiers required by: [@hs.ddif.core.test.qualifiers.Red() class hs.ddif.core.config.standard.DefaultInstanceResolverTest$A]")
        .hasNoCause();
    }

    @Test
    void getInstanceShouldThrowExceptionWhenDiscoveryFails() {
      assertThatThrownBy(() -> instanceResolver.getInstance(J.class))
        .isExactlyInstanceOf(InstanceCreationException.class)
        .hasMessage("Path [class hs.ddif.core.config.standard.DefaultInstanceResolverTest$J]: [class hs.ddif.core.config.standard.DefaultInstanceResolverTest$J] cannot be injected; failures:\n"
          + " - Type cannot be abstract: class hs.ddif.core.config.standard.DefaultInstanceResolverTest$J"
        )
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(DefinitionException.class)
        .hasMessage("[class hs.ddif.core.config.standard.DefaultInstanceResolverTest$J] cannot be injected; failures:\n"
          + " - Type cannot be abstract: class hs.ddif.core.config.standard.DefaultInstanceResolverTest$J"
        )
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

  @WeakSingleton
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
