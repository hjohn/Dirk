package hs.ddif.core.inject.instantiator;

import hs.ddif.annotations.Produces;
import hs.ddif.annotations.WeakSingleton;
import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.inject.store.AutoDiscoveringGatherer;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.inject.store.InstanceInjectableFactory;
import hs.ddif.core.inject.store.MethodInjectableFactory;
import hs.ddif.core.scope.AbstractScopeResolver;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.scope.SingletonScopeResolver;
import hs.ddif.core.scope.WeakSingletonScopeResolver;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.AnnotationDescriptor;

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

public class InstantiatorTest {
  private final InjectableStore<ResolvableInjectable> store = new InjectableStore<>();
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

  private final ScopeResolver[] scopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(), new WeakSingletonScopeResolver(), scopeResolver};

  private final ClassInjectableFactory classInjectableFactory = new ClassInjectableFactory(ResolvableInjectable::new);
  private final MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(ResolvableInjectable::new);
  private final InstanceInjectableFactory instanceInjectableFactory = new InstanceInjectableFactory(ResolvableInjectable::new);

  private String currentScope;

  @Nested
  class WhenStoreIsEmpty {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(store, false, List.of(), classInjectableFactory);
    private final Instantiator instantiator = new Instantiator(store, gatherer, false, scopeResolvers);

    @Test
    void shouldThrowExceptionWhenGettingSingleInstance() {
      assertThatThrownBy(() -> instantiator.getInstance(A.class))
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasNoCause();

      assertThatThrownBy(() -> instantiator.getParameterizedInstance(A.class, new NamedParameter[] {new NamedParameter("param", 5)}))
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasNoCause();
    }

    @Test
    void shouldReturnEmptySetWhenGettingMultipleInstances() throws InstanceCreationFailure {
      assertThat(instantiator.getInstances(A.class)).isEmpty();
    }
  }

  @Nested
  class WhenStoreNotEmpty {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(store, false, List.of(), classInjectableFactory);
    private final Instantiator instantiator = new Instantiator(store, gatherer, false, scopeResolvers);

    {
      try {
        store.put(classInjectableFactory.create(A.class));
        store.put(classInjectableFactory.create(B.class));
        store.put(classInjectableFactory.create(C.class));
        store.put(classInjectableFactory.create(D.class));
        store.put(classInjectableFactory.create(F.class));
        store.put(classInjectableFactory.create(G.class));
        store.put(instanceInjectableFactory.create("red", AnnotationDescriptor.describe(Red.class)));
        store.put(instanceInjectableFactory.create("green", AnnotationDescriptor.named("green")));
        store.put(methodInjectableFactory.create(B.class.getDeclaredMethod("createH"), B.class));
        store.put(methodInjectableFactory.create(B.class.getDeclaredMethod("createI"), B.class));
        store.put(classInjectableFactory.create(K.class));
      }
      catch(NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException();
      }
    }

    @Test
    void shouldReturnInstancesOfKnownTypes() throws InstanceCreationFailure, NoSuchInstance, MultipleInstances {
      assertNotNull(instantiator.getInstance(A.class));
      assertNotNull(instantiator.getInstance(String.class, Red.class));
      assertThat(instantiator.getInstances(String.class)).hasSize(2);
    }

    @Test
    void shouldFollowScopeRules() throws InstanceCreationFailure, NoSuchInstance, MultipleInstances {
      assertFalse(instantiator.getInstance(A.class).equals(instantiator.getInstance(A.class)));
      assertTrue(instantiator.getInstance(B.class).equals(instantiator.getInstance(B.class)));
      assertTrue(instantiator.getInstance(C.class).equals(instantiator.getInstance(C.class)));
    }

    @Test
    void weakSingletongsShouldBeGCd() throws InstanceCreationFailure, NoSuchInstance, MultipleInstances {
      int hash1 = instantiator.getInstance(C.class).hashCode();

      System.gc();

      int hash2 = instantiator.getInstance(C.class).hashCode();

      assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldThrowOutOfScopeExceptionWhenScopeNotActive() {
      assertThatThrownBy(() -> instantiator.getInstance(D.class))
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasMessage("No such instance: class hs.ddif.core.inject.instantiator.InstantiatorTest$D")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(OutOfScopeException.class)
        .hasMessage("Scope not active: interface hs.ddif.core.inject.instantiator.InstantiatorTest$TestScoped for key: Injectable[hs.ddif.core.inject.instantiator.InstantiatorTest$D]")
        .hasNoCause();
    }

    @Test
    void shouldThrowExceptionWhenNotSingular() {
      assertThatThrownBy(() -> instantiator.getInstance(String.class))
        .isExactlyInstanceOf(MultipleInstances.class)
        .hasNoCause();
    }

    @Test
    void getInstancesShouldRetrieveScopedInstancesOnlyWhenActive() throws InstanceCreationFailure {
      assertThat(instantiator.getInstances(E.class)).hasSize(1);

      currentScope = "Active!";

      assertThat(instantiator.getInstances(E.class)).hasSize(2);
    }

    @Test
    void getInstancesShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instantiator.getInstances(H.class))
        .isExactlyInstanceOf(InstanceCreationFailure.class)
        .hasMessage("Exception while constructing instance via Producer: hs.ddif.core.inject.instantiator.InstantiatorTest$H hs.ddif.core.inject.instantiator.InstantiatorTest$B.createH()")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(InvocationTargetException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(RuntimeException.class)
        .hasMessage("can't create H")
        .hasNoCause();
    }

    @Test
    void getInstancesShouldRetrieveSingletons() throws InstanceCreationFailure {
      assertThat(instantiator.getInstances(B.class))
        .hasSize(1)
        .containsExactlyInAnyOrderElementsOf(instantiator.getInstances(B.class));
    }

    @Test
    void getInstancesShouldIgnoreNullInstancesFromProducers() throws InstanceCreationFailure {
      assertThat(instantiator.getInstances(I.class)).isEmpty();
    }

    @Test
    void getInstanceShouldRejectNullInstancesFromProducers() {
      assertThatThrownBy(() -> instantiator.getInstance(I.class))
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasMessage("No such instance: class hs.ddif.core.inject.instantiator.InstantiatorTest$I")
        .hasNoCause();
    }

    @Test
    void getInstanceShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instantiator.getInstance(H.class))
        .isExactlyInstanceOf(InstanceCreationFailure.class)
        .hasMessage("Exception while constructing instance via Producer: hs.ddif.core.inject.instantiator.InstantiatorTest$H hs.ddif.core.inject.instantiator.InstantiatorTest$B.createH()")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(InvocationTargetException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(RuntimeException.class)
        .hasMessage("can't create H")
        .hasNoCause();
    }

    @Test
    void shouldAllowCreatingInstancesForUnknownScopes() throws InstanceCreationFailure, NoSuchInstance, MultipleInstances {
      assertNotNull(instantiator.getInstance(K.class));  // consistency policy can enforce valid scopes, but not instantiator
    }
  }

  @Nested
  class WhenStoreEmptyAndAutoDiscoveryIsActive {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(store, true, List.of(), classInjectableFactory);
    private final Instantiator instantiator = new Instantiator(store, gatherer, true, scopeResolvers);

    @Test
    void getInstancesShouldNeverDiscoverTypes() throws InstanceCreationFailure {
      assertThat(instantiator.getInstances(A.class)).isEmpty();
      assertThat(instantiator.getInstances(B.class)).isEmpty();
      assertThat(instantiator.getInstances(C.class)).isEmpty();
    }

    @Test
    void getInstanceShouldDiscoverNewTypes() throws InstanceCreationFailure, NoSuchInstance, MultipleInstances {
      assertNotNull(instantiator.getInstance(A.class));
    }

    @Test
    void getInstanceShouldNotDiscoverTypesWithQualifiers() {
      assertThatThrownBy(() -> instantiator.getInstance(A.class, Red.class))
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasMessage("No such instance: class hs.ddif.core.inject.instantiator.InstantiatorTest$A with criteria [interface hs.ddif.core.test.qualifiers.Red]")
        .hasNoCause();
    }

    @Test
    void getInstanceShouldThrowExceptionWhenDiscoveryFails() {
      assertThatThrownBy(() -> instantiator.getInstance(J.class))
        .isExactlyInstanceOf(DiscoveryFailure.class)
        .hasMessage("Exception during auto discovery: class hs.ddif.core.inject.instantiator.InstantiatorTest$J")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(BindingException.class)
        .hasMessage("Type cannot be abstract: class hs.ddif.core.inject.instantiator.InstantiatorTest$J")
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

  @UnknownScoped
  public static class K {
  }
}
