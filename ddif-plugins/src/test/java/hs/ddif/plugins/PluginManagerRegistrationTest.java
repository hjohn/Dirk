package hs.ddif.plugins;

import hs.ddif.core.Injector;
import hs.ddif.core.inject.consistency.UnresolvableDependencyException;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.plugins.test.project.TestAutoDiscoverableDependency;
import hs.ddif.plugins.test.project.TestAutoDiscoverableInjectAnnotatedDependency;
import hs.ddif.plugins.test.project.TestClassWithSetterInjection;
import hs.ddif.plugins.test.project.TestDatabase;
import hs.ddif.plugins.test.project.TestStatement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PluginManagerRegistrationTest {

  @Test
  public void shouldLoadWithAutoDiscovery() throws BeanResolutionException {
    Injector injector = new Injector(true);

    injector.registerInstance("{jsonconfig}", AnnotationDescriptor.named("configuration"));

    ComponentScanner.scan(injector.getCandidateRegistry(), "hs.ddif.plugins.test.project");

    assertNotNull(injector.getInstance(TestDatabase.class));
    assertNotNull(injector.getInstance(TestStatement.class));
    assertNotNull(injector.getInstance(TestAutoDiscoverableInjectAnnotatedDependency.class));
    assertEquals("10.0.2", injector.getInstance(String.class, AnnotationDescriptor.named("version")));
    assertNotNull(injector.getInstance(TestClassWithSetterInjection.class));
  }

  @Test
  public void shouldLoadWithoutAutoDiscovery() throws BeanResolutionException {
    Injector injector = new Injector(false);

    injector.registerInstance("{jsonconfig}", AnnotationDescriptor.named("configuration"));

    assertThatThrownBy(() -> ComponentScanner.scan(injector.getCandidateRegistry(), "hs.ddif.plugins.test.project"))
      .isInstanceOf(UnresolvableDependencyException.class);

    injector.register(TestAutoDiscoverableDependency.class);

    ComponentScanner.scan(injector.getCandidateRegistry(), "hs.ddif.plugins.test.project");

    assertNotNull(injector.getInstance(TestDatabase.class));
    assertNotNull(injector.getInstance(TestStatement.class));
    assertNotNull(injector.getInstance(TestAutoDiscoverableInjectAnnotatedDependency.class));
    assertEquals("10.0.2", injector.getInstance(String.class, AnnotationDescriptor.named("version")));
    assertNotNull(injector.getInstance(TestClassWithSetterInjection.class));
  }
}
