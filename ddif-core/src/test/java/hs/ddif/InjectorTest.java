package hs.ddif;

import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import hs.ddif.test.injectables.BeanWithBigInjection;
import hs.ddif.test.injectables.BeanWithBigRedInjection;
import hs.ddif.test.injectables.BeanWithCollection;
import hs.ddif.test.injectables.BeanWithCollectionProvider;
import hs.ddif.test.injectables.BeanWithDirectCollectionItemDependency;
import hs.ddif.test.injectables.BeanWithDirectRedCollectionItemDependency;
import hs.ddif.test.injectables.BeanWithInjection;
import hs.ddif.test.injectables.BeanWithInterfaceBasedInjection;
import hs.ddif.test.injectables.BeanWithOptionalConstructorDependency;
import hs.ddif.test.injectables.BeanWithOptionalDependency;
import hs.ddif.test.injectables.BeanWithUnsupportedOptionalProviderDependency;
import hs.ddif.test.injectables.BeanWithProvider;
import hs.ddif.test.injectables.BeanWithProviderWithoutMatch;
import hs.ddif.test.injectables.BeanWithUnregisteredParent;
import hs.ddif.test.injectables.BeanWithUnresolvedDependency;
import hs.ddif.test.injectables.BeanWithUnresolvedProviderDependency;
import hs.ddif.test.injectables.BigBean;
import hs.ddif.test.injectables.BigRedBean;
import hs.ddif.test.injectables.ConstructorInjectionSample;
import hs.ddif.test.injectables.ConstructorInjectionSampleWithMultipleAnnotatedConstructors;
import hs.ddif.test.injectables.FieldInjectionSampleWithAnnotatedFinalField;
import hs.ddif.test.injectables.SampleWithAnnotatedFinalFields;
import hs.ddif.test.injectables.SampleWithMultipleAnnotatedConstructors;
import hs.ddif.test.injectables.SimpleBean;
import hs.ddif.test.injectables.SimpleChildBean;
import hs.ddif.test.injectables.SimpleCollectionItemImpl1;
import hs.ddif.test.injectables.SimpleCollectionItemImpl2;
import hs.ddif.test.injectables.SimpleCollectionItemImpl3;
import hs.ddif.test.injectables.SimpleCollectionItemInterface;
import hs.ddif.test.injectables.SimpleImpl;
import hs.ddif.test.injectables.SimpleInterface;
import hs.ddif.test.injectables.UnavailableBean;
import hs.ddif.test.injectables.UnregisteredParentBean;

public class InjectorTest {
  private Injector injector;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
    injector = new Injector();

    injector.register(SimpleBean.class);
    injector.register(BeanWithInjection.class);
    injector.register(SimpleImpl.class);
    injector.register(BeanWithInterfaceBasedInjection.class);
    injector.register(BeanWithCollection.class);
    injector.register(BeanWithProvider.class);
    injector.register(BeanWithProviderWithoutMatch.class);
    injector.register(SimpleCollectionItemImpl1.class);
    injector.register(SimpleCollectionItemImpl2.class);
    injector.register(BeanWithUnregisteredParent.class);
  }

  /*
   * Injector#getBean tests:
   */

  @Test
  public void shouldGetSimpleBean() {
    assertNotNull(injector.getInstance(SimpleBean.class));
  }

  @Test(expected = NoSuchBeanException.class)
  public void shouldThrowExceptionWhenGettingUnregisteredBean() {
    injector.getInstance(ArrayList.class);
  }

  @Test(expected = AmbigiousBeanException.class)
  public void shouldThrowExceptionWhenBeanIsAmbigious() {
    injector.getInstance(SimpleCollectionItemInterface.class);
  }

  @Test
  public void shouldGetBeanWithInjection() {
    BeanWithInjection bean = injector.getInstance(BeanWithInjection.class);

    Assert.assertNotNull(bean.getInjectedValue());
    Assert.assertEquals(SimpleBean.class, bean.getInjectedValue().getClass());
  }

  @Test
  public void shouldGetBeanWithInterfaceBasedInjection() {
    BeanWithInterfaceBasedInjection bean = injector.getInstance(BeanWithInterfaceBasedInjection.class);

    Assert.assertNotNull(bean.getInjectedValue());
    Assert.assertEquals(SimpleImpl.class, bean.getInjectedValue().getClass());
  }

  @Test
  public void shouldGetBeanWithProviderInjection() {
    BeanWithProvider bean = injector.getInstance(BeanWithProvider.class);

    Assert.assertNotNull(bean.getSimpleBean());
    Assert.assertEquals(SimpleBean.class, bean.getSimpleBean().getClass());
  }

  @Test
  public void shouldGetBeanWithOptionalDependencyWhenProviderReturnsNull() {
    injector.register(BeanWithOptionalDependency.class);
    injector.register(new Provider<UnavailableBean>() {
      @Override
      public UnavailableBean get() {
        return null;
      }
    });

    Assert.assertNotNull(injector.getInstance(BeanWithOptionalDependency.class));
  }

  @Test
  public void shouldGetBeanWithOptionalDependencyWhenNoProviderAvailable() {
    injector.register(BeanWithOptionalDependency.class);

    Assert.assertNotNull(injector.getInstance(BeanWithOptionalDependency.class));
  }

  @Test
  public void shouldGetBeanWithOptionalConstructorDependencyWhenNoProviderAvailable() {
    injector.register(BeanWithOptionalConstructorDependency.class);

    Assert.assertNotNull(injector.getInstance(BeanWithOptionalConstructorDependency.class));
  }

  @Test  // @Nullable annotation on Provider is just ignored as it makes no sense for Providers
  public void shouldGetBeanWithOptionalProviderDependency() {
    injector.register(new Provider<UnavailableBean>() {
      @Override
      public UnavailableBean get() {
        return null;  // this provider breaks its contract
      }
    });
    injector.register(BeanWithUnsupportedOptionalProviderDependency.class);

    BeanWithUnsupportedOptionalProviderDependency instance = injector.getInstance(BeanWithUnsupportedOptionalProviderDependency.class);

    Assert.assertNotNull(instance);
    Assert.assertNotNull(instance.getUnavailableBeanProvider());

    thrown.expect(NoSuchBeanException.class);

    instance.getUnavailableBeanProvider().get();  // Make sure that null is not returned as that is not part of the Provider contract
  }

  @Test  // Providers cannot be made optional with @Nullable as a provider can always be created; it's only the result of the Provider that could be optional but that requires a different approach (new annotation of sub-interface)
  public void shouldThrowUnresolvedDependencyExceptionWhenRegisteringBeanWithNullableProviderDependencyWhenNoProviderAvailableBecauseProvidersCannotBeMadeOptional() {
    thrown.expect(UnresolvedDependencyException.class);

    injector.register(BeanWithUnsupportedOptionalProviderDependency.class);
  }

  @Test(expected = NoSuchBeanException.class)
  public void shouldThrowExceptionWhenGettingUnavailableBean() {
    injector.register(new Provider<UnavailableBean>() {
      @Override
      public UnavailableBean get() {
        return null;
      }
    });

    injector.getInstance(UnavailableBean.class);
  }

  /*
   * Injector#remove tests:
   */

  @Test
  public void shouldRemoveBean() {
    injector.remove(BeanWithInjection.class);

    try {
      injector.getInstance(BeanWithInjection.class);
      Assert.fail();
    }
    catch(NoSuchBeanException e) {
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenRemovingInterface() {
    injector.remove(SimpleInterface.class);
  }

  @Test(expected = NoSuchInjectableException.class)
  public void shouldThrowExceptionWhenRemovingUnregisteredBean() {
    injector.remove(ArrayList.class);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldThrowExceptionWhenRemovingBeanWouldViolateSingularDependencies() {
    injector.remove(SimpleBean.class);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldRepeatedlyThrowExceptionWhenRemovingBeanWouldViolateSingularDependencies() {
    try {
      injector.remove(SimpleBean.class);
      fail();
    }
    catch(UnresolvedDependencyException e) {
      // expected
    }

    injector.remove(SimpleBean.class);
  }

  @Test
  public void shouldBeAbleToRemoveProviderWhichIsOnlyOptionallyDependedOn() {
    Provider<UnavailableBean> provider = new Provider<UnavailableBean>() {
      @Override
      public UnavailableBean get() {
        return new UnavailableBean();
      }
    };

    injector.register(provider);
    injector.register(BeanWithOptionalDependency.class);

    Assert.assertNotNull(injector.getInstance(BeanWithOptionalDependency.class));
    Assert.assertNotNull(injector.getInstance(BeanWithOptionalDependency.class).getUnavailableBean());

    injector.remove(provider);

    Assert.assertNotNull(injector.getInstance(BeanWithOptionalDependency.class));
    Assert.assertNull(injector.getInstance(BeanWithOptionalDependency.class).getUnavailableBean());
  }

  /*
   * Injector#register tests
   */

  @Test
  public void shouldRegisterStringInstances() {
    injector.registerInstance("a");
    injector.registerInstance("b");
    injector.registerInstance("c");
    injector.registerInstance("d");
  }

  @Test
  public void shouldRegisterSameStringInstancesWithDifferentQualifiers() {
    injector.registerInstance("a", AnnotationDescriptor.describe(Named.class, new Value("value", "name-1")));
    injector.registerInstance("a", AnnotationDescriptor.describe(Named.class, new Value("value", "name-2")));
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringDuplicate() {
    injector.register(String.class);

    thrown.expect(DuplicateBeanException.class);

    injector.register(String.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenRegisteringInterface() {
    injector.register(List.class);
  }

  @Test(expected = UnresolvedDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringBeanWithUnresolvedDependencies() {
    injector.register(BeanWithUnresolvedDependency.class);
  }

  @Test(expected = UnresolvedDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringBeanWithUnresolvedProviderDependencies() {
    injector.register(BeanWithUnresolvedProviderDependency.class);
  }

  @Test(expected = AmbigiousDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringBeanWithAmbigiousDependencies() {
    injector.register(BeanWithDirectCollectionItemDependency.class);
  }

  @Test(expected = UnresolvedDependencyException.class)
  public void shouldRepeatedlyThrowExceptionWhenRegisteringBeanWithUnresolvedDependencies() {
    try {
      injector.register(BeanWithUnresolvedDependency.class);
      fail();
    }
    catch(UnresolvedDependencyException e) {
      // expected
    }

    injector.register(BeanWithUnresolvedDependency.class);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringBeanWouldViolateSingularDependencies() {
    injector.register(SimpleChildBean.class);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldRepeatedlyThrowExceptionWhenRegisteringBeanWouldViolateSingularDependencies() {
    try {
      injector.register(SimpleChildBean.class);
      fail();
    }
    catch(AmbigiousDependencyException e) {
      // expected
    }

    injector.register(SimpleChildBean.class);
  }

  /*
   * Singleton tests
   */

  @Test
  public void shouldRespectSingletonAnnotation() {
    SimpleBean bean1 = injector.getInstance(SimpleBean.class);

    assertTrue(bean1 == injector.getInstance(SimpleBean.class));
    assertTrue(bean1 == injector.getInstance(BeanWithInjection.class).getInjectedValue());
  }

  /*
   * Qualifier tests
   */

  @Test
  public void shouldSupportQualifiers() {
    injector.register(BigRedBean.class);

    injector.register(BeanWithBigInjection.class);
    injector.register(BeanWithBigRedInjection.class);

    injector.remove(BeanWithBigInjection.class);
    injector.remove(BeanWithBigRedInjection.class);

    injector.remove(BigRedBean.class);
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringDependentBeanWithNoMatchForAllQualifiers() {
    injector.register(BigBean.class);

    thrown.expect(UnresolvedDependencyException.class);

    injector.register(BeanWithBigRedInjection.class);  // Won't match BigBean, so won't match anything
  }

  @Test
  public void shouldThrowExceptionWhenRegisteringBeanWithMoreQualifiersWhenItWouldViolateSingularDependencies() {
    injector.register(BigBean.class);
    injector.register(BeanWithBigInjection.class);

    thrown.expect(ViolatesSingularDependencyException.class);

    injector.register(BigRedBean.class);  // Would also match injection for BeanWithBigInjection
  }

  @Test
  public void shouldThrowExceptionWhenRemovingRequiredBeanWithMoreQualifiers() {
    injector.register(BigRedBean.class);
    injector.register(BeanWithBigInjection.class);

    thrown.expect(ViolatesSingularDependencyException.class);

    injector.remove(BigRedBean.class);
  }

  @Test
  public void shouldGetBeanWithInjectionWithMultipleTypeMatchesWhenDisambiguatedWithQualifier() {
    injector.register(SimpleCollectionItemImpl3.class);  // One of several, this one qualified
    injector.register(BeanWithDirectRedCollectionItemDependency.class);  // Depends on the specific one above using qualifier

    assertNotNull(injector.getInstance(BeanWithDirectRedCollectionItemDependency.class));
  }

  /*
   * PostConstruct/PreDestroy tests
   */

  /*
   * Collection tests
   */

  @Test
  public void shouldInjectCollection() {
    BeanWithCollection bean = injector.getInstance(BeanWithCollection.class);

    Assert.assertNotNull(bean.getInjectedValues());
    Assert.assertEquals(2, bean.getInjectedValues().size());

    injector.remove(SimpleCollectionItemImpl2.class);

    BeanWithCollection bean2 = injector.getInstance(BeanWithCollection.class);

    Assert.assertEquals(2, bean.getInjectedValues().size());
    Assert.assertEquals(1, bean2.getInjectedValues().size());
  }

  @Test
  public void shouldInjectEmptyCollection() {
    BeanWithCollection bean = injector.getInstance(BeanWithCollection.class);

    Assert.assertNotNull(bean.getInjectedValues());
    Assert.assertEquals(2, bean.getInjectedValues().size());

    injector.remove(SimpleCollectionItemImpl1.class);
    injector.remove(SimpleCollectionItemImpl2.class);

    BeanWithCollection bean2 = injector.getInstance(BeanWithCollection.class);

    Assert.assertEquals(2, bean.getInjectedValues().size());
    Assert.assertEquals(0, bean2.getInjectedValues().size());
  }

  @Test
  public void shouldInjectCollectionProvider() {
    injector.register(BeanWithCollectionProvider.class);

    BeanWithCollectionProvider bean = injector.getInstance(BeanWithCollectionProvider.class);

    Assert.assertNotNull(bean.getInjectedValues());
    Assert.assertEquals(2, bean.getInjectedValues().size());

    injector.remove(SimpleCollectionItemImpl2.class);

    BeanWithCollectionProvider bean2 = injector.getInstance(BeanWithCollectionProvider.class);

    Assert.assertEquals(1, bean.getInjectedValues().size());  // provider resolves dynamically, so this is now 1 after the removal of SimpleCollectionItemImpl2.class
    Assert.assertEquals(1, bean2.getInjectedValues().size());
  }

  /*
   * Misc
   */

  @Test(expected = NoSuchInjectableException.class)
  public void shouldThrowExceptionWhenRemovingUnregisteredSuperClass() {
    injector.remove(UnregisteredParentBean.class);
  }

  /*
   * Field injection
   */

  @Test
  public void shouldThrowExceptionWhenFinalFieldAnnotatedWithInject() throws NoSuchFieldException, SecurityException {
    thrown.expect(BindingException.class);
    thrown.expectMessage("Cannot inject final field: " + FieldInjectionSampleWithAnnotatedFinalField.class.getDeclaredField("injectedValue") + " in: " + FieldInjectionSampleWithAnnotatedFinalField.class);

    injector.register(FieldInjectionSampleWithAnnotatedFinalField.class);
  }

  /*
   * Constructor Injection
   */

  @Test
  public void shouldInjectConstructor() {
    injector.register(ConstructorInjectionSample.class);

    ConstructorInjectionSample instance = injector.getInstance(ConstructorInjectionSample.class);

    assertNotNull(instance);
    assertThat(instance.getInjectedValue(), isA(SimpleBean.class));
  }

  @Test
  public void shouldThrowExceptionWhenMultipleConstructorsAnnotatedWithInject() {
    thrown.expect(DependencyException.class);
    thrown.expectMessage("Multiple constructors found to be annotated with @Inject, but only one allowed: " + ConstructorInjectionSampleWithMultipleAnnotatedConstructors.class);

    injector.register(ConstructorInjectionSampleWithMultipleAnnotatedConstructors.class);
  }

  /*
   * Provider use
   */

  @Test
  public void shouldRegisterAndUseProvider() {
    injector.register(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    assertEquals("a string", injector.getInstance(String.class));
  }

  @Test
  public void shouldRemoveProvider() {
    Provider<String> provider = new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    };

    injector.register(provider);
    injector.remove(provider);
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldThrowExceptionWhenRegisteringProviderWouldViolateSingularDependencies() {
    injector.register(new Provider<SimpleChildBean>() {
      @Override
      public SimpleChildBean get() {
        return new SimpleChildBean();
      }
    });
  }

  @Test(expected = NoSuchInjectableException.class)
  public void shouldThrowExceptionWhenRemovingSimilarProvider() {
    injector.register(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    injector.remove(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });
  }

  @Test(expected = NoSuchInjectableException.class)
  public void shouldThrowExceptionWhenRemovingProviderByClass() {
    injector.register(new Provider<String>() {
      @Override
      public String get() {
        return "a string";
      }
    });

    injector.remove(String.class);
  }

  @Test
  public void shouldRegisterInstance() {
    injector.registerInstance(new String("hello there!"));
  }

  @Test
  public void shouldRegisterInstanceEvenWithBadAnnotations() {
    injector.registerInstance(new SampleWithMultipleAnnotatedConstructors());  // note, not a class, but an instantiated object!
  }

  @Test
  public void shouldRegisterInstanceEvenWithAnnotatedFinalFields() {
    injector.registerInstance(new SampleWithAnnotatedFinalFields());  // note, not a class, but an instantiated object!
  }
}
