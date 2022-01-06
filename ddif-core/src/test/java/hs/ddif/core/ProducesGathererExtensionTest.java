package hs.ddif.core;

import hs.ddif.annotations.Produces;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.inject.store.FieldInjectableFactory;
import hs.ddif.core.inject.store.MethodInjectableFactory;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProducesGathererExtensionTest {
  private final ClassInjectableFactory classInjectableFactory = InjectableFactories.forClass();
  private final MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(ResolvableInjectable::new);
  private final FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(ResolvableInjectable::new);

  private ProducesGathererExtension extension = new ProducesGathererExtension(methodInjectableFactory, fieldInjectableFactory);

  @Test
  void shouldFindProducesAnnotatedMethods() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
    List<ResolvableInjectable> injectables = extension.getDerived(classInjectableFactory.create(A.class));

    assertThat(injectables).containsExactlyInAnyOrder(
      methodInjectableFactory.create(A.class.getDeclaredMethod("createB"), A.class),
      methodInjectableFactory.create(A.class.getDeclaredMethod("createC"), A.class),
      fieldInjectableFactory.create(A.class.getDeclaredField("d"), A.class),
      fieldInjectableFactory.create(A.class.getDeclaredField("e"), A.class)
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
