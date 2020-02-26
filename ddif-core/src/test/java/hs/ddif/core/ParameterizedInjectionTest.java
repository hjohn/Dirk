package hs.ddif.core;

import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.bind.Parameter;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.store.ConstructionException;

import javax.inject.Inject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ParameterizedInjectionTest {
  private Injector injector = new Injector();

  @Test
  public void registerShouldAcceptParameterizedClass() throws BeanResolutionException {
    for(int i = 0; i < 2; i++) {
      injector.register(TestService.class);
      injector.register(TestParameterizedFieldSample.class);

      // Normal case:
      TestParameterizedFieldSample instance = injector.getParameterizedInstance(TestParameterizedFieldSample.class, new NamedParameter[] {new NamedParameter("interval", 30)});

      assertEquals(TestParameterizedFieldSample.class, instance.getClass());
      assertEquals((Integer)30, instance.interval);

      // Incorrect parameter case:
      try {
        injector.getParameterizedInstance(TestParameterizedFieldSample.class, new NamedParameter[] {new NamedParameter("wrongName", 30)});
        fail("Expected ConstructionException");
      }
      catch(ConstructionException e) {
        assertTrue(e.getMessage(), e.getMessage().contains("Parameter 'interval' was not supplied"));
      }

      // Too many parameters case:
      try {
        injector.getParameterizedInstance(TestParameterizedFieldSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("interval", 30)});
        fail("Expected ConstructionException");
      }
      catch(ConstructionException e) {
        assertTrue(e.getMessage(), e.getMessage().contains("Superflous parameters supplied"));
      }

      injector.remove(TestParameterizedFieldSample.class);
      injector.remove(TestService.class);
    }
  }

  public static class TestParameterizedFieldSample {
    @Inject TestService service;
    @Inject @Parameter Integer interval;
  }

  @Test
  public void registerShouldAcceptParameterizedConstructorClass() throws BeanResolutionException {
    for(int i = 0; i < 2; i++) {
      injector.register(TestService.class);
      injector.register(TestParameterizedConstructorSample.class);

      // Normal case:
      TestParameterizedConstructorSample instance = injector.getParameterizedInstance(TestParameterizedConstructorSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc")});

      assertEquals(TestParameterizedConstructorSample.class, instance.getClass());
      assertEquals((Integer)30, instance.interval);
      assertEquals("abc", instance.prefix);

      // Incorrect parameter case:
      try {
        injector.getParameterizedInstance(TestParameterizedConstructorSample.class, new NamedParameter[] {new NamedParameter("wrongName", 30)});
        fail("Expected ConstructionException");
      }
      catch(ConstructionException e) {
        assertTrue(e.getMessage(), e.getMessage().contains("Parameter 'interval' was not supplied"));
      }

      // Too many parameters case:
      try {
        injector.getParameterizedInstance(TestParameterizedConstructorSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc"), new NamedParameter("postfix", "xyz")});
        fail("Expected ConstructionException");
      }
      catch(ConstructionException e) {
        assertTrue(e.getMessage(), e.getMessage().contains("Superflous parameters supplied"));
      }

      injector.remove(TestParameterizedConstructorSample.class);
      injector.remove(TestService.class);
    }
  }

  public static class TestParameterizedConstructorSample {
    TestService testService;
    Integer interval;
    String prefix;

    @Inject
    public TestParameterizedConstructorSample(TestService testService, @Parameter("interval") Integer interval, @Parameter("prefix") String prefix) {
      this.testService = testService;
      this.interval = interval;
      this.prefix = prefix;
    }
  }

  @Test
  public void registerShouldAcceptParameterizedMixedClass() throws BeanResolutionException {
    for(int i = 0; i < 2; i++) {
      injector.register(TestService.class);
      injector.register(TestParameterizedMixedSample.class);

      // Normal case:
      TestParameterizedMixedSample instance = injector.getParameterizedInstance(TestParameterizedMixedSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc")});

      assertEquals(TestParameterizedMixedSample.class, instance.getClass());
      assertEquals((Integer)30, instance.interval);
      assertEquals("abc", instance.prefix);

      // Incorrect parameter case:
      try {
        injector.getParameterizedInstance(TestParameterizedMixedSample.class, new NamedParameter[] {new NamedParameter("wrongName", 30)});
        fail("Expected ConstructionException");
      }
      catch(ConstructionException e) {
        assertTrue(e.getMessage(), e.getMessage().contains("Parameter 'interval' was not supplied"));
      }

      // Too many parameters case:
      try {
        injector.getParameterizedInstance(TestParameterizedMixedSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc"), new NamedParameter("postfix", "xyz")});
        fail("Expected ConstructionException");
      }
      catch(ConstructionException e) {
        assertTrue(e.getMessage(), e.getMessage().contains("Superflous parameters supplied"));
      }

      injector.remove(TestParameterizedMixedSample.class);
      injector.remove(TestService.class);
    }
  }

  public static class TestParameterizedMixedSample {
    TestService testService;
    Integer interval;
    @Inject @Parameter String prefix;

    @Inject
    public TestParameterizedMixedSample(TestService testService, @Parameter("interval") Integer interval) {
      this.testService = testService;
      this.interval = interval;
    }
  }

  @Test
  public void registerShouldNotCheckParameterTypesForSingularDependencyViolations() throws BeanResolutionException {
    injector.register(TestService.class);
    injector.register(TestParameterizedFieldSample.class);

    // Normal case:
    TestParameterizedFieldSample instance = injector.getParameterizedInstance(TestParameterizedFieldSample.class, new NamedParameter[] {new NamedParameter("interval", 30)});

    assertEquals(TestParameterizedFieldSample.class, instance.getClass());
    assertEquals((Integer)30, instance.interval);

    injector.registerInstance(Integer.valueOf(5));  // Should be fine, even though an Integer is need as parameter
    injector.registerInstance(Integer.valueOf(6));  // Should still be fine, as nothing requires a single Integer
  }

  public static class TestService {
  }
}
