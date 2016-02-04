package hs.ddif.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import hs.ddif.core.BindingException;
import hs.ddif.core.ClassInjectable;
import hs.ddif.core.InjectableStore;
import hs.ddif.core.InjectorStoreConsistencyPolicy;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.ddif.core.Key;
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

public class JustInTimeDiscoveryPolicyTest {
  private InjectableStore store;

  @Before
  public void before() {
    store = new InjectableStore(new InjectorStoreConsistencyPolicy(), new JustInTimeDiscoveryPolicy());
  }

  @Test
  public void shouldDiscoverNewTypes() {
    assertFalse(store.resolve(new Key(BigRedBean.class)).isEmpty());
    assertTrue(store.contains(BigRedBean.class));
  }

  @Test
  public void shouldDiscoverNewTypesAndDependentTypes() {
    assertFalse(store.resolve(new Key(BeanWithInjection.class)).isEmpty());
    assertTrue(store.contains(BeanWithInjection.class));
    assertTrue(store.contains(SimpleBean.class));
  }

  @Test
  public void shouldNotDiscoverNewTypeWithoutAnyConstructorMatch() {
    try {
      assertTrue(store.resolve(new Key(SampleWithDependencyOnSampleWithoutConstructorMatch.class)).isEmpty());
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
      assertTrue(store.resolve(new Key(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class)).isEmpty());
      fail("expected UnresolvedDependencyException");
    }
    catch(BindingException e) {
      assertFalse(store.contains(SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors.class));
      assertFalse(store.contains(SampleWithMultipleAnnotatedConstructors.class));
    }
  }

  @Test(expected = BindingException.class)
  public void shouldThrowBindingExceptionWhenAddingClassWithoutConstructorMatch() {
    store.put(new ClassInjectable(SampleWithoutConstructorMatch.class));
  }

  @Test
  public void shouldDiscoverNewTypeWithEmptyUnannotatedConstructorAndAnnotatedConstructor() {
    assertFalse(store.resolve(new Key(SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor.class)).isEmpty());
  }
}
