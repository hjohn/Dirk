package hs.ddif.core;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.core.definition.BadQualifiedTypeException;
import hs.ddif.core.definition.Binding;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.injection.Constructable;
import hs.ddif.core.definition.injection.Injection;
import hs.ddif.core.test.qualifiers.Green;
import hs.ddif.core.test.qualifiers.Red;
import hs.ddif.core.test.scope.Dependent;
import hs.ddif.core.test.scope.TestScope;
import hs.ddif.util.Annotations;
import hs.ddif.util.Types;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public class DefaultInjectableFactoryTest {
  private final ScopeResolverManager manager = ScopeResolverManagers.create();
  private final DefaultInjectableFactory factory = new DefaultInjectableFactory(manager, InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.SCOPE_STRATEGY, Set.of(Provider.class, List.class, Set.class));
  private final Constructable<BookShop> constructable = new Constructable<>() {
    @Override
    public BookShop create(List<Injection> injections) {
      return null;
    }

    @Override
    public void destroy(BookShop instance) {
    }
  };

  @Test
  void constructorShouldRejectInvalidParameters() {
    assertThatThrownBy(() -> new DefaultInjectableFactory(null, InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.SCOPE_STRATEGY, Set.of()))
      .isExactlyInstanceOf(NullPointerException.class)
      .hasMessage("scopeResolverManager cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectableFactory(manager, null, InjectableFactories.SCOPE_STRATEGY, Set.of()))
      .isExactlyInstanceOf(NullPointerException.class)
      .hasMessage("annotationStrategy cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectableFactory(manager, InjectableFactories.ANNOTATION_STRATEGY, null, Set.of()))
      .isExactlyInstanceOf(NullPointerException.class)
      .hasMessage("scopeStrategy cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectableFactory(manager,  InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.SCOPE_STRATEGY, null))
      .isExactlyInstanceOf(NullPointerException.class)
      .hasMessage("extendedTypes cannot be null")
      .hasNoCause();
  }

  @Test
  void createShouldCreateClassBasedInjectable() throws DefinitionException {
    Injectable<BookShop> injectable = factory.create(BookShop.class, null, BookShop.class, List.of(), constructable);

    assertThat(injectable).isNotNull();
    assertThat(injectable.getType()).isEqualTo(BookShop.class);
    assertThat(injectable.getTypes()).containsExactlyInAnyOrder(
      BookShop.class,
      Business.class,
      Types.parameterize(Shop.class, Book.class),
      Object.class
    );
    assertThat(injectable.getBindings()).isEqualTo(List.of());
    assertThat(injectable.getScopeResolver().getAnnotationClass()).isEqualTo(Dependent.class);
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class));
    assertThat(injectable.toString()).isEqualTo("Class [@hs.ddif.core.test.qualifiers.Red() hs.ddif.core.DefaultInjectableFactoryTest$BookShop]");
  }

  @Test
  void createShouldCreateMethodBasedInjectable() throws Exception {
    Method method = BookShopFactory.class.getDeclaredMethod("createBookShop");

    Injectable<BookShop> injectable = factory.create(BookShopFactory.class, method, method, List.of(), constructable);

    assertThat(injectable).isNotNull();
    assertThat(injectable.getType()).isEqualTo(BookShop.class);
    assertThat(injectable.getTypes()).containsExactlyInAnyOrder(
      BookShop.class,
      Business.class,
      Types.parameterize(Shop.class, Book.class),
      Object.class
    );
    assertThat(injectable.getBindings()).isEqualTo(List.of());
    assertThat(injectable.getScopeResolver().getAnnotationClass()).isEqualTo(Singleton.class);
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Green.class));
    assertThat(injectable.toString()).isEqualTo("Producer [@hs.ddif.core.test.qualifiers.Green() public hs.ddif.core.DefaultInjectableFactoryTest$BookShop hs.ddif.core.DefaultInjectableFactoryTest$BookShopFactory.createBookShop()]");
  }

  @Test
  void createShouldCreateFieldBasedInjectable() throws Exception {
    Field field = BookShopFactory.class.getDeclaredField("bookShop");

    Injectable<BookShop> injectable = factory.create(BookShopFactory.class, field, field, List.of(), constructable);

    assertThat(injectable).isNotNull();
    assertThat(injectable.getType()).isEqualTo(BookShop.class);
    assertThat(injectable.getTypes()).containsExactlyInAnyOrder(
      BookShop.class,
      Business.class,
      Types.parameterize(Shop.class, Book.class),
      Object.class
    );
    assertThat(injectable.getBindings()).isEqualTo(List.of());
    assertThat(injectable.getScopeResolver().getAnnotationClass()).isEqualTo(Singleton.class);
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.toString()).isEqualTo("Producer [hs.ddif.core.DefaultInjectableFactoryTest$BookShop hs.ddif.core.DefaultInjectableFactoryTest$BookShopFactory.bookShop]");
  }

  @Test
  void createShouldCreateInstanceBasedInjectable() throws Exception {
    String instance = "Hello";
    Annotation[] qualifiers = new Annotation[] {Annotations.of(Singleton.class), Annotations.of(Red.class)};

    Injectable<String> injectable = factory.create(
      instance.getClass(),
      null,
      new FakeAnnotatedElement(instance, qualifiers),
      List.of(),
      new Constructable<>() {
        @Override
        public String create(List<Injection> injections) {
          return instance;
        }

        @Override
        public void destroy(String instance) {
        }
      }
    );

    assertThat(injectable).isNotNull();
    assertThat(injectable.getType()).isEqualTo(String.class);
    assertThat(injectable.getTypes()).contains(
      CharSequence.class,
      Serializable.class,
      Types.parameterize(Comparable.class, String.class),
      String.class,
      Object.class
    );
    assertThat(injectable.getBindings()).isEqualTo(List.of());
    assertThat(injectable.getScopeResolver().getAnnotationClass()).isEqualTo(Singleton.class);
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class));
    assertThat(injectable.toString()).isEqualTo("Instance of [@hs.ddif.core.test.qualifiers.Red() java.lang.String -> Hello]");
  }

  @Test
  void createShouldRejectBadParameters() throws NoSuchMethodException, SecurityException {
    assertThatThrownBy(() -> factory.create(null, null, BookShop.class, List.of(), constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> factory.create(BookShop.class, null, null, List.of(), constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("element cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> factory.create(BookShop.class, null, BookShop.class, null, constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("bindings cannot be null")
      .hasNoCause();

    assertThatThrownBy(() -> factory.create(BookShop.class, null, BookShop.class, List.of(), null))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("constructable cannot be null")
      .hasNoCause();

    Method factoryMethod = BookShopFactory.class.getDeclaredMethod("createBookShop");

    assertThatThrownBy(() -> factory.create(BookShop.class, factoryMethod, factoryMethod, List.of(), constructable))
      .isExactlyInstanceOf(IllegalArgumentException.class)
      .hasMessage("ownerType must be assignable to member's declaring class: class hs.ddif.core.DefaultInjectableFactoryTest$BookShop; declaring class: class hs.ddif.core.DefaultInjectableFactoryTest$BookShopFactory")
      .hasNoCause();
  }

  @Test
  void createShouldRejectTypesWithMultipleScopeAnnotations() {
    assertThatThrownBy(() -> factory.create(OverScoped.class, null, OverScoped.class, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class hs.ddif.core.DefaultInjectableFactoryTest$OverScoped] cannot have multiple scope annotations, but found: [@hs.ddif.core.test.scope.TestScope(), @jakarta.inject.Singleton()]")
      .hasNoCause();
  }

  @Test
  void createShouldRejectProducersWithInjectAnnotation() throws NoSuchMethodException, SecurityException {
    Method method = IllegallyInjectAnnotated.class.getDeclaredMethod("producerMethod");

    assertThatThrownBy(() -> factory.create(IllegallyInjectAnnotated.class, method, method, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [java.lang.String hs.ddif.core.DefaultInjectableFactoryTest$IllegallyInjectAnnotated.producerMethod()] should not have an inject annotation, but found: [@jakarta.inject.Inject()]")
      .hasNoCause();
  }

  @Test
  void createShouldRejectBindingsWithScopeAnnotation() throws Exception {
    Binding binding1 = mock(Binding.class);
    Binding binding2 = mock(Binding.class);

    when(binding1.getAnnotatedElement()).thenReturn(IllegallyScopeAnnotated.class.getDeclaredField("test"));
    when(binding2.getAnnotatedElement()).thenReturn(IllegallyScopeAnnotated.class.getDeclaredField("test2"));

    assertThatThrownBy(() -> factory.create(IllegallyScopeAnnotated.class, null, IllegallyScopeAnnotated.class, List.of(binding1, binding2), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [private java.lang.String hs.ddif.core.DefaultInjectableFactoryTest$IllegallyScopeAnnotated.test2] should not have a scope annotation, but found: interface jakarta.inject.Singleton")
      .hasNoCause();
  }

  @Test
  void createShouldRejectBaseTypesWhichAreProvidedByTypeExtensions() {
    assertThatThrownBy(() -> factory.create(Provider.class, null, Provider.class, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[interface jakarta.inject.Provider] cannot be registered as it conflicts with a TypeExtension for type: interface jakarta.inject.Provider")
      .hasNoCause();
  }

  @Test
  void createShouldRejectProducerWithUnresolvableTypeVariables() throws NoSuchFieldException, SecurityException {
    Field field = UnresolvableProducer.class.getDeclaredField("shop");

    assertThatThrownBy(() -> factory.create(UnresolvableProducer.class, field, field, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [hs.ddif.core.DefaultInjectableFactoryTest$Shop hs.ddif.core.DefaultInjectableFactoryTest$UnresolvableProducer.shop] has unsuitable type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[hs.ddif.core.DefaultInjectableFactoryTest.Shop<T>] cannot have unresolvable type variables or wild cards")
      .hasNoCause();
  }

  @Test
  void createShouldRejectProducerWithUnknownType() throws NoSuchFieldException, SecurityException {
    Field field = UnknownProducer.class.getDeclaredField("shop");

    assertThatThrownBy(() -> factory.create(UnknownProducer.class, field, field, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [java.lang.Object hs.ddif.core.DefaultInjectableFactoryTest$UnknownProducer.shop] has unresolvable return type")
      .hasNoCause();
  }

  interface Shop<T> {
    default T shopStuff(T t) { return t; }
  }

  static class Book {
  }

  @Green  // ignored, not on main type
  static class Business {
  }

  @Red
  static class BookShop extends Business implements Shop<Book>, Provider<String> {
    @Override
    public String get() {
      return "My BookShop";
    }
  }

  static class BookShopFactory {
    @Singleton
    BookShop bookShop = new BookShop();

    @Green
    @Singleton
    public BookShop createBookShop() {
      return null;
    }
  }

  @Singleton @TestScope
  static class OverScoped {
  }

  static class IllegallyInjectAnnotated {
    @Inject
    String producerMethod() {
      return "";
    }
  }

  static class IllegallyScopeAnnotated {
    @Inject private String test;
    @Inject @Singleton private String test2;
  }

  static class UnresolvableProducer<T> {
    Shop<T> shop;
  }

  static class UnknownProducer<T> {
    T shop;
  }

  private static class FakeAnnotatedElement implements AnnotatedElement {
    private final Object instance;
    private final Annotation[] qualifiers;

    FakeAnnotatedElement(Object instance, Annotation... qualifiers) {
      this.instance = instance;
      this.qualifiers = qualifiers;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
      return (T)Arrays.stream(qualifiers).filter(q -> q.annotationType().equals(annotationClass)).findFirst().orElse(null);
    }

    @Override
    public Annotation[] getAnnotations() {
      return qualifiers.clone();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
      return qualifiers.clone();
    }

    @Override
    public int hashCode() {
      return instance.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }
      if(obj == null || getClass() != obj.getClass()) {
        return false;
      }

      FakeAnnotatedElement other = (FakeAnnotatedElement)obj;

      return Objects.equals(instance, other.instance);
    }

    @Override
    public String toString() {
      return instance.toString();
    }
  }
}
