package hs.ddif.core;

import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.DiscoveryException;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.test.injectables.BeanWithInjection;
import hs.ddif.core.test.injectables.BigRedBean;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SimpleBean;

import javax.inject.Inject;
import javax.inject.Named;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InjectorDiscoveryTest {
  private Injector injector = new Injector(true);

  @Test
  public void shouldDiscoverNewTypes() throws BeanResolutionException {
    assertNotNull(injector.getInstance(BigRedBean.class));
    assertTrue(injector.contains(BigRedBean.class));
  }

  @Test
  public void shouldDiscoverNewTypesAndDependentTypes() throws BeanResolutionException {
    assertNotNull(injector.getInstance(BeanWithInjection.class));
    assertTrue(injector.contains(BeanWithInjection.class));
    assertTrue(injector.contains(SimpleBean.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithoutAnyConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithDependencyOnSampleWithoutConstructorMatch.class))
      .isInstanceOf(BeanResolutionException.class)
      .hasMessage("No such bean: class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(DiscoveryException.class)
      .hasMessage("Auto discovery failed for: class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(BindingException.class)
      .hasMessage("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch");

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithoutConstructorMatch.class));
    assertFalse(injector.contains(SampleWithoutConstructorMatch.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithMultipleConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class))
      .isInstanceOf(BeanResolutionException.class)
      .hasMessage("No such bean: class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(DiscoveryException.class)
      .hasMessage("Auto discovery failed for: class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(BindingException.class)
      .hasMessage("Multiple @Inject annotated constructors found, but only one allowed: class hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors");

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class));
    assertFalse(injector.contains(SampleWithMultipleAnnotatedConstructors.class));
  }

  @Test
  public void shouldThrowBindingExceptionWhenAddingClassWithoutConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithoutConstructorMatch.class))
      .isInstanceOf(BeanResolutionException.class)
      .hasMessage("No such bean: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(DiscoveryException.class)
      .hasMessage("Auto discovery failed for: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(BindingException.class)
      .hasMessage("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch");
  }

  @Test
  public void shouldDiscoverNewTypeWithEmptyUnannotatedConstructorAndAnnotatedConstructor() throws Exception {
    assertNotNull(injector.getInstance(SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor.class));
  }

  @Test
  public void autoDiscoveryShouldNotLeaveStoreInModifiedState() {

    /*
     * D dependencies are checked.  C and B are auto discovered.
     * B dependencies are checked.  A is auto discovered.
     * C dependencies are checked.  A is present.  E and F are not, and cannot be auto discovered as they're interfaces.  Fatal.
     */

    assertThatThrownBy(() -> injector.getInstance(D.class))
      .isInstanceOf(BeanResolutionException.class)
      .hasMessage("No such bean: class hs.ddif.core.InjectorDiscoveryTest$D")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(DiscoveryException.class)
      .hasMessage("Auto discovery failed for: class hs.ddif.core.InjectorDiscoveryTest$D")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(BindingException.class)
      .hasMessage("Unable to resolve 2 binding(s) while processing extensions");

    assertFalse(injector.contains(A.class));
    assertFalse(injector.contains(B.class));
    assertFalse(injector.contains(C.class));
    assertFalse(injector.contains(D.class));
  }

  @Test
  public void shouldThrowBindingExceptionWhenDiscoveredClassRequiresQualifiers() {
    assertThatThrownBy(() -> injector.getInstance(G.class))
      .isInstanceOf(BeanResolutionException.class)
      .hasMessage("No such bean: class hs.ddif.core.InjectorDiscoveryTest$G")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(DiscoveryException.class)
      .hasMessage("Auto discovery failed for: class hs.ddif.core.InjectorDiscoveryTest$G")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isInstanceOf(BindingException.class)
      .hasMessage("Auto discovered class cannot be required to have qualifiers: [@javax.inject.Named[value=some-qualifier] class hs.ddif.core.InjectorDiscoveryTest$A]");
  }

  @Test
  public void shouldReturnEmptyListWhenNoInstancesOfInterfaceKnown() throws BeanResolutionException {
    assertThat(injector.getInstances(E.class)).isEmpty();  // auto discovery should not trigger for #getInstances
  }

  @Test
  public void shouldReturnEmptyListWhenNoInstancesOfDiscoverableClassKnown() throws BeanResolutionException {
    assertThat(injector.getInstances(A.class)).isEmpty();  // auto discovery should not trigger for #getInstances
  }

  @Test
  public void registerShouldDiscoverNewTypes() {
    injector.register(B.class);

    assertNotNull(injector.contains(A.class));
  }

  public static class A {
  }

  public static class B {
    @Inject A a;
  }

  public static class C {
    @Inject A a;
    @Inject E e;
    @Inject F f;
  }

  public static class D {
    @Inject B b;
    @Inject C c;
  }

  interface E {
  }

  interface F {
  }

  public static class G {
    @Named("some-qualifier")
    @Inject A a;
  }
}
