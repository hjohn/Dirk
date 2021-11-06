package hs.ddif.core;

import hs.ddif.core.inject.instantiator.BeanResolutionException;
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
      .hasCause(new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch"));

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithoutConstructorMatch.class));
    assertFalse(injector.contains(SampleWithoutConstructorMatch.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithMultipleConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class))
      .isInstanceOf(BeanResolutionException.class)
      .hasMessage("No such bean: class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors")
      .hasCause(new BindingException("Multiple @Inject annotated constructors found, but only one allowed: class hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors"));

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class));
    assertFalse(injector.contains(SampleWithMultipleAnnotatedConstructors.class));
  }

  @Test
  public void shouldThrowBindingExceptionWhenAddingClassWithoutConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithoutConstructorMatch.class))
      .isInstanceOf(BeanResolutionException.class)
      .hasMessage("No such bean: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch")
      .hasCause(new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch"));
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
      .hasCause(new BindingException("Unable to resolve 2 binding(s) while processing extensions"));

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
      .hasCause(new BindingException("Auto discovered class cannot be required to have qualifiers: [@javax.inject.Named[value=some-qualifier] class hs.ddif.core.InjectorDiscoveryTest$A]"));
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
