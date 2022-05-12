package hs.ddif.core;

import hs.ddif.api.CandidateRegistry;
import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.definition.UnsatisfiedDependencyException;

import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

public class InjectableStoreCandidateRegistryTest {
  private CandidateRegistry registry = Injectors.autoDiscovering();
  private CandidateRegistry manual = Injectors.manual();

  @Test
  void shouldGiveClearProblemDescriptionWhenRegistrationFails() {
    assertThatThrownBy(() -> registry.register(A.class))
      .isExactlyInstanceOf(AutoDiscoveryException.class)
      .hasMessage("Unable to register [class hs.ddif.core.InjectableStoreCandidateRegistryTest$A]\n"
        + "    -> [hs.ddif.core.InjectableStoreCandidateRegistryTest$B] required by [hs.ddif.core.InjectableStoreCandidateRegistryTest$A], via Field [hs.ddif.core.InjectableStoreCandidateRegistryTest$B hs.ddif.core.InjectableStoreCandidateRegistryTest$A.b], is not registered and cannot be discovered (reason: [class hs.ddif.core.InjectableStoreCandidateRegistryTest$B] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.core.InjectableStoreCandidateRegistryTest$B] required for Field [hs.ddif.core.InjectableStoreCandidateRegistryTest$B hs.ddif.core.InjectableStoreCandidateRegistryTest$A.b]")
      .hasNoCause();

    assertThatThrownBy(() -> manual.register(A.class))
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.core.InjectableStoreCandidateRegistryTest$B] required for Field [hs.ddif.core.InjectableStoreCandidateRegistryTest$B hs.ddif.core.InjectableStoreCandidateRegistryTest$A.b]")
      .hasNoCause();
  }

  @Test
  void shouldGiveClearProblemDescriptionWhenRegistrationOfTwoTypesFails() {
    assertThatThrownBy(() -> registry.register(List.of(A.class, C.class)))
      .isExactlyInstanceOf(AutoDiscoveryException.class)
      .hasMessage("Unable to register [class hs.ddif.core.InjectableStoreCandidateRegistryTest$A, class hs.ddif.core.InjectableStoreCandidateRegistryTest$C] and the discovered types [Class [hs.ddif.core.InjectableStoreCandidateRegistryTest$D]]\n"
        + "    -> [hs.ddif.core.InjectableStoreCandidateRegistryTest$B] required by [hs.ddif.core.InjectableStoreCandidateRegistryTest$A], via Field [hs.ddif.core.InjectableStoreCandidateRegistryTest$B hs.ddif.core.InjectableStoreCandidateRegistryTest$A.b], is not registered and cannot be discovered (reason: [class hs.ddif.core.InjectableStoreCandidateRegistryTest$B] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)\n"
        + "    -> [hs.ddif.core.InjectableStoreCandidateRegistryTest$E] required by [hs.ddif.core.InjectableStoreCandidateRegistryTest$D] required by [hs.ddif.core.InjectableStoreCandidateRegistryTest$C], via Parameter 0 [class hs.ddif.core.InjectableStoreCandidateRegistryTest$E] of [public hs.ddif.core.InjectableStoreCandidateRegistryTest$D(hs.ddif.core.InjectableStoreCandidateRegistryTest$E)], is not registered and cannot be discovered (reason: Field [final java.lang.String hs.ddif.core.InjectableStoreCandidateRegistryTest$E.d] of [class hs.ddif.core.InjectableStoreCandidateRegistryTest$E] cannot be final)")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      // message not verified as it is (currently) random which of the two problems the InjectableStore will throw first
      .hasNoCause();

    assertThatThrownBy(() -> manual.register(List.of(A.class, C.class)))
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      // message not verified as it is (currently) random which of the two problems the InjectableStore will throw first
      .hasNoCause();
  }

  public static class A {
    @Inject B b;
  }

  public static class B {  // not going to be discovered as it has no suitable constructor
    private B() {
    }
  }

  public static class C {
    @Inject D d;
  }

  public static class D {
    E e;

    @Inject
    public D(E e) {
      this.e = e;
    }
  }

  public static class E {  // not going to be discovered as it is has a final field annotated with Inject
    @Inject final String d = "";
  }
}
