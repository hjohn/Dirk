package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.inject.consistency.ScopeConflictException;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.scope.AbstractScopeResolver;
import hs.ddif.core.scope.OutOfScopeException;

import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InjectorScopeTest {

  public static class Classes {

    @Test
    public void shouldThrowOutOfScopeExceptionWhenNoScopeActive() {
      TestScopeResolver scopeResolver = new TestScopeResolver();

      scopeResolver.currentScope = null;

      Injector injector = Injectors.manual(scopeResolver);

      injector.register(TestScopedBean.class);

      NoSuchInstanceException e = assertThrows(NoSuchInstanceException.class, () -> injector.getInstance(TestScopedBean.class));

      assertEquals(OutOfScopeException.class, e.getCause().getClass());
    }

    @Test
    public void shouldKeepScopesSeparated() {
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
    public void shouldKeepScopesSeparatedInReferences() {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = Injectors.manual(scopeResolver);

      injector.register(TestScopedBean.class);
      injector.register(SomeUserBean.class);

      TestScopedBean testScopedBean1 = injector.getInstance(SomeUserBean.class).testScopedBean;
      TestScopedBean testScopedBean2 = injector.getInstance(SomeUserBean.class).testScopedBean;

      scopeResolver.currentScope = "a";

      TestScopedBean testScopedBean3 = injector.getInstance(SomeUserBean.class).testScopedBean;

      scopeResolver.currentScope = "b";

      TestScopedBean testScopedBean4 = injector.getInstance(SomeUserBean.class).testScopedBean;

      scopeResolver.currentScope = "a";

      TestScopedBean testScopedBean5 = injector.getInstance(SomeUserBean.class).testScopedBean;

      assertEquals(testScopedBean1, testScopedBean2);
      assertEquals(testScopedBean3, testScopedBean5);
      assertNotEquals(testScopedBean1, testScopedBean3);
      assertNotEquals(testScopedBean1, testScopedBean4);
      assertNotEquals(testScopedBean1, testScopedBean5);
      assertNotEquals(testScopedBean3, testScopedBean4);
    }

    @Test(expected = ScopeConflictException.class)
    public void shouldThrowExceptionWhenNarrowScopedBeansAreInjectedIntoBroaderScopedBeans() {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = Injectors.manual(scopeResolver);

      injector.register(TestScopedBean.class);
      injector.register(IllegalSingletonBean.class);
    }

    @Test(expected = ScopeConflictException.class)
    public void shouldThrowExceptionWhenNarrowScopedBeansAreInjectedIntoBroaderScopedBeans2() {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = Injectors.manual(scopeResolver);

      injector.register(UnscopedBean.class);
      injector.register(SomeUserBeanWithTestScope2.class);
    }

    @Test(expected = BindingException.class)
    public void shouldThrowExceptionWhenMultipleScopesDefinedOnBean() {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = Injectors.manual(scopeResolver);

      injector.register(TestScopedBean.class);
      injector.register(IllegalMultiScopedBean.class);
    }
  }

  public static class Providers {

    /*
     * Tests related to Providers and Scopes:
     */

    // Singleton -> Singleton = OK
    @Test
    public void shouldRegisterSingletonBeanDependentOnXWhenXIsProvidedAsSingleton() {
      Injector injector = Injectors.manual();

      injector.registerInstance(new X());
      injector.register(SingletonBeanDependentOnX.class);
    }

    // Prototype -> Prototype = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsPrototype() {
      Injector injector = Injectors.manual();

      injector.registerInstance(new XProvider());
      injector.register(PrototypeBeanDependantOnX.class);
    }

    // Prototype -> Singleton = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsSingleton() {
      Injector injector = Injectors.manual();

      injector.registerInstance(new X());
      injector.register(PrototypeBeanDependantOnX.class);
    }

    // Singleton -> Prototype = ERROR
    @Test(expected = ScopeConflictException.class)
    public void shouldThrowExceptionWhenRegisteringSingletonBeanDependentOnXWhenXIsProvidedAsPrototype() {
      Injector injector = Injectors.manual();

      injector.registerInstance(new XProvider());
      injector.register(SingletonBeanDependentOnX.class);
    }

    // Singleton -> Provider<Singleton> = OK
    @Test
    public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsSingleton() {
      Injector injector = Injectors.manual();

      injector.registerInstance(new X());
      injector.register(SingletonBeanDependentOnXProvider.class);
    }

    // Prototype -> Provider<Prototype> = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsPrototype() {
      Injector injector = Injectors.manual();

      injector.registerInstance(new XProvider());
      injector.register(PrototypeBeanDependantOnXProvider.class);
    }

    // Prototype -> Provider<Singleton> = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsSingleton() {
      Injector injector = Injectors.manual();

      injector.registerInstance(new X());
      injector.register(PrototypeBeanDependantOnXProvider.class);
    }

    // Singleton -> Provider<Prototype> = OK
    @Test
    public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsPrototype() {
      Injector injector = Injectors.manual();

      injector.registerInstance(new XProvider());
      injector.register(SingletonBeanDependentOnXProvider.class);
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

    public static class XProvider implements Provider<X> {  // A provider by default is assumed to provide a new bean everytime (the most narrow scope, or prototype scope)
      @Override
      public X get() {
        return null;
      }
    }
  }

  @Test
  public void shouldKeepSameTypesWithDifferentQualifiersSeparated() {
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
    public Class<? extends Annotation> getScopeAnnotationClass() {
      return TestScope.class;
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
    @Inject TestScopedBean testScopedBean;
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
