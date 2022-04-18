package hs.ddif.jakarta;

import hs.ddif.api.Injector;

import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public class ProviderTypeExtensionInjectorTest {
  private Injector injector = Injectors.manual();

  // Singleton -> Singleton = OK
  @Test
  public void shouldRegisterSingletonBeanDependentOnXWhenXIsProvidedAsSingleton() throws Exception {
    injector.registerInstance(new X());
    injector.register(SingletonBeanDependentOnX.class);
  }

  // Prototype -> Prototype = OK
  @Test
  public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsPrototype() throws Exception {
    injector.registerInstance(new XProvider());
    injector.register(PrototypeBeanDependantOnX.class);
  }

  // Prototype -> Singleton = OK
  @Test
  public void shouldRegisterPrototypeBeanDependentOnXWhenXIsProvidedAsSingleton() throws Exception {
    injector.registerInstance(new X());
    injector.register(PrototypeBeanDependantOnX.class);
  }

  // Singleton -> Prototype = OK
  @Test
  public void shouldThrowExceptionWhenRegisteringSingletonBeanDependentOnXWhenXIsProvidedAsPrototype() throws Exception {
    injector.registerInstance(new XProvider());
    injector.register(SingletonBeanDependentOnX.class);
  }

  // Singleton -> Provider<Singleton> = OK
  @Test
  public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsSingleton() throws Exception {
    injector.registerInstance(new X());
    injector.register(SingletonBeanDependentOnXProvider.class);
  }

  // Prototype -> Provider<Prototype> = OK
  @Test
  public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsPrototype() throws Exception {
    injector.registerInstance(new XProvider());
    injector.register(PrototypeBeanDependantOnXProvider.class);
  }

  // Prototype -> Provider<Singleton> = OK
  @Test
  public void shouldRegisterPrototypeBeanDependentOnXProviderWhenXIsProvidedAsSingleton() throws Exception {
    injector.registerInstance(new X());
    injector.register(PrototypeBeanDependantOnXProvider.class);
  }

  // Singleton -> Provider<Prototype> = OK
  @Test
  public void shouldRegisterSingletonBeanDependentOnXProviderWhenXIsProvidedAsPrototype() throws Exception {
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
