package hs.ddif.jsr330;

import hs.ddif.core.Injector;
import hs.ddif.core.inject.store.ScopeConflictException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProviderTypeExtensionInjectorTest {
  private Injector injector = Injectors.manual();

  // Singleton -> Singleton = OK
  @Test
  public void shouldRegisterSingletonBeanDependentOnXWhenXIsProvidedAsSingleton() {
    injector.registerInstance(new X());
    injector.register(SingletonBeanDependentOnX.class);
  }

  // Prototype -> Prototype = OK
  @Test
  public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsPrototype() {
    injector.registerInstance(new XProvider());
    injector.register(PrototypeBeanDependantOnX.class);
  }

  // Prototype -> Singleton = OK
  @Test
  public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsSingleton() {
    injector.registerInstance(new X());
    injector.register(PrototypeBeanDependantOnX.class);
  }

  // Singleton -> Prototype = ERROR
  @Test
  public void shouldThrowExceptionWhenRegisteringSingletonBeanDependentOnXWhenXIsProvidedAsPrototype() {
    injector.registerInstance(new XProvider());

    assertThatThrownBy(() -> injector.register(SingletonBeanDependentOnX.class))
      .isExactlyInstanceOf(ScopeConflictException.class);
  }

  // Singleton -> Provider<Singleton> = OK
  @Test
  public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsSingleton() {
    injector.registerInstance(new X());
    injector.register(SingletonBeanDependentOnXProvider.class);
  }

  // Prototype -> Provider<Prototype> = OK
  @Test
  public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsPrototype() {
    injector.registerInstance(new XProvider());
    injector.register(PrototypeBeanDependantOnXProvider.class);
  }

  // Prototype -> Provider<Singleton> = OK
  @Test
  public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsSingleton() {
    injector.registerInstance(new X());
    injector.register(PrototypeBeanDependantOnXProvider.class);
  }

  // Singleton -> Provider<Prototype> = OK
  @Test
  public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsPrototype() {
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
