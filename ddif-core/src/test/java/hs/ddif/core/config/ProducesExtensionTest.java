package hs.ddif.core.config;

import hs.ddif.annotations.Produces;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.InjectableFactories;
import hs.ddif.core.definition.MethodInjectableFactory;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProducesExtensionTest {
  private final InjectableFactories injectableFactories = new InjectableFactories();
  private final MethodInjectableFactory methodInjectableFactory = injectableFactories.forMethod();
  private final FieldInjectableFactory fieldInjectableFactory = injectableFactories.forField();

  private ProducesExtension extension = new ProducesExtension(methodInjectableFactory, fieldInjectableFactory);

  @Test
  void shouldFindProducesAnnotatedMethods() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
    List<Injectable> injectables = extension.getDerived(A.class);

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
