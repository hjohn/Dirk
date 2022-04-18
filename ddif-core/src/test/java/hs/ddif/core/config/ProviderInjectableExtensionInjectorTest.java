package hs.ddif.core.config;

import hs.ddif.annotations.Opt;
import hs.ddif.api.Injector;
import hs.ddif.api.instantiation.domain.InstanceCreationException;
import hs.ddif.api.instantiation.domain.NoSuchInstanceException;
import hs.ddif.api.util.Types;
import hs.ddif.core.Injectors;
import hs.ddif.core.inject.store.ViolatesSingularDependencyException;
import hs.ddif.core.store.NoSuchKeyException;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class ProviderInjectableExtensionInjectorTest {
  private Injector injector = Injectors.manual();

  @Test
  void shouldUseProviderFromCandidate() throws Exception {
    injector.register(A.class);

    /*
     * Both A and B should be available now:
     */

    assertThat(injector.getInstance(A.class)).isInstanceOf(A.class);
    assertThat(injector.getInstance(B.class)).isInstanceOf(B.class);

    /*
     * Removal of a class that was never registered directly should fail:
     */

    assertThatThrownBy(() -> injector.remove(B.class)).isExactlyInstanceOf(NoSuchKeyException.class);

    injector.remove(A.class);

    /*
     * Both A and B should be unavailable now:
     */

    assertThatThrownBy(() -> injector.getInstance(A.class)).isExactlyInstanceOf(NoSuchInstanceException.class);
    assertThatThrownBy(() -> injector.getInstance(B.class)).isExactlyInstanceOf(NoSuchInstanceException.class);
  }

  @Test
  void shouldAllowAddAndRemoveOfProviderWhenOnlyOptionalDependencyExists() throws Exception {
    injector.register(F.class);

    assertThat(injector.getInstance(F.class).b).isNull();

    injector.register(A.class);

    assertThat(injector.getInstance(F.class).b).isNotNull();

    injector.remove(A.class);

    assertThat(injector.getInstance(F.class).b).isNull();
  }

  @Test
  void shouldRejectDuplicateProviderWhenSingularDependencyExists() throws Exception {
    injector.register(A.class);
    injector.register(C.class);  // needs B via A

    assertThatThrownBy(() -> injector.registerInstance(new B()))  // Provides B as instance
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();

    assertThatThrownBy(() -> injector.registerInstance(new D()))  // Provides B via D provider instance
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();

    assertThatThrownBy(() -> injector.register(D.class))  // Provides B via D provider
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  void shouldRejectDuplicateProviderWhenLazySingularDependencyExists() throws Exception {
    injector.register(A.class);
    injector.register(E.class);  // needs Provider<B> via A

    assertThatThrownBy(() -> injector.registerInstance(new B()))  // Provides B as instance
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();

    assertThatThrownBy(() -> injector.registerInstance(new D()))  // Provides B via D provider instance
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();

    assertThatThrownBy(() -> injector.register(D.class))  // Provides B via D provider
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  void shouldRejectInjectingNullFromProvider() throws Exception {
    Provider<B> providerInstance = new Provider<>() {
      @Override
      public B get() {
        return null;  // breaks contract to do this
      }
    };

    injector.registerInstance(providerInstance);
    injector.register(C.class);

    assertThatThrownBy(() -> injector.getInstance(C.class))
      .isExactlyInstanceOf(InstanceCreationException.class)
      .hasMessage("[class hs.ddif.core.config.ProviderInjectableExtensionInjectorTest$C] could not be created")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasMessage("No such instance: [hs.ddif.core.config.ProviderInjectableExtensionInjectorTest$B]")
      .hasNoCause();

    Provider<B> provider = injector.getInstance(Types.parameterize(Provider.class, B.class));

    /*
     * Providers are always wrapped, so requesting one directly should not
     * return the same instance:
     */

    assertThat(provider).isNotEqualTo(providerInstance);

    assertThatThrownBy(() -> provider.get())
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasMessage("No such instance: [hs.ddif.core.config.ProviderInjectableExtensionInjectorTest$B]")
      .hasNoCause();

    injector.register(E.class);

    /*
     * The "bad" provider is never injected directly, but always wrapped. This means
     * that #get will never return the contract breaking null but throw an exception instead:
     */

    E instance = injector.getInstance(E.class);

    assertThat(instance.b).isNotNull();
    assertThat(instance.b).isNotEqualTo(providerInstance);  // not same because it was wrapped
    assertThat(instance.b).isNotEqualTo(provider);  // wrapped providers are not singletons, this should be a different one

    assertThatThrownBy(() -> instance.b.get())
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasMessage("No such instance: [hs.ddif.core.config.ProviderInjectableExtensionInjectorTest$B]")
      .hasNoCause();
  }

  @Test
  void shouldAllowInjectingNullFromProviderIfOptional() throws Exception {
    injector.registerInstance(new Provider<B>() {
      @Override
      public B get() {
        return null;  // breaks contract to do this
      }
    });

    injector.register(F.class);

    F instance = injector.getInstance(F.class);

    assertThat(instance).isInstanceOf(F.class);
    assertThat(instance.b).isNull();
  }

  public static class A implements Provider<B> {
    @Override
    public B get() {
      return new B();
    }
  }

  public static class B {
  }

  public static class C {
    @Inject B b;
  }

  public static class D implements Provider<B> {
    @Override
    public B get() {
      return new B();
    }
  }

  public static class E {
    @Inject Provider<B> b;
  }

  public static class F {
    @Inject @Opt B b;
  }

}
