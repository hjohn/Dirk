package hs.ddif.core.config;

import hs.ddif.annotations.Produces;
import hs.ddif.core.config.standard.DiscoveryExtension.Registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ProducesInjectableExtensionTest {
  private Registry registry = mock(Registry.class);
  private ProducesDiscoveryExtension extension = new ProducesDiscoveryExtension(Produces.class);

  @Test
  void shouldFindProducesAnnotatedMethods() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
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
