package hs.ddif.core;

import hs.ddif.core.api.InstanceCreationException;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.test.injectables.BeanWithInjection;
import hs.ddif.core.test.injectables.BigRedBean;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SimpleBean;

import java.util.regex.Pattern;

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
  private Injector injector = new Injector(ResolvableInjectable::new, true);

  @Test
  public void shouldDiscoverNewTypes() {
    assertNotNull(injector.getInstance(BigRedBean.class));
    assertTrue(injector.contains(BigRedBean.class));
  }

  @Test
  public void shouldDiscoverNewTypesAndDependentTypes() {
    assertNotNull(injector.getInstance(BeanWithInjection.class));
    assertTrue(injector.contains(BeanWithInjection.class));
    assertTrue(injector.contains(SimpleBean.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithoutAnyConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithDependencyOnSampleWithoutConstructorMatch.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("Exception during auto discovery: class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Unable to inject: Field [public hs.ddif.core.test.injectables.SampleWithoutConstructorMatch hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch.sampleWithoutConstructorMatch] with: [class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch")
      .hasNoCause();

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithoutConstructorMatch.class));
    assertFalse(injector.contains(SampleWithoutConstructorMatch.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithMultipleConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("Exception during auto discovery: class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Unable to inject: Field [public hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.sample] with: [class hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Multiple @Inject annotated constructors found, but only one allowed: class hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors")
      .hasNoCause();

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class));
    assertFalse(injector.contains(SampleWithMultipleAnnotatedConstructors.class));
  }

  @Test
  public void shouldThrowBindingExceptionWhenAddingClassWithoutConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithoutConstructorMatch.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("Exception during auto discovery: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("No suitable constructor found; provide an empty constructor or annotate one with @Inject: class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch")
      .hasNoCause();
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
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("Exception during auto discovery: class hs.ddif.core.InjectorDiscoveryTest$D")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessage("Unable to resolve 2 binding(s) while processing extensions")
      .hasNoCause();

    assertFalse(injector.contains(A.class));
    assertFalse(injector.contains(B.class));
    assertFalse(injector.contains(C.class));
    assertFalse(injector.contains(D.class));
  }

  @Test
  public void shouldThrowBindingExceptionWhenDiscoveredClassRequiresQualifiers() {
    assertThatThrownBy(() -> injector.getInstance(G.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("Exception during auto discovery: class hs.ddif.core.InjectorDiscoveryTest$G")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessageMatching(Pattern.quote("Unable to inject: Field [hs.ddif.core.InjectorDiscoveryTest$A hs.ddif.core.InjectorDiscoveryTest$G.a] with: [@javax.inject.Named(") + "(value=)?" + Pattern.quote("\"some-qualifier\") class hs.ddif.core.InjectorDiscoveryTest$A]"))
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BindingException.class)
      .hasMessageMatching(Pattern.quote("Auto discovered class cannot be required to have qualifiers: [@javax.inject.Named(") + "(value=)?" + Pattern.quote("\"some-qualifier\") class hs.ddif.core.InjectorDiscoveryTest$A]"))
      .hasNoCause();
  }

  @Test
  public void shouldReturnEmptyListWhenNoInstancesOfInterfaceKnown() {
    assertThat(injector.getInstances(E.class)).isEmpty();  // auto discovery should not trigger for #getInstances
  }

  @Test
  public void shouldReturnEmptyListWhenNoInstancesOfDiscoverableClassKnown() {
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
