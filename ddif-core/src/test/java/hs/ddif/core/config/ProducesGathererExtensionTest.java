package hs.ddif.core.config;

import hs.ddif.annotations.Produces;
import hs.ddif.core.config.standard.DefaultBinding;
import hs.ddif.core.config.standard.DefaultInjectable;
import hs.ddif.core.inject.bind.BindingProvider;
import hs.ddif.core.inject.injectable.ClassInjectableFactory;
import hs.ddif.core.inject.injectable.FieldInjectableFactory;
import hs.ddif.core.inject.injectable.InjectableFactories;
import hs.ddif.core.inject.injectable.MethodInjectableFactory;
import hs.ddif.core.inject.injectable.ResolvableInjectable;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProducesGathererExtensionTest {
  private final BindingProvider bindingProvider = new BindingProvider(DefaultBinding::new);
  private final ClassInjectableFactory classInjectableFactory = InjectableFactories.forClass();
  private final MethodInjectableFactory methodInjectableFactory = new MethodInjectableFactory(bindingProvider, DefaultInjectable::new);
  private final FieldInjectableFactory fieldInjectableFactory = new FieldInjectableFactory(bindingProvider, DefaultInjectable::new);

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