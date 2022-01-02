package hs.ddif.core.inject.consistency;

import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BindingProvider;
import hs.ddif.core.inject.store.ClassInjectableFactory;
import hs.ddif.core.store.Injectables;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UnresolvableDependencyExceptionTest {
  private final ClassInjectableFactory classInjectableFactory = new ClassInjectableFactory(ResolvableInjectable::new);

  @Test
  void constructorShouldAcceptValidParameters() throws NoSuchMethodException, SecurityException {
    List<Binding> bindings = classInjectableFactory.create(A.class).getBindings();
    UnresolvableDependencyException e;

    e = new UnresolvableDependencyException(
      bindings.stream().filter(b -> TypeUtils.isAssignable(b.getType(), Integer.class)).findFirst().get(),
      Collections.emptySet()
    );

    assertThat(e).hasMessageStartingWith("Missing dependency of type [class java.lang.Integer] required for Parameter 1 of [");

    e = new UnresolvableDependencyException(
      bindings.stream().filter(b -> TypeUtils.isAssignable(b.getType(), Double.class)).findFirst().get(),
      Set.of(Injectables.create(), Injectables.create())
    );

    assertThat(e)
      .hasMessageStartingWith("Multiple candidates for dependency of type [class java.lang.Double] required for Field [")
      .hasMessageEndingWith(": [Injectable(String.class), Injectable(String.class)]");

    e = new UnresolvableDependencyException(
      BindingProvider.ofMethod(A.class.getDeclaredMethod("d", Long.class), A.class).get(0),
      Collections.emptySet()
    );

    assertThat(e).hasMessageStartingWith("Missing dependency of type [class java.lang.Long] required for Parameter 0 of [");
  }

  static class A {
    String a;
    Integer b;
    @Inject Double c;

    @Inject
    public A(String a, Integer b) {
      this.a = a;
      this.b = b;
    }

    public void d(Long d) {
      this.b = d.intValue();
    }
  }
}

