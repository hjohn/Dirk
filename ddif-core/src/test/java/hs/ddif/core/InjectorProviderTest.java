package hs.ddif.core;

import java.sql.Connection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

  private void setupDatabaseProvider() {
    injector.register(new Provider<Connection>() {
      @Override
      public Connection get() {
        return null;
      }
    });
    injector.registerInstance(Boolean.TRUE, AnnotationDescriptor.describe(Named.class, new Value("value", "db.readonly")));
    injector.register(DatabaseProvider.class);
  }

  @Test
  public void getInstanceShouldReturnInjectedProviderClass() {
    setupDatabaseProvider();

    assertNotNull(injector.getInstance(DatabaseProvider.class));
  }

  @Test
  public void getInstanceShouldReturnInjectableFromProviderClass() {
    setupDatabaseProvider();

    assertNotNull(injector.getInstance(Database.class));
  }

  @Test
  public void getInstanceShouldReturnInjectableFromProviderInstance() {
    injector.registerInstance(new SimpleDatabaseProvider());

    assertNotNull(injector.getInstance(Database.class));
  }

  @Test
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void getInstanceShouldReturnInjectableFromNestedProviderInstance() {
    injector.registerInstance(new NestedDatabaseProvider());

    assertNotNull(injector.getInstance(Database.class));
  }

  @Test
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void getInstanceShouldReturnInjectableFromNestedProvider() {
    injector.register(NestedDatabaseProvider.class);

    assertNotNull(injector.getInstance(Database.class));
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsCreatedAndProvided() {
    injector.registerInstance(new Database());
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
      catch(NoSuchBeanException e2) {
        // Expected
      }
    }
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void registerShouldThrowExceptionWhenDatabaseIsCreatedAndProvided2() {
    injector.register(SimpleDatabaseProvider.class);
    injector.register(BeanWithDatabase.class);

    injector.registerInstance(new Database());  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
  }

  @Test
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndProvided() {
    injector.registerInstance(new Database());
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
      catch(NoSuchBeanException e2) {
        // Expected
      }
    }
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndProvided2() {
    injector.registerInstance(new SimpleDatabaseProvider());
    injector.register(BeanWithDatabase.class);

    injector.registerInstance(new Database());  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
  }

  @Test
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndNestedProvided() {
    injector.registerInstance(new Database());
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
      catch(NoSuchBeanException e2) {
        // Expected
      }
    }
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void registerShouldThrowExceptionWhenDatabaseIsInstancedAndNestedProvided2() {
    injector.registerInstance(new NestedDatabaseProvider());
    injector.register(BeanWithDatabase.class);

    injector.registerInstance(new Database());  // Not allowed as BeanWithDatabase expects just one dependency to match, and now there are two sources for Database
  }

  @Test
  @Ignore("Nested Providers require support for Injectables with type parameters")
  public void registerShouldThrowExceptionWhenDatabaseIsProvidedAndNestedProvided() {
    injector.register(new SimpleDatabaseProvider());
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
      catch(NoSuchBeanException e2) {
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
      return new Database();
    }
  }

  public static class DatabaseProvider implements Provider<Database> {
    @SuppressWarnings("unused")
    @Inject
    public DatabaseProvider(Provider<Connection> cls, @Named("db.readonly") boolean readOnly) {
    }

    @Override
    public Database get() {
      return new Database();
    }
  }

}
