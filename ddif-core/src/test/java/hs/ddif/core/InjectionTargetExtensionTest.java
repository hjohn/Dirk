package hs.ddif.core;
import hs.ddif.annotations.Opt;
import hs.ddif.api.Injector;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;
import hs.ddif.util.Types;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class InjectionTargetExtensionTest {
  Injector injector = Injectors.manual();

  @Test
  void shouldGetInstancesForNestedTypes() throws Exception {
    injector.registerInstance("A");

    assertThat(injector.<Provider<String>>getInstance(Types.parameterize(Provider.class, String.class)).get()).isEqualTo("A");
    assertThat(injector.<Provider<Provider<String>>>getInstance(Types.parameterize(Provider.class, Types.parameterize(Provider.class, String.class))).get().get()).isEqualTo("A");

    Provider<Integer> integerProvider = injector.getInstance(Types.parameterize(Provider.class, Integer.class));

    assertThat(integerProvider).isInstanceOf(Provider.class);
    assertThatThrownBy(() -> integerProvider.get())
      .isExactlyInstanceOf(UnsatisfiedResolutionException.class);

    Provider<Provider<Integer>> integerProviderProvider = injector.getInstance(Types.parameterize(Provider.class, Types.parameterize(Provider.class, Integer.class)));

    assertThat(integerProviderProvider).isInstanceOf(Provider.class);
    assertThat(integerProviderProvider.get()).isInstanceOf(Provider.class);
    assertThatThrownBy(() -> integerProviderProvider.get().get())
      .isExactlyInstanceOf(UnsatisfiedResolutionException.class);
  }

  public void shouldInjectAvailableTypeInVariousNestedTargets() throws Exception {
    injector.register(IntegerProviderInjected.class);

    IntegerProviderInjected instance = injector.getInstance(IntegerProviderInjected.class);

    assertThat(instance.b.get()).isNull();
    assertThat(instance.c.get()).isEmpty();
    assertThat(instance.d.get()).isNull();
    assertThat(instance.e.get()).isEmpty();
    assertThat(instance.f.get()).isNull();
    assertThat(instance.h.get().get()).isNull();
  }

  public void shouldInjectDefaultsInVariousTargets() throws Exception {
    injector.registerInstance("A");
    injector.register(IntegerProviderInjected.class);

    StringProviderInjected instance = injector.getInstance(StringProviderInjected.class);

    assertThat(instance.a.get()).isEqualTo("A");
    assertThat(instance.b.get()).isEqualTo("A");
    assertThat(instance.c.get()).containsExactly("A");
    assertThat(instance.d.get()).containsExactly("A");
    assertThat(instance.e.get()).containsExactly("A");
    assertThat(instance.f.get()).containsExactly("A");
    assertThat(instance.g.get().get()).isEqualTo("A");
    assertThat(instance.h.get().get()).isEqualTo("A");
  }

  public static class StringProviderInjected {
    @Inject Provider<String> a;
    @Inject @Opt Provider<String> b;
    @Inject Provider<List<String>> c;
    @Inject @Opt Provider<List<String>> d;
    @Inject Provider<Set<String>> e;
    @Inject @Opt Provider<Set<String>> f;
    @Inject Provider<Provider<String>> g;
    @Inject @Opt Provider<Provider<String>> h;
  }

  public static class IntegerProviderInjected {
    @Inject @Opt Provider<Integer> b;
    @Inject Provider<List<Integer>> c;
    @Inject @Opt Provider<List<Integer>> d;
    @Inject Provider<Set<Integer>> e;
    @Inject @Opt Provider<Set<Integer>> f;
    @Inject @Opt Provider<Provider<Integer>> h;
  }
}
