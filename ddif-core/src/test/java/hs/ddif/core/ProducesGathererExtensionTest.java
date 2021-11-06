package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.inject.store.FieldInjectable;
import hs.ddif.core.inject.store.MethodInjectable;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProducesGathererExtensionTest {
  private ProducesGathererExtension extension = new ProducesGathererExtension();

  @Test
  void shouldFindProducesAnnotatedMethods() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
    List<ResolvableInjectable> injectables = extension.getDerived(new ClassInjectable(A.class));

    assertThat(injectables).containsExactlyInAnyOrder(
      new MethodInjectable(A.class.getDeclaredMethod("createB"), A.class),
      new MethodInjectable(A.class.getDeclaredMethod("createC"), A.class),
      new FieldInjectable(A.class.getDeclaredField("d"), A.class),
      new FieldInjectable(A.class.getDeclaredField("e"), A.class)
    );
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
