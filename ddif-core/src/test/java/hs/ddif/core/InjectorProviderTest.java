package hs.ddif.core;

import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.inject.consistency.ViolatesSingularDependencyException;
import hs.ddif.core.store.DuplicateInjectableException;
import hs.ddif.core.test.injectables.BeanWithProvider;
import hs.ddif.core.test.injectables.SimpleBean;
import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.test.qualifiers.Small;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.TypeReference;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InjectorProviderTest {
  private Injector injector;

  @BeforeEach
  public void beforeEach() {
    injector = new Injector();
  }

  @Test
  public void providerShouldBreakCircularDependencyAndFailWhenUsedAndWorkWhenDependencyBecomesAvailable() {
    // allowed:
    injector.register(BeanWithProvider.class);

    BeanWithProvider instance = injector.getInstance(BeanWithProvider.class);

    assertThatThrownBy(() -> instance.getSimpleBean())
      .isExactlyInstanceOf(NoSuchInstanceException.class)
      .hasNoCause();

    // works when SimpleBean registered:
    injector.register(SimpleBean.class);

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
    injector.registerInstance(Boolean.TRUE, AnnotationDescriptor.named("db.readonly"));

    for(int i = 0; i < 2; i++) {
      injector.register(DatabaseProvider.class);

      assertEquals(DatabaseProvider.class, injector.getInstance(DatabaseProvider.class).getClass());
      assertEquals(Database.class, injector.getInstance(Database.class).getClass());
      assertEquals(DatabaseProvider.class, injector.getInstance(new TypeReference<Provider<Database>>() {}.getType()).getClass());
      assertEquals(anonymousProvider.getClass(), injector.getInstance(new TypeReference<Provider<Connection>>() {}.getType()).getClass());
      assertEquals(2, injector.getInstances(Provider.class).size());
      assertEquals(1, injector.getInstances(new TypeReference<Provider<Database>>() {}.getType()).size());
      assertEquals(1, injector.getInstances(new TypeReference<Provider<Connection>>() {}.getType()).size());

      injector.remove(DatabaseProvider.class);

      assertThatThrownBy(() -> injector.getInstance(DatabaseProvider.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasNoCause();

      assertThatThrownBy(() -> injector.getInstance(Database.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasNoCause();

      assertEquals(anonymousProvider.getClass(), injector.getInstance(new TypeReference<Provider<Connection>>() {}.getType()).getClass());
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

      try {
        injector.register(SimpleDatabaseProvider.class);
        fail();
      }
      catch(DuplicateInjectableException e) {
        // expected
      }

      assertEquals(BeanWithDatabase.class, injector.getInstance(BeanWithDatabase.class).getClass());

      try {
        injector.removeInstance(provider);
        fail();
      }
      catch(ViolatesSingularDependencyException e) {
      }

      injector.remove(BeanWithDatabase.class);
      injector.removeInstance(provider);

      assertThatThrownBy(() -> injector.getInstance(BeanWithDatabase.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasNoCause();
    }
  }

  @Test
  public void classRemovalShouldFailWhenImplicitProviderRemovalWouldViolatesDependencies() {
    for(int i = 0; i < 2; i++) {
      injector.register(SimpleDatabaseProvider.class);
      injector.register(BeanWithDatabase.class);

      assertEquals(BeanWithDatabase.class, injector.getInstance(BeanWithDatabase.class).getClass());

      try {
        injector.remove(SimpleDatabaseProvider.class);
        fail();
      }
      catch(ViolatesSingularDependencyException e) {
      }

      assertEquals(BeanWithDatabase.class, injector.getInstance(BeanWithDatabase.class).getClass());

      injector.remove(BeanWithDatabase.class);
      injector.remove(SimpleDatabaseProvider.class);

      assertThatThrownBy(() -> injector.getInstance(BeanWithDatabase.class))
        .isExactlyInstanceOf(NoSuchInstanceException.class)
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
  public void getInstanceShouldReturnInjectableFromNestedProviderInstance() {
    injector.registerInstance(new NestedDatabaseProvider());

    assertNotNull(injector.getInstance(Database.class));
  }

  @Test
  public void getInstanceShouldReturnInjectableFromNestedProvider() {
    injector.register(NestedDatabaseProvider.class);

    assertNotNull(injector.getInstance(Database.class));
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsCreatedAndProvided() {
    injector.registerInstance(new Database("jdbc:localhost"));
    injector.register(BeanWithDatabase.class);

    try {
      injector.register(SimpleDatabaseProvider.class);  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      fail("Expected ViolatesSingularDependencyException");
    }
    catch(ViolatesSingularDependencyException e) {
      assertThatThrownBy(() -> injector.getInstance(SimpleDatabaseProvider.class))  // Should not be part of Injector when registration fails
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasNoCause();
    }
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsCreatedAndProvided2() {
    injector.register(SimpleDatabaseProvider.class);
    injector.register(BeanWithDatabase.class);

    assertThatThrownBy(() -> injector.registerInstance(new Database("jdbc:localhost")))  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndProvided() {
    injector.registerInstance(new Database("jdbc:localhost"));
    injector.register(BeanWithDatabase.class);

    try {
      injector.registerInstance(new SimpleDatabaseProvider());  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      fail("Expected ViolatesSingularDependencyException");
    }
    catch(ViolatesSingularDependencyException e) {
      assertThatThrownBy(() -> injector.getInstance(SimpleDatabaseProvider.class))  // Should not be part of Injector when registration fails
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasNoCause();
    }
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndProvided2() {
    injector.registerInstance(new SimpleDatabaseProvider());
    injector.register(BeanWithDatabase.class);

    assertThatThrownBy(() -> injector.registerInstance(new Database("jdbc:localhost")))  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
      .hasNoCause();
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndNestedProvided() {
    injector.registerInstance(new Database("jdbc:localhost"));
    injector.register(BeanWithDatabase.class);

    try {
      injector.registerInstance(new NestedDatabaseProvider());  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      fail("Expected ViolatesSingularDependencyException");
    }
    catch(ViolatesSingularDependencyException e) {
      assertThatThrownBy(() -> injector.getInstance(SimpleDatabaseProvider.class))  // Should not be part of Injector when registration fails
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasNoCause();
    }
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndNestedProvided2() {
    injector.registerInstance(new NestedDatabaseProvider());
    injector.register(BeanWithDatabase.class);

    assertThatThrownBy(() -> injector.registerInstance(new Database("jdbc:localhost")))  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      .isExactlyInstanceOf(ViolatesSingularDependencyException.class)
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

  @Test
  @Disabled("Providers can be used to break cyclical dependencies, and thus can always be registered...")
  public void registerShouldThrowExceptionWhenDatabaseIsProvidedAndNestedProvided() {
    injector.registerInstance(new SimpleDatabaseProvider());
    injector.register(BeanWithProvidedDatabase.class);

    try {
      injector.registerInstance(new NestedDatabaseProvider());  // Not allowed as BeanWithProvidedDatabase expects just one dependency to match, and now there are two sources for Provider<Database>
      fail("Expected ViolatesSingularDependencyException");
    }
    catch(ViolatesSingularDependencyException e) {
      assertThatThrownBy(() -> injector.getInstance(SimpleDatabaseProvider.class))  // Should not be part of Injector when registration fails
        .isExactlyInstanceOf(NoSuchInstanceException.class)
        .hasNoCause();
    }
  }

  @Test
  @Disabled("Providers can be used to break cyclical dependencies, and thus can always be registered...")
  public void registerShouldThrowExceptionWhenDatabaseIsProvidedAndNestedProvided2() {
    injector.registerInstance(new NestedDatabaseProvider());
    injector.register(BeanWithProvidedDatabase.class);

    injector.registerInstance(new SimpleDatabaseProvider());  // Not allowed as BeanWithProvidedDatabase expects just one dependency to match, and now there are two sources for Provider<Database>
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

  public static class AppendableProvider implements Provider<Appendable> {

    @Override
    public Appendable get() {
      return new Appendable() {
        @Override
        public Appendable append(CharSequence csq) throws IOException {
          return null;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) throws IOException {
          return null;
        }

        @Override
        public Appendable append(char c) throws IOException {
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
      .hasNoCause();
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
      return new Z();
    }
  }

  public static class B implements Provider<Z> {
    @Override
    @Green
    public Z get() {
      return new Z();
    }
  }

  public static class C implements Provider<Z> {
    @Override
    @Big
    @Singleton
    public Z get() {
      return new Z();
    }
  }

  public static class D implements Provider<Y> {
    @Override
    public Y get() {
      return new Y();
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

  public static class Y {
  }

  public static class Z {
  }
}
