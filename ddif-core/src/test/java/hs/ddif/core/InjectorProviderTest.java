package hs.ddif.core;

import hs.ddif.core.inject.consistency.ViolatesSingularDependencyException;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.util.AnnotationDescriptor;

import java.sql.Connection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class InjectorProviderTest {
  private Injector injector;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
    injector = new Injector();
  }

  @Test
  public void getInstanceShouldReturnInjectedProviderClass() throws BeanResolutionException {
    Provider<Connection> anonymousProvider = new Provider<Connection>() {
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
      assertEquals(DatabaseProvider.class, injector.getInstance(TypeUtils.parameterize(Provider.class, Database.class)).getClass());
      assertEquals(2, injector.getInstances(Provider.class).size());
      assertEquals(anonymousProvider.getClass(), injector.getInstance(TypeUtils.parameterize(Provider.class, Connection.class)).getClass());

      injector.remove(DatabaseProvider.class);

      try {
        injector.getInstance(DatabaseProvider.class);
        fail();
      }
      catch(BeanResolutionException e) {
      }

      try {
        injector.getInstance(Database.class);
        fail();
      }
      catch(BeanResolutionException e) {
      }

      assertEquals(anonymousProvider.getClass(), injector.getInstance(Provider.class).getClass());
    }
  }

  @Test
  public void getInstanceShouldReturnInjectableFromProviderInstance() throws BeanResolutionException {
    injector.registerInstance(new SimpleDatabaseProvider());

    assertEquals(Database.class, injector.getInstance(Database.class).getClass());
  }

  @Test
  public void classRegistrationShouldFailWhenImplicitProviderWouldViolatesDependencies() throws BeanResolutionException {
    for(int i = 0; i < 2; i++) {
      SimpleDatabaseProvider provider = new SimpleDatabaseProvider();

      injector.registerInstance(provider);
      injector.register(BeanWithDatabase.class);

      try {
        injector.register(SimpleDatabaseProvider.class);
        fail();
      }
      catch(ViolatesSingularDependencyException e) {
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

      try {
        injector.getInstance(BeanWithDatabase.class).getClass();
        fail();
      }
      catch(BeanResolutionException e) {
      }
    }
  }

  @Test
  public void classRemovalShouldFailWhenImplicitProviderRemovalWouldViolatesDependencies() throws BeanResolutionException {
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

      try {
        injector.getInstance(BeanWithDatabase.class).getClass();
        fail();
      }
      catch(BeanResolutionException e) {
      }
    }
  }

  public static class BeanWithProvidedListOfString {
    @Inject private Provider<List<String>> texts;

    List<String> getTexts() {
      return texts.get();
    }
  }

  @Test
  public void getInstanceShouldReturnInstanceInjectedWithProviderOfList() throws BeanResolutionException {
    injector.register(BeanWithProvidedListOfString.class);
    injector.registerInstance("a");
    injector.registerInstance("b");

    BeanWithProvidedListOfString bean = injector.getInstance(BeanWithProvidedListOfString.class);

    assertEquals(2, bean.getTexts().size());

    injector.registerInstance("c");

    assertEquals(3, bean.getTexts().size());
  }

  @Test
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void getInstanceShouldReturnInjectableFromNestedProviderInstance() throws BeanResolutionException {
    injector.registerInstance(new NestedDatabaseProvider());

    assertNotNull(injector.getInstance(Database.class));
  }

  @Test
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void getInstanceShouldReturnInjectableFromNestedProvider() throws BeanResolutionException {
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
      try {
        injector.getInstance(SimpleDatabaseProvider.class);  // Should not be part of Injector when registration fails
        fail("Expected NoSuchBeanException");
      }
      catch(BeanResolutionException e2) {
        // Expected
      }
    }
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void registerShouldThrowExceptionWhenDatabaseIsCreatedAndProvided2() {
    injector.register(SimpleDatabaseProvider.class);
    injector.register(BeanWithDatabase.class);

    injector.registerInstance(new Database("jdbc:localhost"));  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
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
      try {
        injector.getInstance(SimpleDatabaseProvider.class);  // Should not be part of Injector when registration fails
        fail("Expected NoSuchBeanException");
      }
      catch(BeanResolutionException e2) {
        // Expected
      }
    }
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndProvided2() {
    injector.registerInstance(new SimpleDatabaseProvider());
    injector.register(BeanWithDatabase.class);

    injector.registerInstance(new Database("jdbc:localhost"));  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
  }

  @Test
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndNestedProvided() {
    injector.registerInstance(new Database("jdbc:localhost"));
    injector.register(BeanWithDatabase.class);

    try {
      injector.registerInstance(new NestedDatabaseProvider());  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
      fail("Expected ViolatesSingularDependencyException");
    }
    catch(ViolatesSingularDependencyException e) {
      try {
        injector.getInstance(SimpleDatabaseProvider.class);  // Should not be part of Injector when registration fails
        fail("Expected NoSuchBeanException");
      }
      catch(BeanResolutionException e2) {
        // Expected
      }
    }
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndNestedProvided2() {
    injector.registerInstance(new NestedDatabaseProvider());
    injector.register(BeanWithDatabase.class);

    injector.registerInstance(new Database("jdbc:localhost"));  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
  }

  @Test
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void registerShouldThrowExceptionWhenDatabaseIsProvidedAndNestedProvided() {
    injector.registerInstance(new SimpleDatabaseProvider());
    injector.register(BeanWithProvidedDatabase.class);

    try {
      injector.registerInstance(new NestedDatabaseProvider());  // Not allowed as BeanWithProvidedDatabase expects just one dependency to match, and now there are two sources for Provider<Database>
      fail("Expected ViolatesSingularDependencyException");
    }
    catch(ViolatesSingularDependencyException e) {
      try {
        injector.getInstance(SimpleDatabaseProvider.class);  // Should not be part of Injector when registration fails
        fail("Expected NoSuchBeanException");
      }
      catch(BeanResolutionException e2) {
        // Expected
      }
    }
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  @Ignore("Nested Providers require support for Injectables with type parameters")
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
      return new Provider<Provider<Database>>() {
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
}
