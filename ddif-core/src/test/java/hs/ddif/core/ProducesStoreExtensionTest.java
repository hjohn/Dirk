package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.inject.store.FieldInjectable;
import hs.ddif.core.inject.store.MethodInjectable;
import hs.ddif.core.store.InjectableStore;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProducesStoreExtensionTest {
  private ProducesStoreExtension extension = new ProducesStoreExtension();

  @Test
  void shouldFindProducesAnnotatedMethods() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
    InjectableStore<ResolvableInjectable> store = new InjectableStore<>();

    List<ResolvableInjectable> injectables = extension.getDerived(store, new ClassInjectable(A.class)).stream()
      .map(Supplier::get)
      .collect(Collectors.toList());

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
