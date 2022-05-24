package org.int4.dirk.plugins;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.inject.Named;

import org.int4.dirk.api.Injector;
import org.int4.dirk.api.definition.UnsatisfiedDependencyException;
import org.int4.dirk.jsr330.Injectors;
import org.int4.dirk.plugins.test.project.TestAutoDiscoverableDependency;
import org.int4.dirk.plugins.test.project.TestAutoDiscoverableInjectAnnotatedDependency;
import org.int4.dirk.plugins.test.project.TestClassWithSetterInjection;
import org.int4.dirk.plugins.test.project.TestDatabase;
import org.int4.dirk.plugins.test.project.TestStatement;
import org.int4.dirk.util.Annotations;
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

    ComponentScanner scanner = new DefaultComponentScannerFactory().create("org.int4.dirk.plugins.test.project");

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

    ComponentScanner scanner = new DefaultComponentScannerFactory().create("org.int4.dirk.plugins.test.project");

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
