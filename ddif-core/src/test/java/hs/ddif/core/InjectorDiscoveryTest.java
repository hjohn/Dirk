package hs.ddif.core;

import hs.ddif.api.Injector;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.instantiation.domain.InstanceCreationException;
import hs.ddif.core.definition.BindingException;
import hs.ddif.core.discovery.DiscoveryFailure;
import hs.ddif.core.inject.store.UnresolvableDependencyException;
import hs.ddif.core.test.injectables.BeanWithInjection;
import hs.ddif.core.test.injectables.BigRedBean;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SimpleBean;

import java.util.regex.Pattern;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class InjectorDiscoveryTest {
  private Injector injector = Injectors.autoDiscovering();

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
      .hasMessage("[hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch] instantiation failed because auto discovery was unable to resolve all dependencies; found: [Injectable[hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch]]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DiscoveryFailure.class)
      .hasMessage("[hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch] instantiation failed because auto discovery was unable to resolve all dependencies; found: [Injectable[hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch]]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] required for Field [public hs.ddif.core.test.injectables.SampleWithoutConstructorMatch hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch.sampleWithoutConstructorMatch]")
      .hasNoCause();

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithoutConstructorMatch.class));
    assertFalse(injector.contains(SampleWithoutConstructorMatch.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithMultipleConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("[hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors] instantiation failed because auto discovery was unable to resolve all dependencies; found: [Injectable[hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors]]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DiscoveryFailure.class)
      .hasMessage("[hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors] instantiation failed because auto discovery was unable to resolve all dependencies; found: [Injectable[hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors]]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors] required for Field [public hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.sample]")
      .hasNoCause();

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class));
    assertFalse(injector.contains(SampleWithMultipleAnnotatedConstructors.class));
  }

  @Test
  public void shouldThrowBindingExceptionWhenAddingClassWithoutConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithoutConstructorMatch.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("[hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] instantiation failed because auto discovery was unable to resolve all dependencies; found: []")
      .hasNoSuppressedExceptions()
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DiscoveryFailure.class)
      .hasMessage("[hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] instantiation failed because auto discovery was unable to resolve all dependencies; found: []")
      .hasNoSuppressedExceptions()
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Exception occurred during discovery via path: [hs.ddif.core.test.injectables.SampleWithoutConstructorMatch]")
      .satisfies(throwable -> {
        assertThat(throwable.getSuppressed()).hasSize(1);
        assertThat(throwable.getSuppressed()[0])
          .isExactlyInstanceOf(DefinitionException.class)
          .hasMessage("[class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] could not be bound")
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isExactlyInstanceOf(BindingException.class)
          .hasMessage("[class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
          .hasNoCause();
      })
      .hasNoCause();
  }

  @Test
  public void shouldDiscoverNewTypeWithEmptyUnannotatedConstructorAndAnnotatedConstructor() {
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
      .hasMessage("[hs.ddif.core.InjectorDiscoveryTest$D] instantiation failed because auto discovery was unable to resolve all dependencies; found: [Injectable[hs.ddif.core.InjectorDiscoveryTest$A], Injectable[hs.ddif.core.InjectorDiscoveryTest$B], Injectable[hs.ddif.core.InjectorDiscoveryTest$C], Injectable[hs.ddif.core.InjectorDiscoveryTest$D]]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DiscoveryFailure.class)
      .hasMessage("[hs.ddif.core.InjectorDiscoveryTest$D] instantiation failed because auto discovery was unable to resolve all dependencies; found: [Injectable[hs.ddif.core.InjectorDiscoveryTest$A], Injectable[hs.ddif.core.InjectorDiscoveryTest$B], Injectable[hs.ddif.core.InjectorDiscoveryTest$C], Injectable[hs.ddif.core.InjectorDiscoveryTest$D]]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.core.InjectorDiscoveryTest$E] required for Field [hs.ddif.core.InjectorDiscoveryTest$E hs.ddif.core.InjectorDiscoveryTest$C.e]")
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
      .hasMessage("[hs.ddif.core.InjectorDiscoveryTest$G] instantiation failed because auto discovery was unable to resolve all dependencies; found: [Injectable[hs.ddif.core.InjectorDiscoveryTest$G]]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DiscoveryFailure.class)
      .hasMessage("[hs.ddif.core.InjectorDiscoveryTest$G] instantiation failed because auto discovery was unable to resolve all dependencies; found: [Injectable[hs.ddif.core.InjectorDiscoveryTest$G]]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasMessageMatching(Pattern.quote("Missing dependency [@jakarta.inject.Named(") + "(value=)?" + Pattern.quote("\"some-qualifier\") hs.ddif.core.InjectorDiscoveryTest$A] required for Field [@jakarta.inject.Named(") + "(value=)?" + Pattern.quote("\"some-qualifier\") hs.ddif.core.InjectorDiscoveryTest$A hs.ddif.core.InjectorDiscoveryTest$G.a]"))
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
