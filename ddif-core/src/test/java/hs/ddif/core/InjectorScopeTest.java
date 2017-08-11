package hs.ddif.core;

import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class InjectorScopeTest {

  public static class Classes {

    @Test(expected = OutOfScopeException.class)
    public void shouldThrowOutOfScopeExceptionWhenNoScopeActive() {
      TestScopeResolver scopeResolver = new TestScopeResolver();

      scopeResolver.currentScope = null;

      Injector injector = new Injector(scopeResolver);

      injector.register(TestScopedBean.class);

      injector.getInstance(TestScopedBean.class);
    }

    @Test
    public void shouldKeepScopesSeparated() {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = new Injector(scopeResolver);

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
      Injector injector = new Injector(scopeResolver);

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
      Injector injector = new Injector(scopeResolver);

      injector.register(TestScopedBean.class);
      injector.register(IllegalSingletonBean.class);
    }

    @Test(expected = ScopeConflictException.class)
    public void shouldThrowExceptionWhenNarrowScopedBeansAreInjectedIntoBroaderScopedBeans2() {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = new Injector(scopeResolver);

      injector.register(UnscopedBean.class);
      injector.register(SomeUserBeanWithTestScope2.class);
    }

    @Test(expected = MultipleScopesException.class)
    public void shouldThrowExceptionWhenMultipleScopesDefinedOnBean() {
      TestScopeResolver scopeResolver = new TestScopeResolver();
      Injector injector = new Injector(scopeResolver);

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
      Injector injector = new Injector();

      injector.registerInstance(new X());
      injector.register(SingletonBeanDependentOnX.class);
    }

    // Prototype -> Prototype = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsPrototype() {
      Injector injector = new Injector();

      injector.register(new XProvider());
      injector.register(PrototypeBeanDependantOnX.class);
    }

    // Prototype -> Singleton = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsSingleton() {
      Injector injector = new Injector();

      injector.registerInstance(new X());
      injector.register(PrototypeBeanDependantOnX.class);
    }

    // Singleton -> Prototype = ERROR
    @Test(expected = ScopeConflictException.class)
    public void shouldThrowExceptionWhenRegisteringSingletonBeanDependentOnXWhenXIsProvidedAsPrototype() {
      Injector injector = new Injector();

      injector.register(new XProvider());
      injector.register(SingletonBeanDependentOnX.class);
    }

    // Singleton -> Provider<Singleton> = OK
    @Test
    public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsSingleton() {
      Injector injector = new Injector();

      injector.registerInstance(new X());
      injector.register(SingletonBeanDependentOnXProvider.class);
    }

    // Prototype -> Provider<Prototype> = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsPrototype() {
      Injector injector = new Injector();

      injector.register(new XProvider());
      injector.register(PrototypeBeanDependantOnXProvider.class);
    }

    // Prototype -> Provider<Singleton> = OK
    @Test
    public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsSingleton() {
      Injector injector = new Injector();

      injector.registerInstance(new X());
      injector.register(PrototypeBeanDependantOnXProvider.class);
    }

    // Singleton -> Provider<Prototype> = OK
    @Test
    public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsPrototype() {
      Injector injector = new Injector();

      injector.register(new XProvider());
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
