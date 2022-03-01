package hs.ddif.jsr330;

import hs.ddif.annotations.Opt;
import hs.ddif.annotations.Produces;
import hs.ddif.core.Injector;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.inject.store.UnresolvableDependencyException;
import hs.ddif.core.inject.store.ViolatesSingularDependencyException;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.store.DuplicateKeyException;
import hs.ddif.core.store.FilteredKeyException;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InjectorProviderTest {
  private Injector injector = Injectors.manual();

  @Test
  public void optionalProvidersShouldBreakCircularDependenciesAndAllowDelayedRegistration() {  // Only optional Providers can be used to delay registration of the provisioned class.
    // allowed:
    injector.register(BeanWithOptionalProvider.class);

    BeanWithOptionalProvider instance = injector.getInstance(BeanWithOptionalProvider.class);

    assertThat(instance.getSimpleBean()).isNull();

    // works when SimpleBean registered:
    injector.register(SimpleBean.class);

    assertEquals(SimpleBean.class, instance.getSimpleBean().getClass());
  }

  @Test
  public void providersShouldBreakCircularDependenciesOnly() {  // Required Providers cannot be used to delay registration of the provisioned class.
    assertThatThrownBy(() -> injector.register(BeanWithProvider.class))
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasMessage("Missing dependency [hs.ddif.jsr330.InjectorProviderTest$SimpleBean] required for Field [private javax.inject.Provider<hs.ddif.jsr330.InjectorProviderTest$SimpleBean> hs.ddif.jsr330.InjectorProviderTest$BeanWithProvider.simpleBeanProvider]")
      .hasNoCause();

    injector.register(SimpleBean.class);
    injector.register(BeanWithProvider.class);

    BeanWithProvider instance = injector.getInstance(BeanWithProvider.class);

    assertEquals(SimpleBean.class, instance.getSimpleBean().getClass());
  }

  @Test
  public void getInstanceShouldReturnInjectedProviderClass() {
    Provider<Connection> anonymousProvider = new Provider<>() {
      @Override
      public Connection get() {
        return null;
      }
    };

    injector.registerInstance(anonymousProvider);
    injector.registerInstance(true, Annotations.of(Named.class, Map.of("value", "db.readonly")));

    for(int i = 0; i < 2; i++) {
      injector.register(DatabaseProvider.class);

      assertEquals(DatabaseProvider.class, injector.getInstance(DatabaseProvider.class).getClass());
      assertEquals(Database.class, injector.getInstance(Database.class).getClass());

      assertThat(injector.getInstances(Provider.class)).isEmpty();

      injector.remove(DatabaseProvider.class);

      assertThatThrownBy(() -> injector.getInstance(DatabaseProvider.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasNoCause();

      assertThatThrownBy(() -> injector.getInstance(Database.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasNoCause();
    }
  }

  @Test
  public void getInstanceShouldReturnInjectableFromProviderInstance() {
    injector.registerInstance(new SimpleDatabaseProvider());

    assertEquals(Database.class, injector.getInstance(Database.class).getClass());
  }

  @Test
  public void classRegistrationShouldFailWhenImplicitProviderWouldViolatesDependencies() {
    for(int i = 0; i < 2; i++) {
      SimpleDatabaseProvider provider = new SimpleDatabaseProvider();

      injector.registerInstance(provider);  // registers a Instance injectable but also a Provider injectable
      injector.register(BeanWithDatabase.class);

      assertThatThrownBy(() -> injector.register(SimpleDatabaseProvider.class))
        .isExactlyInstanceOf(DuplicateKeyException.class)
        .hasMessage("[hs.ddif.jsr330.InjectorProviderTest$Database] is already present")
        .hasNoCause();

      assertEquals(BeanWithDatabase.class, injector.getInstance(BeanWithDatabase.class).getClass());

      assertThatThrownBy(() -> injector.removeInstance(provider))
        .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
        .hasMessage("[hs.ddif.jsr330.InjectorProviderTest$Database] is only provided by: class hs.ddif.jsr330.InjectorProviderTest$Database")
        .hasNoCause();

      injector.remove(BeanWithDatabase.class);
      injector.removeInstance(provider);

      assertThatThrownBy(() -> injector.getInstance(BeanWithDatabase.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasMessage("No such instance: [hs.ddif.jsr330.InjectorProviderTest$BeanWithDatabase]")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasMessage("No such instance: [hs.ddif.jsr330.InjectorProviderTest$BeanWithDatabase]")
        .hasNoCause();
    }
  }

  @Test
  public void classRemovalShouldFailWhenImplicitProviderRemovalWouldViolatesDependencies() {
    for(int i = 0; i < 2; i++) {
      injector.register(SimpleDatabaseProvider.class);
      injector.register(BeanWithDatabase.class);

      assertEquals(BeanWithDatabase.class, injector.getInstance(BeanWithDatabase.class).getClass());
      assertThatThrownBy(() -> injector.remove(SimpleDatabaseProvider.class))
        .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
        .hasMessage("[hs.ddif.jsr330.InjectorProviderTest$Database] is only provided by: class hs.ddif.jsr330.InjectorProviderTest$Database")
        .hasNoCause();

      assertEquals(BeanWithDatabase.class, injector.getInstance(BeanWithDatabase.class).getClass());

      injector.remove(BeanWithDatabase.class);
      injector.remove(SimpleDatabaseProvider.class);

      assertThatThrownBy(() -> injector.getInstance(BeanWithDatabase.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasMessage("No such instance: [hs.ddif.jsr330.InjectorProviderTest$BeanWithDatabase]")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isExactlyInstanceOf(NoSuchInstance.class)
        .hasMessage("No such instance: [hs.ddif.jsr330.InjectorProviderTest$BeanWithDatabase]")
        .hasNoCause();
    }
  }

  public static class BeanWithProvidedListOfString {
    @Inject private Provider<List<String>> texts;

    List<String> getTexts() {
      return texts.get();
    }
  }

  @Test
  public void getInstanceShouldReturnInstanceInjectedWithProviderOfList() {
    injector.register(BeanWithProvidedListOfString.class);
    injector.registerInstance("a");
    injector.registerInstance("b");

    BeanWithProvidedListOfString bean = injector.getInstance(BeanWithProvidedListOfString.class);

    assertEquals(2, bean.getTexts().size());

    injector.registerInstance("c");

    assertEquals(3, bean.getTexts().size());
  }

  @Test
  public void nestedProvidersShouldNotBeAllowed() {
    assertThatThrownBy(() -> injector.registerInstance(new NestedDatabaseProvider()))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[Injectable[javax.inject.Provider<javax.inject.Provider<hs.ddif.jsr330.InjectorProviderTest$Database>> <- public javax.inject.Provider hs.ddif.jsr330.InjectorProviderTest$NestedDatabaseProvider.get()]] cannot be registered as its type conflicts with a TypeExtension for interface javax.inject.Provider")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(FilteredKeyException.class)
      .hasMessage("[javax.inject.Provider<javax.inject.Provider<hs.ddif.jsr330.InjectorProviderTest$Database>>] cannot be added to or removed from the store as it matched the class filter")
      .hasNoCause();
  }

  @Test
  public void producerFieldProducingProviderShouldNotBeAllowed() {
    assertThatThrownBy(() -> injector.register(ProviderFieldProducer.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[Injectable[javax.inject.Provider<hs.ddif.jsr330.InjectorProviderTest$Database> <- private static javax.inject.Provider hs.ddif.jsr330.InjectorProviderTest$ProviderFieldProducer.product]] cannot be registered as its type conflicts with a TypeExtension for interface javax.inject.Provider")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(FilteredKeyException.class)
      .hasMessage("[javax.inject.Provider<hs.ddif.jsr330.InjectorProviderTest$Database>] cannot be added to or removed from the store as it matched the class filter")
      .hasNoCause();
  }

  @Test
  public void producerMethodProducingProviderShouldNotBeAllowed() {
    assertThatThrownBy(() -> injector.register(ProviderMethodProducer.class))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[Injectable[javax.inject.Provider<hs.ddif.jsr330.InjectorProviderTest$Database> <- private static javax.inject.Provider hs.ddif.jsr330.InjectorProviderTest$ProviderMethodProducer.product()]] cannot be registered as its type conflicts with a TypeExtension for interface javax.inject.Provider")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(FilteredKeyException.class)
      .hasMessage("[javax.inject.Provider<hs.ddif.jsr330.InjectorProviderTest$Database>] cannot be added to or removed from the store as it matched the class filter")
      .hasNoCause();
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsCreatedAndProvided() {
    injector.registerInstance(new Database("jdbc:localhost"));
    injector.register(BeanWithDatabase.class);

    assertThatThrownBy(() -> injector.register(SimpleDatabaseProvider.class))  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasMessage("[hs.ddif.jsr330.InjectorProviderTest$Database] would be provided again by: class hs.ddif.jsr330.InjectorProviderTest$Database")
      .hasNoCause();

    assertThatThrownBy(() -> injector.getInstance(SimpleDatabaseProvider.class))  // Should not be part of Injector when registration fails
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasMessage("No such instance: [hs.ddif.jsr330.InjectorProviderTest$SimpleDatabaseProvider]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NoSuchInstance.class)
      .hasMessage("No such instance: [hs.ddif.jsr330.InjectorProviderTest$SimpleDatabaseProvider]")
      .hasNoCause();
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsCreatedAndProvided2() {
    injector.register(SimpleDatabaseProvider.class);
    injector.register(BeanWithDatabase.class);

    assertThatThrownBy(() -> injector.registerInstance(new Database("jdbc:localhost")))  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasMessage("[hs.ddif.jsr330.InjectorProviderTest$Database] would be provided again by: class hs.ddif.jsr330.InjectorProviderTest$Database")
      .hasNoCause();
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndProvided() {
    injector.registerInstance(new Database("jdbc:localhost"));
    injector.register(BeanWithDatabase.class);

    assertThatThrownBy(() -> injector.registerInstance(new SimpleDatabaseProvider()))  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasMessage("[hs.ddif.jsr330.InjectorProviderTest$Database] would be provided again by: class hs.ddif.jsr330.InjectorProviderTest$Database")
      .hasNoCause();

    assertThatThrownBy(() -> injector.getInstance(SimpleDatabaseProvider.class))  // Should not be part of Injector when registration fails
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasMessage("No such instance: [hs.ddif.jsr330.InjectorProviderTest$SimpleDatabaseProvider]")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NoSuchInstance.class)
      .hasMessage("No such instance: [hs.ddif.jsr330.InjectorProviderTest$SimpleDatabaseProvider]")
      .hasNoCause();
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndProvided2() {
    injector.registerInstance(new SimpleDatabaseProvider());
    injector.register(BeanWithDatabase.class);

    assertThatThrownBy(() -> injector.registerInstance(new Database("jdbc:localhost")))  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasMessage("[hs.ddif.jsr330.InjectorProviderTest$Database] would be provided again by: class hs.ddif.jsr330.InjectorProviderTest$Database")
      .hasNoCause();
  }

  @Test
  public void registrationAndUnregistrationOfInterfaceProviderShouldWork() {
    // Tests specifically if registering and unregistering a Provider, that provides an interface instead of
    // of a concrete class, works.
    for(int i = 0; i < 2; i++) {
      injector.register(AppendableProvider.class);

      assertTrue(injector.contains(AppendableProvider.class));
      assertNotNull(injector.getInstance(AppendableProvider.class));
      assertNotNull(injector.getInstance(Appendable.class));

      injector.remove(AppendableProvider.class);

      assertFalse(injector.contains(AppendableProvider.class));
      assertThrows(NoSuchInstanceException.class, () -> injector.getInstance(AppendableProvider.class));
      assertThrows(NoSuchInstanceException.class, () -> injector.getInstance(Appendable.class));
    }
  }

  public static class BeanWithDatabase {
    @Inject Database database;
  }

  public static class BeanWithProvidedDatabase {
    @Inject Provider<Database> database;
  }

  public static class Database {
    public Database(@SuppressWarnings("unused") String url) {
    }
  }

  public static class NestedDatabaseProvider implements Provider<Provider<Provider<Database>>> {

    @Override
    public Provider<Provider<Database>> get() {
      return new Provider<>() {
        @Override
        public Provider<Database> get() {
          return new SimpleDatabaseProvider();
        }
      };
    }
  }

  public static class SimpleDatabaseProvider implements Provider<Database> {
    @Override
    public Database get() {
      return new Database("jdbc:localhost");
    }
  }

  public static class DatabaseProvider implements Provider<Database> {
    @SuppressWarnings("unused")
    @Inject
    public DatabaseProvider(Provider<Connection> cls, @Named("db.readonly") boolean readOnly) {
    }

    @Override
    public Database get() {
      return new Database("jdbc:localhost");
    }
  }

  public static class ProviderFieldProducer {
    @Produces private static Provider<Database> product;
  }

  public static class ProviderMethodProducer {
    @Produces private static Provider<Database> product() {
      return null;
    }
  }

  public static class AppendableProvider implements Provider<Appendable> {

    @Override
    public Appendable get() {
      return new Appendable() {
        @Override
        public Appendable append(CharSequence csq) {
          return null;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
          return null;
        }

        @Override
        public Appendable append(char c) {
          return null;
        }
      };
    }
  }

  @Test
  public void providersShouldRespectQualifiers() {
    injector.register(A.class);
    injector.register(B.class);
    injector.register(C.class);

    assertThat(injector.getInstances(Z.class)).hasSize(3);
    assertThat(injector.getInstance(Z.class, Red.class)).isExactlyInstanceOf(Z.class);
    assertThat(injector.getInstance(Z.class, Green.class)).isExactlyInstanceOf(Z.class);
    assertThat(injector.getInstance(Z.class, Big.class)).isExactlyInstanceOf(Z.class);
    assertThatThrownBy(() -> injector.getInstance(Z.class, Small.class))
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(NoSuchInstance.class)
      .hasNoCause();

    injector.register(X.class);

    X x = injector.getInstance(X.class);

    /*
     * Note, the below tests for the red and green fields could test if the injected
     * field is an instance of A or B.  However, the injector has trouble locating
     * these in the store as it is not (currently) possible to locate a Provider
     * where its supplied type must have certain qualifiers (@Red/@Green).
     *
     * So the injector instead creates a custom Provider which, indirectly, still
     * obtains the target type via the "get" methods of the registered Providers
     * A and B. This extra provider wrapper is unnecessary, but should not cause
     * inconsistencies.
     *
     * In the future it might be possible to instead locate the supplied type
     * directly, then have a Matcher filter out the correct match. This would
     * require Matcher to accept Injectable and Injectable to expose its owner type.
     * In this way it is possible to search for a method Injectable for a "get"
     * method and via the owner type it can be checked if it implements Provider.
     */

    assertThat(x.red).isNotNull();
    assertThat(x.green).isNotNull();

    assertThat(x.red.get().name).isEqualTo("red");
    assertThat(x.green.get().name).isEqualTo("green");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void multipleProvidersOfSameTypeShouldReturnMultipleInstances() {
    injector.register(B.class);  // provides @Green Z("green")
    injector.register(E.class);  // provides @Green Z("light green")

    assertThat(injector.getInstances(Z.class)).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");

    injector.register(W.class);
    W w = injector.getInstance(W.class);

    assertThat(w.zs).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
    assertThat(w.zList.get()).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");

    assertThat(w.reds).isEmpty();
    assertThat(w.zReds).isEmpty();
    assertThat(w.redList.get()).isEmpty();
    assertThat(w.zRedList.get()).isEmpty();

    assertThat((List<Z>)w.greens).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
    assertThat(w.zGreens).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
    assertThat((List<Z>)w.greenList.get()).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
    assertThat(w.zGreenList.get()).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void multipleProducersOfSameTypeShouldReturnMultipleInstances() {
    injector.register(V.class);  // produces @Green Z("green") and @Green Z("light green")

    assertThat(injector.getInstances(Z.class)).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");

    injector.register(W.class);
    W w = injector.getInstance(W.class);

    assertThat(w.zs).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
    assertThat(w.zList.get()).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");

    assertThat(w.reds).isEmpty();
    assertThat(w.zReds).isEmpty();
    assertThat(w.redList.get()).isEmpty();
    assertThat(w.zRedList.get()).isEmpty();

    assertThat((List<Z>)w.greens).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
    assertThat(w.zGreens).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
    assertThat((List<Z>)w.greenList.get()).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
    assertThat(w.zGreenList.get()).extracting(z -> z.name).containsExactlyInAnyOrder("green", "light green");
  }

  @Test
  public void providerShouldRespectScope() {
    injector.register(B.class);  // provides non-singleton Green Z
    injector.register(C.class);  // provides singleton Big Z
    injector.register(D.class);  // provider non-singleton Y
    injector.register(P.class);
    injector.register(Q.class);
    injector.register(R.class);

    P p1 = injector.getInstance(P.class);
    Q q1 = injector.getInstance(Q.class);
    R r1 = injector.getInstance(R.class);
    P p2 = injector.getInstance(P.class);
    Q q2 = injector.getInstance(Q.class);
    R r2 = injector.getInstance(R.class);

    assertThat(p1.y).isNotEqualTo(q1.y);
    assertThat(p1.y).isNotEqualTo(r1.y);
    assertThat(q1.y).isNotEqualTo(r1.y);
    assertThat(p1.y).isNotEqualTo(p2.y);
    assertThat(q1.y).isNotEqualTo(q2.y);
    assertThat(r1.y).isNotEqualTo(r2.y);

    assertThat(p1.z).isEqualTo(q1.z);
    assertThat(p1.z).isNotEqualTo(r1.z);
    assertThat(q1.z).isNotEqualTo(r1.z);
    assertThat(p1.z).isEqualTo(p2.z);
    assertThat(q1.z).isEqualTo(q2.z);
    assertThat(r1.z).isNotEqualTo(r2.z);
  }

  public static class A implements Provider<Z> {
    @Override
    @Red
    public Z get() {
      return new Z("red");
    }
  }

  public static class B implements Provider<Z> {
    @Override
    @Green
    public Z get() {
      return new Z("green");
    }
  }

  @Singleton
  public static class C implements Provider<Z> {
    @Override
    @Big
    @Singleton
    public Z get() {
      return new Z("big");
    }
  }

  public static class D implements Provider<Y> {
    @Override
    public Y get() {
      return new Y();
    }
  }

  public static class E implements Provider<Z> {
    @Override
    @Green
    public Z get() {
      return new Z("light green");
    }
  }

  public static class P {
    @Inject Y y;
    @Inject @Big Z z;
  }

  public static class Q {
    @Inject Y y;
    @Inject @Big Z z;
  }

  public static class R {
    @Inject Y y;
    @Inject @Green Z z;
  }

  public static class V {
    @Produces @Green Z z1 = new Z("green");
    @Produces @Green Z z2 = new Z("light green");
  }

  public static class W {
    @Inject List<Z> zs;
    @Inject Provider<List<Z>> zList;
    @Inject @Green List<?> greens;
    @Inject @Green List<Z> zGreens;
    @Inject @Green Provider<List<?>> greenList;
    @Inject @Green Provider<List<Z>> zGreenList;
    @Inject @Red List<?> reds;
    @Inject @Red List<Z> zReds;
    @Inject @Red Provider<List<?>> redList;
    @Inject @Red Provider<List<Z>> zRedList;
  }

  public static class X {
    @Inject @Red Provider<Z> red;
    @Inject @Green Provider<Z> green;
  }

  public static class Y {
  }

  public static class Z {
    public final String name;

    public Z(String name) {
      this.name = name;
    }
  }

  @Nested
  class WhenInjectorContainsPrimitiveValues {
    {
      injector.registerInstance(7);
      injector.registerInstance(2.5);
      injector.registerInstance(true);
      injector.registerInstance((byte)11);
    }

    @Test
    void shouldInjectProvidersWithCorrespondingPrimitives() {
      injector.register(PrimitivesA.class);

      assertThat(injector.getInstance(PrimitivesA.class).calculate()).isEqualTo(23.0);
    }
  }

  static class PrimitivesA {
    @Inject Provider<Integer> i;
    @Inject Provider<Double> d;

    private Provider<Byte> b;
    private Provider<Boolean> flag;

    @Inject
    PrimitivesA(Provider<Byte> b, Provider<Boolean> flag) {
      this.b = b;
      this.flag = flag;
    }

    double calculate() {
      return d.get() * i.get() + b.get() / (flag.get() ? 2.0 : 1.5);
    }
  }

  public static class BeanWithOptionalProvider {
    @Inject @Opt
    private Provider<SimpleBean> simpleBeanProvider;

    public SimpleBean getSimpleBean() {
      return simpleBeanProvider.get();
    }
  }

  @Singleton
  public static class SimpleBean {
  }

  public static class BeanWithProvider {
    @Inject
    private Provider<SimpleBean> simpleBeanProvider;

    public SimpleBean getSimpleBean() {
      return simpleBeanProvider.get();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Red {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Green {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Big {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Small {
  }
}