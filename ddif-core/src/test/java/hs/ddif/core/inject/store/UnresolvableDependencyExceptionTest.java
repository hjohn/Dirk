package hs.ddif.core.inject.store;

import hs.ddif.core.config.ConfigurableAnnotationStrategy;
import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.InjectableFactories;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.store.Injectables;
import hs.ddif.core.store.Key;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.util.Annotations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Scope;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UnresolvableDependencyExceptionTest {
  private final ClassInjectableFactory classInjectableFactory = new InjectableFactories().forClass();
  private final BindingProvider bindingProvider = new BindingProvider(new ConfigurableAnnotationStrategy(Inject.class, Qualifier.class, Scope.class, null));

  @Test
  void constructorShouldAcceptValidParameters() throws NoSuchMethodException, SecurityException, BadQualifiedTypeException {
    List<Binding> bindings = classInjectableFactory.create(A.class).getBindings();
    UnresolvableDependencyException e;

    e = new UnresolvableDependencyException(
      new Key(Integer.class),
      bindings.stream().filter(b -> TypeUtils.isAssignable(b.getType(), Integer.class)).findFirst().get(),
      Collections.emptySet()
    );

    assertThat(e).hasMessageStartingWith("Missing dependency [java.lang.Integer] required for Parameter 1 [class java.lang.Integer] of [");

    e = new UnresolvableDependencyException(
      new Key(Double.class, Set.of(Annotations.of(Red.class))),
      bindings.stream().filter(b -> TypeUtils.isAssignable(b.getType(), Double.class)).findFirst().get(),
      Set.of(Injectables.create(), Injectables.create())
    );

    assertThat(e)
      .hasMessageStartingWith("Multiple candidates for dependency [@hs.ddif.core.test.qualifiers.Red() java.lang.Double] required for Field [")
      .hasMessageEndingWith(": [Injectable(String.class), Injectable(String.class)]");

    e = new UnresolvableDependencyException(
      new Key(Long.class),
      bindingProvider.ofMethod(A.class.getDeclaredMethod("d", Long.class), A.class).get(0),
      Collections.emptySet()
    );

    assertThat(e).hasMessageStartingWith("Missing dependency [java.lang.Long] required for Parameter 0 [class java.lang.Long] of [");
  }

  static class A {
    String a;
    Integer b;
    @Inject @Red Double c;

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

