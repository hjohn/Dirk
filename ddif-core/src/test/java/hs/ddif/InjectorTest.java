package hs.ddif;

import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import hs.ddif.AmbigiousBeanException;
import hs.ddif.AmbigiousDependencyException;
import hs.ddif.DependencyException;
import hs.ddif.DuplicateBeanException;
import hs.ddif.Injector;
import hs.ddif.NoSuchBeanException;
import hs.ddif.UnresolvedDependencyException;
import hs.ddif.ViolatesSingularDependencyException;
import hs.ddif.test.injectables.BeanWithBigInjection;
import hs.ddif.test.injectables.BeanWithBigRedInjection;
import hs.ddif.test.injectables.BeanWithCollection;
import hs.ddif.test.injectables.BeanWithCollectionProvider;
import hs.ddif.test.injectables.BeanWithDirectCollectionItemDependency;
import hs.ddif.test.injectables.BeanWithDirectRedCollectionItemDependency;
import hs.ddif.test.injectables.BeanWithInjection;
import hs.ddif.test.injectables.BeanWithInterfaceBasedInjection;
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
import hs.ddif.test.injectables.SimpleBean;
import hs.ddif.test.injectables.SimpleChildBean;
import hs.ddif.test.injectables.SimpleCollectionItemImpl1;
import hs.ddif.test.injectables.SimpleCollectionItemImpl2;
import hs.ddif.test.injectables.SimpleCollectionItemImpl3;
import hs.ddif.test.injectables.SimpleCollectionItemInterface;
import hs.ddif.test.injectables.SimpleImpl;
import hs.ddif.test.injectables.SimpleInterface;
import hs.ddif.test.injectables.UnregisteredParentBean;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

  @Test(expected = NoSuchBeanException.class)
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

  /*
   * Injector#register tests
   */

  @Test
  public void shouldThrowExceptionWhenRegisteringDuplicate() {
    injector.register(ArrayList.class);

    thrown.expect(DuplicateBeanException.class);

    injector.register(ArrayList.class);
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

  @Test(expected = NoSuchBeanException.class)
  public void shouldThrowExceptionWhenRemovingUnregisteredSuperClass() {
    injector.remove(UnregisteredParentBean.class);
  }

  /*
   * Field injection
   */

  @Test
  public void shouldThrowExceptionWhenFinalFieldAnnotatedWithInject() throws NoSuchFieldException, SecurityException {
    thrown.expect(DependencyException.class);
    thrown.expectMessage("Cannot inject final fields: " + FieldInjectionSampleWithAnnotatedFinalField.class.getDeclaredField("injectedValue") + " in: " + FieldInjectionSampleWithAnnotatedFinalField.class);

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
    thrown.expectMessage("Only one constructor is allowed to be annoted with @Inject: " + ConstructorInjectionSampleWithMultipleAnnotatedConstructors.class);

    injector.register(ConstructorInjectionSampleWithMultipleAnnotatedConstructors.class);
  }
}
