package org.int4.dirk.core;

import java.lang.annotation.Annotation;

import org.int4.dirk.annotations.Produces;
import org.int4.dirk.api.Injector;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.api.definition.ScopeConflictException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.test.scope.TestScope;
import org.int4.dirk.spi.scope.AbstractScopeResolver;
import org.int4.dirk.util.Annotations;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public class InjectorScopeTest {

  @Nested
  class Classes {

    @Test
    public void shouldThrowScopeNotActiveExceptionWhenNoScopeActive() throws Exception {
      TestScopeResolver scopeResolver = new TestScopeResolver();

      scopeResolver.currentScope = null;

      Injector injector = Injectors.manual(scopeResolver);

      injector.register(TestScopedBean.class);

      assertThatThrownBy(() -> injector.getInstance(TestScopedBean.class))
        .isExactlyInstanceOf(ScopeNotActiveException.class)
        .hasMessage("Scope not active: @org.int4.dirk.core.test.scope.TestScope() for: Class [org.int4.dirk.core.InjectorScopeTest$TestScopedBean]")
        .hasNoCause();
    }

    @Test
    public void shouldRemoveInstancesFromScopeResolver() throws Exception {
      Injector injector = Injectors.manual();

      injector.register(S.class);

      S instance1 = injector.getInstance(S.class);
      S instance2 = injector.getInstance(S.class);

      assertThat(instance1).isEqualTo(instance2);

      injector.remove(S.class);

      assertThatThrownBy(() -> injector.getInstance(S.class)).isExactlyInstanceOf(UnsatisfiedResolutionException.class);

      injector.register(S.class);

      S instance3 = injector.getInstance(S.class);
      S instance4 = injector.getInstance(S.class);

      assertThat(instance3).isEqualTo(instance4);
      assertThat(instance3).isNotEqualTo(instance1);  // proves that it was removed from the ScopeResolver
    }

    @Test
    public void shouldKeepScopesSeparated() throws Exception {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = Injectors.manual(scopeResolver);

      injector.register(TestScopedBean.class);
      injector.register(SomeUserBeanWithTestScope.class);

      SomeUserBeanWithTestScope testScopedBean1 = injector.getInstance(SomeUserBeanWithTestScope.class);
      SomeUserBeanWithTestScope testScopedBean2 = injector.getInstance(SomeUserBeanWithTestScope.class);

      scopeResolver.currentScope = "a";

      SomeUserBeanWithTestScope testScopedBean3 = injector.getInstance(SomeUserBeanWithTestScope.class);

      scopeResolver.currentScope = "b";

      SomeUserBeanWithTestScope testScopedBean4 = injector.getInstance(SomeUserBeanWithTestScope.class);

      scopeResolver.currentScope = "a";

      SomeUserBeanWithTestScope testScopedBean5 = injector.getInstance(SomeUserBeanWithTestScope.class);

      assertEquals(testScopedBean1, testScopedBean2);
      assertEquals(testScopedBean3, testScopedBean5);
      assertNotEquals(testScopedBean1, testScopedBean3);
      assertNotEquals(testScopedBean1, testScopedBean4);
      assertNotEquals(testScopedBean1, testScopedBean5);
      assertNotEquals(testScopedBean3, testScopedBean4);
    }

    @Test
    public void shouldKeepScopesSeparatedInReferences() throws Exception {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = Injectors.manual(scopeResolver);

      injector.register(TestScopedBean.class);
      injector.register(SomeUserBean.class);

      TestScopedBean testScopedBean1 = injector.getInstance(SomeUserBean.class).testScopedBean.get();
      TestScopedBean testScopedBean2 = injector.getInstance(SomeUserBean.class).testScopedBean.get();

      scopeResolver.currentScope = "a";

      TestScopedBean testScopedBean3 = injector.getInstance(SomeUserBean.class).testScopedBean.get();

      scopeResolver.currentScope = "b";

      TestScopedBean testScopedBean4 = injector.getInstance(SomeUserBean.class).testScopedBean.get();

      scopeResolver.currentScope = "a";

      TestScopedBean testScopedBean5 = injector.getInstance(SomeUserBean.class).testScopedBean.get();

      assertEquals(testScopedBean1, testScopedBean2);
      assertEquals(testScopedBean3, testScopedBean5);
      assertNotEquals(testScopedBean1, testScopedBean3);
      assertNotEquals(testScopedBean1, testScopedBean4);
      assertNotEquals(testScopedBean1, testScopedBean5);
      assertNotEquals(testScopedBean3, testScopedBean4);
    }

    @Test
    public void shouldThrowExceptionWhenNarrowScopedBeansAreInjectedIntoBroaderScopedBeans() throws Exception {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = Injectors.manual(scopeResolver);

      injector.register(TestScopedBean.class);

      assertThatThrownBy(() -> injector.register(IllegalSingletonBean.class))
        .isExactlyInstanceOf(ScopeConflictException.class);
    }

    @Test
    public void shouldAllowInjectingUnscopedInstancesAlways() throws Exception {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = Injectors.manual(scopeResolver);

      injector.register(UnscopedBean.class);
      injector.register(SomeUserBeanWithTestScope2.class);

      assertNotNull(injector.getInstance(SomeUserBeanWithTestScope2.class));
    }

    @Test
    public void shouldThrowExceptionWhenMultipleScopesDefinedOnBean() throws Exception {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = Injectors.manual(scopeResolver);

      injector.register(TestScopedBean.class);

      assertThatThrownBy(() -> injector.register(IllegalMultiScopedBean.class))
        .isExactlyInstanceOf(DefinitionException.class)
        .hasMessage("[class org.int4.dirk.core.InjectorScopeTest$IllegalMultiScopedBean] cannot have multiple scope annotations, but found: [@jakarta.inject.Singleton(), @org.int4.dirk.core.test.scope.TestScope()]")
        .hasNoCause();
    }
  }

  @Nested
  class Providers {

    /*
     * Tests related to Providers and Scopes:
     */

    // Singleton -> Singleton = OK
    @Test
    public void shouldRegisterSingletonBeanDependentOnXWhenXIsProvidedAsSingleton() throws Exception {
      Injector injector = Injectors.manual();

      injector.registerInstance(new X());
      injector.register(SingletonBeanDependentOnX.class);
    }

    // Prototype -> Prototype = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsPrototype() throws Exception {
      Injector injector = Injectors.manual();

      injector.registerInstance(new XProvider());
      injector.register(PrototypeBeanDependantOnX.class);
    }

    // Prototype -> Singleton = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsSingleton() throws Exception {
      Injector injector = Injectors.manual();

      injector.registerInstance(new X());
      injector.register(PrototypeBeanDependantOnX.class);
    }

    // Singleton -> Prototype = OK
    @Test
    public void shouldRegisterSingletonBeanDependentOnXWhenXIsProvidedAsPrototype() throws Exception {
      Injector injector = Injectors.manual();

      injector.registerInstance(new XProvider());
      injector.register(SingletonBeanDependentOnX.class);
    }

    // Singleton -> Provider<Singleton> = OK
    @Test
    public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsSingleton() throws Exception {
      Injector injector = Injectors.manual();

      injector.registerInstance(new X());
      injector.register(SingletonBeanDependentOnXProvider.class);
    }

    // Prototype -> Provider<Prototype> = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsPrototype() throws Exception {
      Injector injector = Injectors.manual();

      injector.registerInstance(new XProvider());
      injector.register(PrototypeBeanDependantOnXProvider.class);
    }

    // Prototype -> Provider<Singleton> = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsSingleton() throws Exception {
      Injector injector = Injectors.manual();

      injector.registerInstance(new X());
      injector.register(PrototypeBeanDependantOnXProvider.class);
    }

    // Singleton -> Provider<Prototype> = OK
    @Test
    public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsPrototype() throws Exception {
      Injector injector = Injectors.manual();

      injector.registerInstance(new XProvider());
      injector.register(SingletonBeanDependentOnXProvider.class);
    }
  }

  @Singleton
  public static class S {
  }

  public static class X {
  }

  public static class PrototypeBeanDependantOnX {
    @Inject X x;
  }

  @Singleton
  public static class SingletonBeanDependentOnX {
    @Inject X x;
  }

  public static class PrototypeBeanDependantOnXProvider {
    @Inject Provider<X> xProvider;
  }

  @Singleton
  public static class SingletonBeanDependentOnXProvider {
    @Inject Provider<X> xProvider;
  }

  public static class XProvider implements Provider<X> {  // A provider by default is assumed to provide a new bean every time (the most narrow scope, or prototype scope)
    @Override
    public X get() {
      return null;
    }
  }

  @Test
  public void shouldKeepSameTypesWithDifferentQualifiersSeparated() throws Exception {
    TestScopeResolver scopeResolver = new TestScopeResolver();
    Injector injector = Injectors.manual(scopeResolver);

    injector.register(Producers.class);
    assertThat(injector.getInstances(String.class))
      .containsExactlyInAnyOrder("a", "b");
  }

  public static class Producers {
    @Produces @TestScope @Named("a") static String a = "a";
    @Produces @TestScope @Named("b") static String b = "b";
  }

  static class TestScopeResolver extends AbstractScopeResolver<String> {
    public String currentScope = "default";

    @Override
    public Annotation getAnnotation() {
      return Annotations.of(TestScope.class);
    }

    @Override
    public String getCurrentScope() {
      return currentScope;
    }
  }

  @TestScope
  public static class TestScopedBean {
  }

  public static class UnscopedBean {
  }

  public static class SomeUserBean {
    @Inject Provider<TestScopedBean> testScopedBean;
  }

  @TestScope
  public static class SomeUserBeanWithTestScope {
    @Inject TestScopedBean testScopedBean;
  }

  @TestScope
  public static class SomeUserBeanWithTestScope2 {
    @Inject UnscopedBean unscopedBean;
  }

  @Singleton
  public static class IllegalSingletonBean {
    @Inject TestScopedBean testScopedBean;
  }

  @Singleton
  @TestScope
  public static class IllegalMultiScopedBean {
    @Inject TestScopedBean testScopedBean;
  }
}
