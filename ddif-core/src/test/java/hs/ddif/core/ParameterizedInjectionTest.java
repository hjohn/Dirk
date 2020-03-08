package hs.ddif.core;

import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.bind.Parameter;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.store.ConstructionException;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParameterizedInjectionTest {
  private Injector injector = new Injector();

  @Test
  void registerShouldAcceptParameterizedClass() throws BeanResolutionException {
    for(int i = 0; i < 2; i++) {
      injector.register(TestService.class);
      injector.register(TestParameterizedFieldSample.class);

      // Normal case:
      TestParameterizedFieldSample instance = injector.getParameterizedInstance(TestParameterizedFieldSample.class, new NamedParameter[] {new NamedParameter("interval", 30)});

      assertEquals(TestParameterizedFieldSample.class, instance.getClass());
      assertEquals((Integer)30, instance.interval);

      // Incorrect parameter case:
      assertThat(assertThrows(ConstructionException.class, () -> injector.getParameterizedInstance(TestParameterizedFieldSample.class, new NamedParameter[] {new NamedParameter("wrongName", 30)})))
        .hasMessageStartingWith("Parameter 'interval' was not supplied");

      // Too many parameters case:
      assertThat(assertThrows(ConstructionException.class, () -> injector.getParameterizedInstance(TestParameterizedFieldSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("interval", 30)})))
        .hasMessageStartingWith("Superflous parameters supplied");

      injector.remove(TestParameterizedFieldSample.class);
      injector.remove(TestService.class);
    }
  }

  public static class TestParameterizedFieldSample {
    @Inject TestService service;
    @Inject @Parameter Integer interval;
  }

  @Test
  void registerShouldAcceptParameterizedConstructorClass() throws BeanResolutionException {
    for(int i = 0; i < 2; i++) {
      injector.register(TestService.class);
      injector.register(TestParameterizedConstructorSample.class);

      // Normal case:
      TestParameterizedConstructorSample instance = injector.getParameterizedInstance(TestParameterizedConstructorSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc")});

      assertEquals(TestParameterizedConstructorSample.class, instance.getClass());
      assertEquals((Integer)30, instance.interval);
      assertEquals("abc", instance.prefix);

      // Incorrect parameter case:
      assertThat(assertThrows(ConstructionException.class, () -> injector.getParameterizedInstance(TestParameterizedConstructorSample.class, new NamedParameter[] {new NamedParameter("wrongName", 30)})))
        .hasMessageStartingWith("Parameter 'interval' was not supplied");

      // Too many parameters case:
      assertThat(assertThrows(ConstructionException.class, () -> injector.getParameterizedInstance(TestParameterizedConstructorSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc"), new NamedParameter("postfix", "xyz")})))
        .hasMessageStartingWith("Superflous parameters supplied");

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
  void registerShouldAcceptParameterizedMixedClass() throws BeanResolutionException {
    for(int i = 0; i < 2; i++) {
      injector.register(TestService.class);
      injector.register(TestParameterizedMixedSample.class);

      // Normal case:
      TestParameterizedMixedSample instance = injector.getParameterizedInstance(TestParameterizedMixedSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc")});

      assertEquals(TestParameterizedMixedSample.class, instance.getClass());
      assertEquals((Integer)30, instance.interval);
      assertEquals("abc", instance.prefix);

      // Incorrect parameter case:
      assertThat(assertThrows(ConstructionException.class, () -> injector.getParameterizedInstance(TestParameterizedMixedSample.class, new NamedParameter[] {new NamedParameter("wrongName", 30)})))
        .hasMessageStartingWith("Parameter 'interval' was not supplied");

      // Too many parameters case:
      assertThat(assertThrows(ConstructionException.class, () -> injector.getParameterizedInstance(TestParameterizedMixedSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc"), new NamedParameter("postfix", "xyz")})))
        .hasMessageStartingWith("Superflous parameters supplied");

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
  void registerShouldNotCheckParameterTypesForSingularDependencyViolations() throws BeanResolutionException {
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
