package hs.ddif.plugins;

import hs.ddif.core.Injector;
import hs.ddif.core.inject.store.UnresolvableDependencyException;
import hs.ddif.core.util.Annotations;
import hs.ddif.jsr330.Injectors;
import hs.ddif.plugins.test.project.TestAutoDiscoverableDependency;
import hs.ddif.plugins.test.project.TestAutoDiscoverableInjectAnnotatedDependency;
import hs.ddif.plugins.test.project.TestClassWithSetterInjection;
import hs.ddif.plugins.test.project.TestDatabase;
import hs.ddif.plugins.test.project.TestStatement;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PluginManagerRegistrationTest {
  private static final Annotation CONFIGURATION = Annotations.of(Named.class, Map.of("value", "configuration"));
  private static final Annotation VERSION = Annotations.of(Named.class, Map.of("value", "version"));

  @Test
  public void shouldLoadWithAutoDiscovery() {
    Injector injector = Injectors.autoDiscovering();

    injector.registerInstance("{jsonconfig}", CONFIGURATION);

    ComponentScanner.scan(injector.getCandidateRegistry(), "hs.ddif.plugins.test.project");

    assertNotNull(injector.getInstance(TestDatabase.class));
    assertNotNull(injector.getInstance(TestStatement.class));
    assertNotNull(injector.getInstance(TestAutoDiscoverableInjectAnnotatedDependency.class));
    assertEquals("10.0.2", injector.getInstance(String.class, VERSION));
    assertNotNull(injector.getInstance(TestClassWithSetterInjection.class));
  }

  @Test
  public void shouldLoadWithoutAutoDiscovery() {
    Injector injector = Injectors.manual();

    injector.registerInstance("{jsonconfig}", CONFIGURATION);

    assertThatThrownBy(() -> ComponentScanner.scan(injector.getCandidateRegistry(), "hs.ddif.plugins.test.project"))
      .isExactlyInstanceOf(UnresolvableDependencyException.class)
      .hasNoCause();

    injector.register(TestAutoDiscoverableDependency.class);

    ComponentScanner.scan(injector.getCandidateRegistry(), "hs.ddif.plugins.test.project");

    assertNotNull(injector.getInstance(TestDatabase.class));
    assertNotNull(injector.getInstance(TestStatement.class));
    assertNotNull(injector.getInstance(TestAutoDiscoverableInjectAnnotatedDependency.class));
    assertEquals("10.0.2", injector.getInstance(String.class, VERSION));
    assertNotNull(injector.getInstance(TestClassWithSetterInjection.class));
  }
}
