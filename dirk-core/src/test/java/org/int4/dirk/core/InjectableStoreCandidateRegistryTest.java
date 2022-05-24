package org.int4.dirk.core;

import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.int4.dirk.api.CandidateRegistry;
import org.int4.dirk.api.definition.AutoDiscoveryException;
import org.int4.dirk.api.definition.UnsatisfiedDependencyException;
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
      .hasMessage("Unable to register [class org.int4.dirk.core.InjectableStoreCandidateRegistryTest$A]\n"
        + "    -> [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B] required by [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$A], via Field [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B org.int4.dirk.core.InjectableStoreCandidateRegistryTest$A.b], is not registered and cannot be discovered (reason: [class org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasMessage("Missing dependency [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B] required for Field [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B org.int4.dirk.core.InjectableStoreCandidateRegistryTest$A.b]")
      .hasNoCause();

    assertThatThrownBy(() -> manual.register(A.class))
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasMessage("Missing dependency [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B] required for Field [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B org.int4.dirk.core.InjectableStoreCandidateRegistryTest$A.b]")
      .hasNoCause();
  }

  @Test
  void shouldGiveClearProblemDescriptionWhenRegistrationOfTwoTypesFails() {
    assertThatThrownBy(() -> registry.register(List.of(A.class, C.class)))
      .isExactlyInstanceOf(AutoDiscoveryException.class)
      .hasMessage("Unable to register [class org.int4.dirk.core.InjectableStoreCandidateRegistryTest$A, class org.int4.dirk.core.InjectableStoreCandidateRegistryTest$C] and the discovered types [Class [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$D]]\n"
        + "    -> [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B] required by [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$A], via Field [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B org.int4.dirk.core.InjectableStoreCandidateRegistryTest$A.b], is not registered and cannot be discovered (reason: [class org.int4.dirk.core.InjectableStoreCandidateRegistryTest$B] should have at least one suitable constructor; annotate a constructor or provide an empty public constructor)\n"
        + "    -> [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$E] required by [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$D] required by [org.int4.dirk.core.InjectableStoreCandidateRegistryTest$C], via Parameter 0 [class org.int4.dirk.core.InjectableStoreCandidateRegistryTest$E] of [public org.int4.dirk.core.InjectableStoreCandidateRegistryTest$D(org.int4.dirk.core.InjectableStoreCandidateRegistryTest$E)], is not registered and cannot be discovered (reason: Field [final java.lang.String org.int4.dirk.core.InjectableStoreCandidateRegistryTest$E.d] of [class org.int4.dirk.core.InjectableStoreCandidateRegistryTest$E] cannot be final)")
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
