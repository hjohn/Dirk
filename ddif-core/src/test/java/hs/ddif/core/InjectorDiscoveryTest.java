package hs.ddif.core;

import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class InjectorDiscoveryTest {
  private Injector injector = new Injector(true);

  @Test
  public void shouldDiscoverNewTypes() throws BeanResolutionException {
    assertThat(injector.getInstances(BigRedBean.class)).isNotEmpty();
    assertTrue(injector.contains(BigRedBean.class));
  }

  @Test
  public void shouldDiscoverNewTypesAndDependentTypes() throws BeanResolutionException {
    assertThat(injector.getInstances(BeanWithInjection.class)).isNotEmpty();
    assertTrue(injector.contains(BeanWithInjection.class));
    assertTrue(injector.contains(SimpleBean.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithoutAnyConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstances(SampleWithDependencyOnSampleWithoutConstructorMatch.class))
      .isInstanceOf(BindingException.class)
      .hasMessage("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch");

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithoutConstructorMatch.class));
    assertFalse(injector.contains(SampleWithoutConstructorMatch.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithMultipleConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstances(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class))
      .isInstanceOf(BindingException.class)
      .hasMessage("Multiple @Inject annotated constructors found, but only one allowed: class hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors");

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class));
    assertFalse(injector.contains(SampleWithMultipleAnnotatedConstructors.class));
  }

  @Test
  public void shouldThrowBindingExceptionWhenAddingClassWithoutConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstances(SampleWithoutConstructorMatch.class))
      .isInstanceOf(BindingException.class)
      .hasMessage("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch");
  }

  @Test
  public void shouldDiscoverNewTypeWithEmptyUnannotatedConstructorAndAnnotatedConstructor() throws Exception {
    assertFalse(injector.getInstances(SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor.class).isEmpty());
  }

  @Test
  public void autoDiscoveryShouldNotLeaveStoreInModifiedState() {

    /*
     * D dependencies are checked.  C and B are auto discovered.
     * B dependencies are checked.  A is auto discovered.
     * C dependencies are checked.  A is present.  E is not, and cannot be auto discovered as it is an interface.  Fatal.
     */

    assertThat(assertThrows(BeanResolutionException.class, () -> injector.getInstance(D.class)))
      .hasMessageMatching("No such bean: class hs.ddif.core.InjectorDiscoveryTest\\$D");

    assertFalse(injector.contains(A.class));
    assertFalse(injector.contains(B.class));
    assertFalse(injector.contains(C.class));
    assertFalse(injector.contains(D.class));
  }

  public static class A {
  }

  public static class B {
    @Inject A a;
  }

  public static class C {
    @Inject A a;
    @Inject E e;
  }

  public static class D {
    @Inject B b;
    @Inject C c;
  }

  interface E {
  }
}
