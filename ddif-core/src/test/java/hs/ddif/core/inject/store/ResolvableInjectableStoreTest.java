package hs.ddif.core.inject.store;

import hs.ddif.annotations.Produces;
import hs.ddif.core.ProducesStoreExtension;
import hs.ddif.core.inject.consistency.CyclicDependencyException;
import hs.ddif.core.inject.consistency.InjectorStoreConsistencyPolicy;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ResolvableInjectableStoreTest {

  enum StoreSupplier {
    NO_DISCOVERING(() -> new ResolvableInjectableStore(new InjectorStoreConsistencyPolicy<>(), List.of(), false)),
    AUTO_DISCOVERING(() -> new ResolvableInjectableStore(new InjectorStoreConsistencyPolicy<>(), List.of(), true)),
    NO_DISCOVERING__PRODUCES(() -> new ResolvableInjectableStore(new InjectorStoreConsistencyPolicy<>(), List.of(new ProducesStoreExtension()), false)),
    AUTO_DISCOVERING__PRODUCES(() -> new ResolvableInjectableStore(new InjectorStoreConsistencyPolicy<>(), List.of(new ProducesStoreExtension()), true));

    private final Supplier<ResolvableInjectableStore> supplier;

    StoreSupplier(Supplier<ResolvableInjectableStore> supplier) {
      this.supplier = supplier;
    }

    public ResolvableInjectableStore getStore() {
      return supplier.get();
    }
  }

  @ParameterizedTest
  @EnumSource(mode = Mode.MATCH_ALL, names = ".*NO_DISCOVERING.*")
  void shouldNotDiscoverTypeWithPublicEmptyConstructor(StoreSupplier supplier) {
    ResolvableInjectableStore store = supplier.getStore();

    assertThat(store.resolve(Simple.class)).isEmpty();
    assertThat(store.toSet()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(mode = Mode.MATCH_ALL, names = ".*AUTO_DISCOVERING.*")
  void shouldDiscoverTypeWithPublicEmptyConstructor(StoreSupplier supplier) {
    ResolvableInjectableStore store = supplier.getStore();

    assertThat(store.resolve(Simple.class)).hasSize(1);
    assertThat(store.toSet()).containsExactlyInAnyOrder(
      new ClassInjectable(Simple.class)
    );
  }

  @ParameterizedTest
  @EnumSource(mode = Mode.MATCH_ALL, names = ".*AUTO_DISCOVERING.*")
  void shouldFailToDiscoverTypeWithoutPublicEmptyConstructor(StoreSupplier supplier) {
    ResolvableInjectableStore store = supplier.getStore();

    assertThatThrownBy(() -> store.resolve(DependentUnannotated.class))
      .isInstanceOf(BindingException.class)
      .hasMessageStartingWith("No suitable constructor found; provide an empty constructor or annotate one with @Inject:");
    assertThat(store.toSet()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(mode = Mode.MATCH_ALL, names = ".*AUTO_DISCOVERING.*")
  void shouldDiscoverTypeWithAnnotatedConstructor(StoreSupplier supplier) {
    ResolvableInjectableStore store = supplier.getStore();

    assertThat(store.resolve(DependentOnSimple.class)).hasSize(1);
    assertThat(store.toSet()).containsExactlyInAnyOrder(
      new ClassInjectable(Simple.class),
      new ClassInjectable(DependentOnSimple.class)
    );
  }

  @ParameterizedTest
  @EnumSource(mode = Mode.MATCH_ALL, names = ".*NO_DISCOVERING.*")
  void shouldNotDiscoverTypeWithoutPublicEmptyConstructor(StoreSupplier supplier) {
    ResolvableInjectableStore store = supplier.getStore();

    assertThat(store.resolve(DependentUnannotated.class)).isEmpty();
    assertThat(store.toSet()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(mode = Mode.MATCH_ALL, names = {".*AUTO_DISCOVERING.*", ".*PRODUCES.*"})
  void shouldDiscoverProducerClassAndAddProducerMethod(StoreSupplier supplier) throws NoSuchMethodException, SecurityException {
    ResolvableInjectableStore store = supplier.getStore();

    assertThat(store.resolve(SimpleProducer.class)).hasSize(1);
    assertThat(store.toSet()).containsExactlyInAnyOrder(
      new ClassInjectable(SimpleProducer.class),
      new MethodInjectable(SimpleProducer.class.getDeclaredMethod("createSimple"), SimpleProducer.class)
    );
  }

  @ParameterizedTest
  @EnumSource(mode = Mode.MATCH_ALL, names = {".*AUTO_DISCOVERING.*", ".*PRODUCES.*"})
  void shouldRejectClassDependentOnNonStaticProducerInSameClass(StoreSupplier supplier) {
    ResolvableInjectableStore store = supplier.getStore();

    assertThatThrownBy(() -> store.resolve(DependentOnSimple_SimpleProducer.class))
      .isInstanceOf(CyclicDependencyException.class);
  }

  @ParameterizedTest
  @EnumSource(mode = Mode.MATCH_ALL, names = {".*AUTO_DISCOVERING.*", ".*PRODUCES.*"})
  void shouldDiscoverClassDependentOnStaticProducerInSameClass(StoreSupplier supplier) throws NoSuchMethodException, SecurityException {
    ResolvableInjectableStore store = supplier.getStore();

    assertThat(store.resolve(DependentOnSimple_StaticSimpleProducer.class)).hasSize(1);
    assertThat(store.toSet()).containsExactlyInAnyOrder(
      new ClassInjectable(DependentOnSimple_StaticSimpleProducer.class),
      new MethodInjectable(DependentOnSimple_StaticSimpleProducer.class.getDeclaredMethod("createSimple"), DependentOnSimple_StaticSimpleProducer.class)
    );
  }

  /*
   * Test Classes:
   */

  public static class Simple {
  }

  public static class DependentUnannotated {
    public DependentUnannotated(@SuppressWarnings("unused") Simple simple) {
    }
  }

  public static class DependentOnSimple {
    @Inject
    public DependentOnSimple(@SuppressWarnings("unused") Simple simple) {
    }
  }

  public static class SimpleProducer {
    @Produces
    public Simple createSimple() {
      return null;
    }
  }

  public static class DependentOnSimple_SimpleProducer {
    @Inject Simple simple;

    @Produces
    public Simple createSimple() {
      return new Simple();
    }
  }

  public static class DependentOnSimple_StaticSimpleProducer {
    @Inject Simple simple;

    @Produces
    public static Simple createSimple() {
      return new Simple();
    }
  }
}
