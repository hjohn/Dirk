package hs.ddif.core;

import hs.ddif.api.Injector;
import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.definition.ScopeConflictException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.core.config.DefaultInjectorStrategy;
import hs.ddif.core.config.SimpleScopeStrategy;
import hs.ddif.core.test.scope.Dependent;
import hs.ddif.core.test.scope.TestScope;
import hs.ddif.extensions.proxy.ByteBuddyProxyStrategy;
import hs.ddif.spi.scope.AbstractScopeResolver;
import hs.ddif.spi.scope.OutOfScopeException;
import hs.ddif.spi.scope.ScopeResolver;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.NoSuchElementException;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

public class ProxiedScopeTest {
  private String currentScope;
  private ScopeResolver scopeResolver = new AbstractScopeResolver<>() {
    @Override
    public Class<? extends Annotation> getAnnotationClass() {
      return TestScope.class;
    }

    @Override
    protected String getCurrentScope() {
      return currentScope;
    }
  };

  private Injector injector = InjectorBuilder.builder()
    .injectorStrategy(new DefaultInjectorStrategy(
      InjectableFactories.ANNOTATION_STRATEGY,
      new SimpleScopeStrategy(Scope.class, Singleton.class, Dependent.class),
      new ByteBuddyProxyStrategy()
    ))
    .scopeResolvers(context -> List.of(scopeResolver))
    .defaultLifeCycleCallbacksFactory()
    .autoDiscovering()
    .defaultDiscoveryExtensions()
    .build();

  @Test
  void getInstanceShouldNotReturnProxies() throws Exception {
    // No scope active, creation fails:
    assertThatThrownBy(() -> injector.getInstance(B.class))
      .isExactlyInstanceOf(CreationException.class);

    currentScope = "A";

    // A scope is active, creation succeeds:
    B b = injector.getInstance(B.class);

    assertThat(b).isNotNull();
    assertThat(b.getClass().getName()).endsWith("ProxiedScopeTest$B");
  }

  @Test
  void shouldCreateProxy() throws Exception {
    A a = injector.getInstance(A.class);

    assertThat(a).isNotNull();

    assertThatThrownBy(() -> a.b.hello())
      .isExactlyInstanceOf(OutOfScopeException.class);

    currentScope = "A";

    injector.getInstance(B.class).setHelloText("Hi");

    currentScope = "B";

    assertThat(a.b.hello()).isEqualTo("Hello");

    currentScope = "A";

    assertThat(a.b.hello()).isEqualTo("Hi");
  }

  @Test
  void providerShouldNeverReturnProxies() throws Exception {
    C c = injector.getInstance(C.class);

    assertThatThrownBy(() -> c.b.get())
      .isExactlyInstanceOf(CreationException.class);  // TODO perhaps OutOfScopeException be nicer here?

    currentScope = "A";

    B b = c.b.get();

    assertThat(b).isNotNull();

    // ensure that it is not a proxy:
    currentScope = null;

    assertThat(b.getClass().getName()).endsWith("ProxiedScopeTest$B");
    assertThat(b.hello()).isEqualTo("Hello");
  }

  @Test
  void shouldNotProxyNestedObjectWithSameScope() throws Exception {
    currentScope = "A";

    D d = injector.getInstance(D.class);

    assertThat(d.getClass().getName()).endsWith("ProxiedScopeTest$D");  // Not a proxy
    assertThat(d.b.getClass().getName()).endsWith("ProxiedScopeTest$B");  // Also not a proxy
  }

  @Test
  void shouldProxyNestedObjectWithinSingletonParent() throws Exception {
    currentScope = "A";

    E e = injector.getInstance(E.class);

    assertThat(e.getClass().getName()).endsWith("ProxiedScopeTest$E");  // Not a proxy
    assertThat(e.b.getClass().getName()).doesNotEndWith("ProxiedScopeTest$B");  // A proxy
  }

  @Test
  void shouldProxyNestedObjectWithinUnscopedParent() throws Exception {
    currentScope = "A";

    F f = injector.getInstance(F.class);

    assertThat(f.getClass().getName()).endsWith("ProxiedScopeTest$F");  // Not a proxy
    assertThat(f.b.getClass().getName()).doesNotEndWith("ProxiedScopeTest$B");  // A proxy
  }

  @Test
  void shouldRejectProxyingFinalClass() {
    assertThatThrownBy(() -> injector.getInstance(G.class))
      .isExactlyInstanceOf(AutoDiscoveryException.class)
      .hasMessage("[hs.ddif.core.ProxiedScopeTest$G] and the discovered types [Class [hs.ddif.core.ProxiedScopeTest$G], Class [hs.ddif.core.ProxiedScopeTest$H]] could not be registered")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(ScopeConflictException.class)
      .hasMessage("Type [class hs.ddif.core.ProxiedScopeTest$G] with scope [interface hs.ddif.core.test.scope.Dependent] is dependent on [class hs.ddif.core.ProxiedScopeTest$H] with normal scope [interface hs.ddif.core.test.scope.TestScope]; this requires the use of a provider or proxy")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("Could not create type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot subclass primitive, array or final types: class hs.ddif.core.ProxiedScopeTest$H")
      .hasNoCause();
  }

  @Test
  void exceptionsThrownByProxiedObjectShouldWorkNormally() {
    A a = injector.getInstance(A.class);

    currentScope = "A";

    assertThatThrownBy(() -> a.b.exception())
      .isExactlyInstanceOf(NoSuchElementException.class);

    assertThatThrownBy(() -> a.b.checkedException())
      .isExactlyInstanceOf(IOException.class);
  }

  @Singleton
  public static class A {
    @Inject public B b;
  }

  @TestScope
  public static class B {
    private String text = "Hello";

    public void setHelloText(String text) {
      this.text = text;
    }

    protected String hello() { // package private won't work easily...
      return text;
    }

    public String exception() {
      throw new NoSuchElementException("5");
    }

    public String checkedException() throws IOException {
      throw new IOException();
    }
  }

  @Singleton
  public static class C {
    @Inject public Provider<B> b;
  }

  @TestScope
  public static class D {
    @Inject public B b;  // no need to proxy this
  }

  @Singleton
  public static class E {
    @Inject public B b;  // must proxy this
  }

  public static class F {
    @Inject public B b;  // must proxy this
  }

  public static class G {
    @Inject public H h;  // must proxy this, but can't as H is final
  }

  @TestScope
  public static final class H {
  }
}
