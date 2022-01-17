package hs.ddif.core;

import hs.ddif.core.api.InstanceCreationException;
import hs.ddif.core.inject.injectable.DefinitionException;
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
      .hasMessage("Path [class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch] -> Field [public hs.ddif.core.test.injectables.SampleWithoutConstructorMatch hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch.sampleWithoutConstructorMatch]: [class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
      .hasNoCause();

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithoutConstructorMatch.class));
    assertFalse(injector.contains(SampleWithoutConstructorMatch.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithMultipleConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("Path [class hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors] -> Field [public hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.sample]: [class hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors] cannot have multiple Inject annotated constructors")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors] cannot have multiple Inject annotated constructors")
      .hasNoCause();

    assertFalse(injector.contains(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class));
    assertFalse(injector.contains(SampleWithMultipleAnnotatedConstructors.class));
  }

  @Test
  public void shouldThrowBindingExceptionWhenAddingClassWithoutConstructorMatch() {
    assertThatThrownBy(() -> injector.getInstance(SampleWithoutConstructorMatch.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("Path [class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch]: [class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.test.injectables.SampleWithoutConstructorMatch] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor")
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
      .hasMessage("Path [class hs.ddif.core.InjectorDiscoveryTest$D] -> [class hs.ddif.core.InjectorDiscoveryTest$C] -> Field [hs.ddif.core.InjectorDiscoveryTest$E hs.ddif.core.InjectorDiscoveryTest$C.e]: [interface hs.ddif.core.InjectorDiscoveryTest$E] cannot be injected; failures:\n"
        + " - Type must have a single abstract method to qualify for assisted injection: interface hs.ddif.core.InjectorDiscoveryTest$E\n"
        + " - Type cannot be abstract: interface hs.ddif.core.InjectorDiscoveryTest$E"
      )
      .hasSuppressedException(new DefinitionException(
        "Path [class hs.ddif.core.InjectorDiscoveryTest$D] -> [class hs.ddif.core.InjectorDiscoveryTest$C] -> Field [hs.ddif.core.InjectorDiscoveryTest$F hs.ddif.core.InjectorDiscoveryTest$C.f]: [interface hs.ddif.core.InjectorDiscoveryTest$F] cannot be injected; failures:\n"
        + " - Type must have a single abstract method to qualify for assisted injection: interface hs.ddif.core.InjectorDiscoveryTest$F\n"
        + " - Type cannot be abstract: interface hs.ddif.core.InjectorDiscoveryTest$F",
        new DefinitionException(
          "[interface hs.ddif.core.InjectorDiscoveryTest$F] cannot be injected; failures:\n"
          + " - Type must have a single abstract method to qualify for assisted injection: interface hs.ddif.core.InjectorDiscoveryTest$F\n"
          + " - Type cannot be abstract: interface hs.ddif.core.InjectorDiscoveryTest$F",
          null
        )
      ))
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[interface hs.ddif.core.InjectorDiscoveryTest$E] cannot be injected; failures:\n"
        + " - Type must have a single abstract method to qualify for assisted injection: interface hs.ddif.core.InjectorDiscoveryTest$E\n"
        + " - Type cannot be abstract: interface hs.ddif.core.InjectorDiscoveryTest$E"
      )
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
      .hasMessageMatching(Pattern.quote("Path [class hs.ddif.core.InjectorDiscoveryTest$G] -> Field [@javax.inject.Named(") + "(value=)?" + Pattern.quote("\"some-qualifier\") hs.ddif.core.InjectorDiscoveryTest$A hs.ddif.core.InjectorDiscoveryTest$G.a]: [class hs.ddif.core.InjectorDiscoveryTest$A] found during auto discovery is missing qualifiers required by: Field [@javax.inject.Named(") + "(value=)?" + Pattern.quote("\"some-qualifier\") hs.ddif.core.InjectorDiscoveryTest$A hs.ddif.core.InjectorDiscoveryTest$G.a]"))
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessageMatching(Pattern.quote("[class hs.ddif.core.InjectorDiscoveryTest$A] found during auto discovery is missing qualifiers required by: Field [@javax.inject.Named(") + "(value=)?" + Pattern.quote("\"some-qualifier\") hs.ddif.core.InjectorDiscoveryTest$A hs.ddif.core.InjectorDiscoveryTest$G.a]"))
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
