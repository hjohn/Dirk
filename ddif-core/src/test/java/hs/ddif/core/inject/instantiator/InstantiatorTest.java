package hs.ddif.core.inject.instantiator;

import hs.ddif.annotations.Produces;
import hs.ddif.annotations.WeakSingleton;
import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.store.AutoDiscoveringGatherer;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.inject.store.InstanceInjectable;
import hs.ddif.core.inject.store.MethodInjectable;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
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

  private String currentScope;

  @Nested
  class WhenStoreIsEmpty {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(store, false, List.of());
    private final Instantiator instantiator = new Instantiator(store, gatherer, false, scopeResolvers);

    @Test
    void shouldThrowExceptionWhenGettingSingleInstance() {
      assertThatThrownBy(() -> instantiator.getInstance(A.class))
        .isInstanceOf(BeanResolutionException.class);

      assertThatThrownBy(() -> instantiator.getParameterizedInstance(A.class, new NamedParameter[] {new NamedParameter("param", 5)}))
        .isInstanceOf(BeanResolutionException.class);
    }

    @Test
    void shouldReturnEmptySetWhenGettingMultipleInstances() throws BeanResolutionException {
      assertThat(instantiator.getInstances(A.class)).isEmpty();
    }
  }

  @Nested
  class WhenStoreNotEmpty {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(store, false, List.of());
    private final Instantiator instantiator = new Instantiator(store, gatherer, false, scopeResolvers);

    {
      try {
        store.put(new ClassInjectable(A.class));
        store.put(new ClassInjectable(B.class));
        store.put(new ClassInjectable(C.class));
        store.put(new ClassInjectable(D.class));
        store.put(new ClassInjectable(F.class));
        store.put(new ClassInjectable(G.class));
        store.put(new InstanceInjectable("red", AnnotationDescriptor.describe(Red.class)));
        store.put(new InstanceInjectable("green", AnnotationDescriptor.named("green")));
        store.put(new MethodInjectable(B.class.getDeclaredMethod("createH"), B.class));
        store.put(new MethodInjectable(B.class.getDeclaredMethod("createI"), B.class));
      }
      catch(NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException();
      }
    }

    @Test
    void shouldReturnInstancesOfKnownTypes() throws BeanResolutionException {
      assertNotNull(instantiator.getInstance(A.class));
      assertNotNull(instantiator.getInstance(String.class, Red.class));
      assertThat(instantiator.getInstances(String.class)).hasSize(2);
    }

    @Test
    void shouldFollowScopeRules() throws BeanResolutionException {
      assertNotEquals(instantiator.getInstance(A.class), instantiator.getInstance(A.class));
      assertEquals(instantiator.getInstance(B.class), instantiator.getInstance(B.class));
      assertEquals(instantiator.getInstance(C.class), instantiator.getInstance(C.class));
    }

    @Test
    void weakSingletongsShouldBeGCd() throws BeanResolutionException {
      int hash1 = instantiator.getInstance(C.class).hashCode();

      System.gc();

      int hash2 = instantiator.getInstance(C.class).hashCode();

      assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldThrowOutOfScopeExceptionWhenScopeNotActive() {
      assertThatThrownBy(() -> instantiator.getInstance(D.class))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.inject.instantiator.InstantiatorTest$D")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(OutOfScopeException.class)
        .hasMessage("Scope not active: interface hs.ddif.core.inject.instantiator.InstantiatorTest$TestScoped for type: class hs.ddif.core.inject.instantiator.InstantiatorTest$D");
    }

    @Test
    void shouldThrowExceptionWhenNotSingular() {
      assertThatThrownBy(() -> instantiator.getInstance(String.class))
        .isInstanceOf(BeanResolutionException.class);
    }

    @Test
    void getInstancesShouldRetrieveScopedInstancesOnlyWhenActive() throws BeanResolutionException {
      assertThat(instantiator.getInstances(E.class)).hasSize(1);

      currentScope = "Active!";

      assertThat(instantiator.getInstances(E.class)).hasSize(2);
    }

    @Test
    void getInstancesShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instantiator.getInstances(H.class))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.inject.instantiator.InstantiatorTest$H")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InstantiationException.class)
        .hasMessage("Exception while constructing instance via Producer: hs.ddif.core.inject.instantiator.InstantiatorTest$H hs.ddif.core.inject.instantiator.InstantiatorTest$B.createH()")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InvocationTargetException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(RuntimeException.class)
        .hasMessage("can't create H");
    }

    @Test
    void getInstancesShouldRetrieveSingletons() throws BeanResolutionException {
      assertThat(instantiator.getInstances(B.class))
        .hasSize(1)
        .containsExactlyInAnyOrderElementsOf(instantiator.getInstances(B.class));
    }

    @Test
    void getInstancesShouldIgnoreNullInstancesFromProducers() throws BeanResolutionException {
      assertThat(instantiator.getInstances(I.class)).isEmpty();
    }

    @Test
    void getInstanceShouldRejectNullInstancesFromProducers() {
      assertThatThrownBy(() -> instantiator.getInstance(I.class))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.inject.instantiator.InstantiatorTest$I");
    }

    @Test
    void getInstanceShouldThrowExceptionWhenInstantiationFails() {
      assertThatThrownBy(() -> instantiator.getInstance(H.class))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.inject.instantiator.InstantiatorTest$H")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InstantiationException.class)
        .hasMessage("Exception while constructing instance via Producer: hs.ddif.core.inject.instantiator.InstantiatorTest$H hs.ddif.core.inject.instantiator.InstantiatorTest$B.createH()")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InvocationTargetException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(RuntimeException.class)
        .hasMessage("can't create H");
    }
  }

  @Nested
  class WhenStoreEmptyAndAutoDiscoveryIsActive {
    private final AutoDiscoveringGatherer gatherer = new AutoDiscoveringGatherer(store, true, List.of());
    private final Instantiator instantiator = new Instantiator(store, gatherer, true, scopeResolvers);

    @Test
    void getInstancesShouldNeverDiscoverTypes() throws BeanResolutionException {
      assertThat(instantiator.getInstances(A.class)).isEmpty();
      assertThat(instantiator.getInstances(B.class)).isEmpty();
      assertThat(instantiator.getInstances(C.class)).isEmpty();
    }

    @Test
    void getInstanceShouldDiscoverNewTypes() throws BeanResolutionException {
      assertNotNull(instantiator.getInstance(A.class));
    }

    @Test
    void getInstanceShouldNotDiscoverTypesWithQualifiers() {
      assertThatThrownBy(() -> instantiator.getInstance(A.class, Red.class))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.inject.instantiator.InstantiatorTest$A with criteria [interface hs.ddif.core.test.qualifiers.Red]");
    }

    @Test
    void getInstanceShouldThrowExceptionWhenDiscoveryFails() {
      assertThatThrownBy(() -> instantiator.getInstance(J.class))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.inject.instantiator.InstantiatorTest$J")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(DiscoveryException.class)
        .hasMessage("Auto discovery failed for: class hs.ddif.core.inject.instantiator.InstantiatorTest$J")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(BindingException.class)
        .hasMessage("Type cannot be abstract: class hs.ddif.core.inject.instantiator.InstantiatorTest$J");
    }
  }

  @Scope
  @Retention(RUNTIME)
  public @interface TestScoped {
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
}
