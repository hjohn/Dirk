package hs.ddif.core;

import hs.ddif.annotations.Parameter;
import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.InstantiationException;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
      assertThatThrownBy(() -> injector.getParameterizedInstance(TestParameterizedFieldSample.class, new NamedParameter[] {new NamedParameter("wrongName", 30)}))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.ParameterizedInjectionTest$TestParameterizedFieldSample")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InstantiationException.class)
        .hasMessage("Exception while injecting: java.lang.Integer hs.ddif.core.ParameterizedInjectionTest$TestParameterizedFieldSample.interval")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("Parameter 'interval' was not supplied");

      // Too many parameters case:
      assertThatThrownBy(() -> injector.getParameterizedInstance(TestParameterizedFieldSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("interval", 30)}))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.ParameterizedInjectionTest$TestParameterizedFieldSample")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InstantiationException.class)
        .hasMessage("Superflous parameters supplied, expected 1 but got: 2: class hs.ddif.core.ParameterizedInjectionTest$TestParameterizedFieldSample");

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
      assertThatThrownBy(() -> injector.getParameterizedInstance(TestParameterizedConstructorSample.class, new NamedParameter[] {new NamedParameter("wrongName", 30)}))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.ParameterizedInjectionTest$TestParameterizedConstructorSample")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InstantiationException.class)
        .hasMessage("Exception while constructing instance: public hs.ddif.core.ParameterizedInjectionTest$TestParameterizedConstructorSample(hs.ddif.core.ParameterizedInjectionTest$TestService,java.lang.Integer,java.lang.String)")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("Parameter 'interval' was not supplied");

      // Too many parameters case:
      assertThatThrownBy(() -> injector.getParameterizedInstance(TestParameterizedConstructorSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc"), new NamedParameter("postfix", "xyz")}))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.ParameterizedInjectionTest$TestParameterizedConstructorSample")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InstantiationException.class)
        .hasMessage("Superflous parameters supplied, expected 2 but got: 3: class hs.ddif.core.ParameterizedInjectionTest$TestParameterizedConstructorSample");

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
      assertThatThrownBy(() -> injector.getParameterizedInstance(TestParameterizedMixedSample.class, new NamedParameter[] {new NamedParameter("wrongName", 30)}))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.ParameterizedInjectionTest$TestParameterizedMixedSample")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InstantiationException.class)
        .hasMessage("Exception while constructing instance: public hs.ddif.core.ParameterizedInjectionTest$TestParameterizedMixedSample(hs.ddif.core.ParameterizedInjectionTest$TestService,java.lang.Integer)")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("Parameter 'interval' was not supplied");

      // Too many parameters case:
      assertThatThrownBy(() -> injector.getParameterizedInstance(TestParameterizedMixedSample.class, new NamedParameter[] {new NamedParameter("interval", 30), new NamedParameter("prefix", "abc"), new NamedParameter("postfix", "xyz")}))
        .isInstanceOf(BeanResolutionException.class)
        .hasMessage("No such bean: class hs.ddif.core.ParameterizedInjectionTest$TestParameterizedMixedSample")
        .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(InstantiationException.class)
        .hasMessage("Superflous parameters supplied, expected 2 but got: 3: class hs.ddif.core.ParameterizedInjectionTest$TestParameterizedMixedSample");

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