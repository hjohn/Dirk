package hs.ddif.library;

import hs.ddif.annotations.Produces;
import hs.ddif.spi.discovery.TypeRegistrationExtension.Registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ProducesTypeRegistrationExtensionTest {
  private Registry registry = mock(Registry.class);
  private ProducesTypeRegistrationExtension extension = new ProducesTypeRegistrationExtension(Produces.class);

  @Test
  void shouldFindProducesAnnotatedMethods() throws Exception {
    extension.deriveTypes(registry, A.class);

    verify(registry).add(A.class.getDeclaredMethod("createB"), A.class);
    verify(registry).add(A.class.getDeclaredMethod("createC"), A.class);
    verify(registry).add(A.class.getDeclaredField("d"), A.class);
    verify(registry).add(A.class.getDeclaredField("e"), A.class);
  }

  public static class A {
    @Produces
    private static E e = new E();

    @Produces
    private D d = new D();

    @Produces
    public static C createC() {
      return new C();
    }

    @Produces
    public B createB() {
      return new B();
    }
  }

  public static class B {
  }

  public static class C {
  }

  public static class D {
  }

  public static class E {
  }
}
