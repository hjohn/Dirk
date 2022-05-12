package hs.ddif.core;

import hs.ddif.api.Injector;
import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.definition.DependencyException;
import hs.ddif.api.definition.UnsatisfiedDependencyException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.core.test.injectables.BeanWithInjection;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SimpleBean;
import hs.ddif.core.test.qualifiers.Red;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

public class InjectorDiscoveryTest {
  private Injector injector = Injectors.autoDiscovering();

  @Test
  public void shouldDiscoverDependentTypes() throws Exception {
    injector.register(BeanWithInjection.class);

    assertTrue(injector.contains(BeanWithInjection.class));
    assertTrue(injector.contains(SimpleBean.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithoutAnyConstructorMatch() {
    assertThatThrownBy(() -> injector.register(SampleWithDependencyOnSampleWithoutConstructorMatch.class))
      .isExactlyInstanceOf(AutoDiscoveryException.class)
      .hasMessage("Unable to register [class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch]\n"
        + "    -> [hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] required by [hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch], via Field [public hs.ddif.core.test.injectables.SampleWithoutConstructorMatch hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch.sampleWithoutConstructorMatch], is not registered and cannot be discovered (reason: [class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] required for Field [public hs.ddif.core.test.injectables.SampleWithoutConstructorMatch hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch.sampleWithoutConstructorMatch]")
      .hasNoCause();

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithoutConstructorMatch.class));
    assertFalse(injector.contains(SampleWithoutConstructorMatch.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithMultipleConstructorMatch() {
    assertThatThrownBy(() -> injector.register(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class))
      .isExactlyInstanceOf(AutoDiscoveryException.class)
      .hasMessage("Unable to register [class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors]\n"
        + "    -> [hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors] required by [hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors], via Field [public hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.sample], is not registered and cannot be discovered (reason: [class hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors] cannot have multiple Inject annotated constructors)")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors] required for Field [public hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.sample]")
      .hasNoCause();

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class));
    assertFalse(injector.contains(SampleWithMultipleAnnotatedConstructors.class));
  }

  @Test
  public void shouldThrowDefinitionExceptionWhenAddingClassWithoutConstructorMatch() {
    assertThatThrownBy(() -> injector.register(SampleWithoutConstructorMatch.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
      .hasNoCause();
  }

  @Test
  public void shouldDiscoverNewTypeWithEmptyUnannotatedConstructorAndAnnotatedConstructor() throws Exception {
    injector.register(SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor.class);

    assertNotNull(injector.getInstance(SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor.class));
  }

  @Test
  public void autoDiscoveryShouldNotLeaveStoreInModifiedState() {

    /*
     * D dependencies are checked.  C and B are auto discovered.
     * B dependencies are checked.  A is auto discovered.
     * C dependencies are checked.  A is present.  E and F are not, and cannot be auto discovered as they're interfaces.  Fatal.
     */

    assertThatThrownBy(() -> injector.register(D.class))
      .isExactlyInstanceOf(AutoDiscoveryException.class)
      .hasMessage("Unable to register [class hs.ddif.core.InjectorDiscoveryTest$D] and the discovered types [Class [hs.ddif.core.InjectorDiscoveryTest$A], Class [hs.ddif.core.InjectorDiscoveryTest$B], Class [hs.ddif.core.InjectorDiscoveryTest$C]]\n"
        + "    -> [hs.ddif.core.InjectorDiscoveryTest$E] required by [hs.ddif.core.InjectorDiscoveryTest$C] required by [hs.ddif.core.InjectorDiscoveryTest$D], via Field [hs.ddif.core.InjectorDiscoveryTest$E hs.ddif.core.InjectorDiscoveryTest$C.e], is not registered and cannot be discovered (reason: [interface hs.ddif.core.InjectorDiscoveryTest$E] cannot be abstract)\n"
        + "    -> [hs.ddif.core.InjectorDiscoveryTest$F] required by [hs.ddif.core.InjectorDiscoveryTest$C] required by [hs.ddif.core.InjectorDiscoveryTest$D], via Field [hs.ddif.core.InjectorDiscoveryTest$F hs.ddif.core.InjectorDiscoveryTest$C.f], is not registered and cannot be discovered (reason: [interface hs.ddif.core.InjectorDiscoveryTest$F] cannot be abstract)")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.core.InjectorDiscoveryTest$E] required for Field [hs.ddif.core.InjectorDiscoveryTest$E hs.ddif.core.InjectorDiscoveryTest$C.e]")
      .hasNoCause();

    assertFalse(injector.contains(A.class));
    assertFalse(injector.contains(B.class));
    assertFalse(injector.contains(C.class));
    assertFalse(injector.contains(D.class));
  }

  @Test
  public void shouldThrowAutoDiscoveryExceptionWhenDiscoveredClassRequiresQualifiers() {
    assertThatThrownBy(() -> injector.register(G.class))
      .isExactlyInstanceOf(AutoDiscoveryException.class)
      .hasMessage("Unable to register [class hs.ddif.core.InjectorDiscoveryTest$G]\n"
        + "    -> [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.InjectorDiscoveryTest$A] required by [hs.ddif.core.InjectorDiscoveryTest$G], via Field [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.InjectorDiscoveryTest$A hs.ddif.core.InjectorDiscoveryTest$G.a], is not registered and cannot be discovered (reason: [class hs.ddif.core.InjectorDiscoveryTest$A] is missing the required qualifiers: [@hs.ddif.core.test.qualifiers.Red()])")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasMessage("Missing dependency [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.InjectorDiscoveryTest$A] required for Field [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.InjectorDiscoveryTest$A hs.ddif.core.InjectorDiscoveryTest$G.a]")
      .hasNoCause();
  }

  @Test
  public void shouldReturnEmptyListWhenNoInstancesOfInterfaceKnown() throws CreationException {
    assertThat(injector.getInstances(E.class)).isEmpty();  // auto discovery should not trigger for #getInstances
  }

  @Test
  public void shouldReturnEmptyListWhenNoInstancesOfDiscoverableClassKnown() throws CreationException {
    assertThat(injector.getInstances(A.class)).isEmpty();  // auto discovery should not trigger for #getInstances
  }

  @Test
  public void registerShouldDiscoverNewTypes() throws AutoDiscoveryException, DefinitionException, DependencyException {
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
    @Red
    @Inject A a;
  }
}
