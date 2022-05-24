package org.int4.dirk.core;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.NoSuchElementException;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.int4.dirk.api.Injector;
import org.int4.dirk.api.definition.ScopeConflictException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.test.scope.Dependent;
import org.int4.dirk.core.test.scope.TestScope;
import org.int4.dirk.extensions.proxy.ByteBuddyProxyStrategy;
import org.int4.dirk.library.AnnotationBasedLifeCycleCallbacksFactory;
import org.int4.dirk.library.DefaultInjectorStrategy;
import org.int4.dirk.library.SimpleScopeStrategy;
import org.int4.dirk.spi.scope.AbstractScopeResolver;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
      new ByteBuddyProxyStrategy(),
      new AnnotationBasedLifeCycleCallbacksFactory(PostConstruct.class, PreDestroy.class)
    ))
    .scopeResolvers(context -> List.of(scopeResolver))
    .autoDiscovering()
    .defaultDiscoveryExtensions()
    .build();

  @Test
  void getInstanceShouldNotReturnProxies() throws Exception {
    injector.register(B.class);

    // No scope active, creation fails:
    assertThatThrownBy(() -> injector.getInstance(B.class))
      .isExactlyInstanceOf(ScopeNotActiveException.class);

    currentScope = "A";

    // A scope is active, creation succeeds:
    B b = injector.getInstance(B.class);

    assertThat(b).isNotNull();
    assertThat(b.getClass().getName()).endsWith("ProxiedScopeTest$B");
  }

  @Test
  void shouldCreateProxy() throws Exception {
    injector.register(A.class);

    A a = injector.getInstance(A.class);

    assertThat(a).isNotNull();

    assertThatThrownBy(() -> a.b.hello())
      .isExactlyInstanceOf(ScopeNotActiveException.class);

    currentScope = "A";

    injector.getInstance(B.class).setHelloText("Hi");

    currentScope = "B";

    assertThat(a.b.hello()).isEqualTo("Hello");

    currentScope = "A";

    assertThat(a.b.hello()).isEqualTo("Hi");
  }

  @Test
  void providerShouldNeverReturnProxies() throws Exception {
    injector.register(C.class);

    C c = injector.getInstance(C.class);

    assertThatThrownBy(() -> c.b.get())
      .isExactlyInstanceOf(ScopeNotActiveException.class);

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

    injector.register(D.class);

    D d = injector.getInstance(D.class);

    assertThat(d.getClass().getName()).endsWith("ProxiedScopeTest$D");  // Not a proxy
    assertThat(d.b.getClass().getName()).endsWith("ProxiedScopeTest$B");  // Also not a proxy
  }

  @Test
  void shouldProxyNestedObjectWithinSingletonParent() throws Exception {
    currentScope = "A";

    injector.register(E.class);

    E e = injector.getInstance(E.class);

    assertThat(e.getClass().getName()).endsWith("ProxiedScopeTest$E");  // Not a proxy
    assertThat(e.b.getClass().getName()).doesNotEndWith("ProxiedScopeTest$B");  // A proxy
  }

  @Test
  void shouldProxyNestedObjectWithinUnscopedParent() throws Exception {
    currentScope = "A";

    injector.register(F.class);

    F f = injector.getInstance(F.class);

    assertThat(f.getClass().getName()).endsWith("ProxiedScopeTest$F");  // Not a proxy
    assertThat(f.b.getClass().getName()).doesNotEndWith("ProxiedScopeTest$B");  // A proxy
  }

  @Test
  void shouldRejectProxyingFinalClass() {
    assertThatThrownBy(() -> injector.register(G.class))
      .isExactlyInstanceOf(ScopeConflictException.class)
      .hasMessage("Type [class org.int4.dirk.core.ProxiedScopeTest$G] with scope [interface org.int4.dirk.core.test.scope.Dependent] is dependent on [class org.int4.dirk.core.ProxiedScopeTest$H] with normal scope [interface org.int4.dirk.core.test.scope.TestScope]; this requires the use of a provider or proxy")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("Could not create type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot subclass primitive, array or final types: class org.int4.dirk.core.ProxiedScopeTest$H")
      .hasNoCause();
  }

  @Test
  void exceptionsThrownByProxiedObjectShouldWorkNormally() throws Exception {
    injector.register(A.class);

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
