package org.int4.dirk.core;

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
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.definition.BadQualifiedTypeException;
import org.int4.dirk.core.definition.Binding;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.injection.Constructable;
import org.int4.dirk.core.definition.injection.Injection;
import org.int4.dirk.core.test.qualifiers.Green;
import org.int4.dirk.core.test.qualifiers.Red;
import org.int4.dirk.core.test.scope.Dependent;
import org.int4.dirk.core.test.scope.TestScope;
import org.int4.dirk.util.Annotations;
import org.int4.dirk.util.Types;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public class DefaultInjectableFactoryTest {
  private final InjectableFactories injectableFactories = new InjectableFactories();
  private final ScopeResolverManager manager = ScopeResolverManagers.create();
  private final DefaultInjectableFactory factory = new DefaultInjectableFactory(manager, injectableFactories.getInstanceFactory(), InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.SCOPE_STRATEGY, Set.of(Provider.class, List.class, Set.class));
  private final Constructable<BookShop> constructable = new Constructable<>() {
    @Override
    public BookShop create(List<Injection> injections) {
      return null;
    }

    @Override
    public void destroy(BookShop instance) {
    }

    @Override
    public boolean needsDestroy() {
      return false;
    }
  };

  @Test
  void constructorShouldRejectInvalidParameters() {
    assertThatThrownBy(() -> new DefaultInjectableFactory(null, injectableFactories.getInstanceFactory(), InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.SCOPE_STRATEGY, Set.of()))
      .isExactlyInstanceOf(NullPointerException.class)
      .hasMessage("scopeResolverManager")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectableFactory(manager, null, InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.SCOPE_STRATEGY, Set.of()))
      .isExactlyInstanceOf(NullPointerException.class)
      .hasMessage("instanceFactory")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectableFactory(manager, injectableFactories.getInstanceFactory(), null, InjectableFactories.SCOPE_STRATEGY, Set.of()))
      .isExactlyInstanceOf(NullPointerException.class)
      .hasMessage("annotationStrategy")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectableFactory(manager, injectableFactories.getInstanceFactory(), InjectableFactories.ANNOTATION_STRATEGY, null, Set.of()))
      .isExactlyInstanceOf(NullPointerException.class)
      .hasMessage("scopeStrategy")
      .hasNoCause();

    assertThatThrownBy(() -> new DefaultInjectableFactory(manager, injectableFactories.getInstanceFactory(), InjectableFactories.ANNOTATION_STRATEGY, InjectableFactories.SCOPE_STRATEGY, null))
      .isExactlyInstanceOf(NullPointerException.class)
      .hasMessage("extendedTypes")
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
    assertThat(injectable.getInjectionTargets()).isEqualTo(List.of());
    assertThat(injectable.getScopeResolver().getAnnotation()).isEqualTo(Annotations.of(Dependent.class));
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class));
    assertThat(injectable.toString()).isEqualTo("Class [@org.int4.dirk.core.test.qualifiers.Red() org.int4.dirk.core.DefaultInjectableFactoryTest$BookShop]");
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
    assertThat(injectable.getInjectionTargets()).isEqualTo(List.of());
    assertThat(injectable.getScopeResolver().getAnnotation()).isEqualTo(Annotations.of(Singleton.class));
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Green.class));
    assertThat(injectable.toString()).isEqualTo("Producer [@org.int4.dirk.core.test.qualifiers.Green() public org.int4.dirk.core.DefaultInjectableFactoryTest$BookShop org.int4.dirk.core.DefaultInjectableFactoryTest$BookShopFactory.createBookShop()]");
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
    assertThat(injectable.getInjectionTargets()).isEqualTo(List.of());
    assertThat(injectable.getScopeResolver().getAnnotation()).isEqualTo(Annotations.of(Singleton.class));
    assertThat(injectable.getQualifiers()).isEmpty();
    assertThat(injectable.toString()).isEqualTo("Producer [org.int4.dirk.core.DefaultInjectableFactoryTest$BookShop org.int4.dirk.core.DefaultInjectableFactoryTest$BookShopFactory.bookShop]");
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

        @Override
        public boolean needsDestroy() {
          return false;
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
    assertThat(injectable.getInjectionTargets()).isEqualTo(List.of());
    assertThat(injectable.getScopeResolver().getAnnotation()).isEqualTo(Annotations.of(Singleton.class));
    assertThat(injectable.getQualifiers()).containsExactlyInAnyOrder(Annotations.of(Red.class));
    assertThat(injectable.toString()).isEqualTo("Instance of [@org.int4.dirk.core.test.qualifiers.Red() java.lang.String -> Hello]");
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
      .hasMessage("ownerType must be assignable to member's declaring class: class org.int4.dirk.core.DefaultInjectableFactoryTest$BookShop; declaring class: class org.int4.dirk.core.DefaultInjectableFactoryTest$BookShopFactory")
      .hasNoCause();
  }

  @Test
  void createShouldRejectTypesWithMultipleScopeAnnotations() {
    assertThatThrownBy(() -> factory.create(OverScoped.class, null, OverScoped.class, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[class org.int4.dirk.core.DefaultInjectableFactoryTest$OverScoped] cannot have multiple scope annotations, but found: [@jakarta.inject.Singleton(), @org.int4.dirk.core.test.scope.TestScope()]")
      .hasNoCause();
  }

  @Test
  void createShouldRejectProducersWithInjectAnnotation() throws NoSuchMethodException, SecurityException {
    Method method = IllegallyInjectAnnotated.class.getDeclaredMethod("producerMethod");

    assertThatThrownBy(() -> factory.create(IllegallyInjectAnnotated.class, method, method, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Method [java.lang.String org.int4.dirk.core.DefaultInjectableFactoryTest$IllegallyInjectAnnotated.producerMethod()] should not have an inject annotation, but found: [@jakarta.inject.Inject()]")
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
      .hasMessage("Field [private java.lang.String org.int4.dirk.core.DefaultInjectableFactoryTest$IllegallyScopeAnnotated.test2] should not have a scope annotation, but found: @jakarta.inject.Singleton()")
      .hasNoCause();
  }

  @Test
  void createShouldRejectBaseTypesWhichAreProvidedByInjectionTargetExtensions() {
    assertThatThrownBy(() -> factory.create(Provider.class, null, Provider.class, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("[interface jakarta.inject.Provider] cannot be registered as it conflicts with an InjectionTargetExtension for type: interface jakarta.inject.Provider")
      .hasNoCause();
  }

  @Test
  void createShouldRejectProducerWithUnresolvableTypeVariables() throws NoSuchFieldException, SecurityException {
    Field field = UnresolvableProducer.class.getDeclaredField("shop");

    assertThatThrownBy(() -> factory.create(UnresolvableProducer.class, field, field, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [org.int4.dirk.core.DefaultInjectableFactoryTest$Shop org.int4.dirk.core.DefaultInjectableFactoryTest$UnresolvableProducer.shop] has unsuitable type")
      .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
      .isExactlyInstanceOf(BadQualifiedTypeException.class)
      .hasMessage("[org.int4.dirk.core.DefaultInjectableFactoryTest.Shop<T>] cannot have unresolvable type variables or wild cards")
      .hasNoCause();
  }

  @Test
  void createShouldRejectProducerWithUnknownType() throws NoSuchFieldException, SecurityException {
    Field field = UnknownProducer.class.getDeclaredField("shop");

    assertThatThrownBy(() -> factory.create(UnknownProducer.class, field, field, List.of(), constructable))
      .isExactlyInstanceOf(DefinitionException.class)
      .hasMessage("Field [java.lang.Object org.int4.dirk.core.DefaultInjectableFactoryTest$UnknownProducer.shop] has unresolvable return type")
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

