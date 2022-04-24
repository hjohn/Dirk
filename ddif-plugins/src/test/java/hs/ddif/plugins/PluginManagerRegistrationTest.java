package hs.ddif.plugins;

import hs.ddif.api.Injector;
import hs.ddif.api.definition.UnsatisfiedDependencyException;
import hs.ddif.api.util.Annotations;
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
  public void shouldLoadWithAutoDiscovery() throws Exception {
    Injector injector = Injectors.autoDiscovering();

    injector.registerInstance("{jsonconfig}", CONFIGURATION);

    ComponentScanner scanner = new DefaultComponentScannerFactory().create("hs.ddif.plugins.test.project");

    scanner.scan(injector.getCandidateRegistry());

    assertNotNull(injector.getInstance(TestDatabase.class));
    assertNotNull(injector.getInstance(TestStatement.class));
    assertNotNull(injector.getInstance(TestAutoDiscoverableInjectAnnotatedDependency.class));
    assertEquals("10.0.2", injector.getInstance(String.class, VERSION));
    assertNotNull(injector.getInstance(TestClassWithSetterInjection.class));
  }

  @Test
  public void shouldLoadWithoutAutoDiscovery() throws Exception {
    Injector injector = Injectors.manual();

    injector.registerInstance("{jsonconfig}", CONFIGURATION);

    ComponentScanner scanner = new DefaultComponentScannerFactory().create("hs.ddif.plugins.test.project");

    assertThatThrownBy(() -> scanner.scan(injector.getCandidateRegistry()))
      .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
      .hasNoCause();

    injector.register(TestAutoDiscoverableDependency.class);

    scanner.scan(injector.getCandidateRegistry());

    assertNotNull(injector.getInstance(TestDatabase.class));
    assertNotNull(injector.getInstance(TestStatement.class));
    assertNotNull(injector.getInstance(TestAutoDiscoverableInjectAnnotatedDependency.class));
    assertEquals("10.0.2", injector.getInstance(String.class, VERSION));
    assertNotNull(injector.getInstance(TestClassWithSetterInjection.class));
  }
}
