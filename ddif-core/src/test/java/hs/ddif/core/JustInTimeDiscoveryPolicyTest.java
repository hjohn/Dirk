package hs.ddif.core;

import hs.ddif.core.inject.consistency.InjectorStoreConsistencyPolicy;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.test.injectables.BeanWithInjection;
import hs.ddif.core.test.injectables.BigRedBean;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithDependencyOnSampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SampleWithMultipleAnnotatedConstructors;
import hs.ddif.core.test.injectables.SampleWithoutConstructorMatch;
import hs.ddif.core.test.injectables.SimpleBean;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JustInTimeDiscoveryPolicyTest {
  private InjectableStore<ResolvableInjectable> store;

  @Before
  public void before() {
    store = new InjectableStore<>(new InjectorStoreConsistencyPolicy<ResolvableInjectable>(), new JustInTimeDiscoveryPolicy());
  }

  @Test
  public void shouldDiscoverNewTypes() {
    assertFalse(store.resolve(BigRedBean.class).isEmpty());
    assertTrue(store.contains(BigRedBean.class));
  }

  @Test
  public void shouldDiscoverNewTypesAndDependentTypes() {
    assertFalse(store.resolve(BeanWithInjection.class).isEmpty());
    assertTrue(store.contains(BeanWithInjection.class));
    assertTrue(store.contains(SimpleBean.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithoutAnyConstructorMatch() {
    try {
      assertTrue(store.resolve(SampleWithDependencyOnSampleWithoutConstructorMatch.class).isEmpty());
      fail("expected UnresolvedDependencyException");
    }
    catch(BindingException e) {
      assertFalse(store.contains(SampleWithDependencyOnSampleWithoutConstructorMatch.class));
      assertFalse(store.contains(SampleWithoutConstructorMatch.class));
    }
  }

  @Test
  public void shouldNotDiscoverNewTypeWithMultipleConstructorMatch() {
    try {
      assertTrue(store.resolve(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class).isEmpty());
      fail("expected UnresolvedDependencyException");
    }
    catch(BindingException e) {
      assertFalse(store.contains(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class));
      assertFalse(store.contains(SampleWithMultipleAnnotatedConstructors.class));
    }
  }

  @Test(expected = BindingException.class)
  public void shouldThrowBindingExceptionWhenAddingClassWithoutConstructorMatch() {
    store.put(ClassInjectable.of(SampleWithoutConstructorMatch.class));
  }

  @Test
  public void shouldDiscoverNewTypeWithEmptyUnannotatedConstructorAndAnnotatedConstructor() {
    assertFalse(store.resolve(SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor.class).isEmpty());
  }
}
