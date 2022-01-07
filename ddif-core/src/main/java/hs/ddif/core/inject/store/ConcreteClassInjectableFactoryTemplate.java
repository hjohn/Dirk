package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Template to construct {@link ResolvableInjectable}s for concrete classes.
 *
 * <p>For a class to qualify for injection:<ul>
 * <li>it must be a concrete (not abstract) class</li>
 * <li>it must have an empty public constructor or a single constructor annotated with {@literal @}Inject</li>
 * <li>it cannot have any {@code final} fields annotated with {@literal @}Inject</li>
 * <li>it cannot have any unresolved generic type parameters</li>
 * </ul>
 */
public class ConcreteClassInjectableFactoryTemplate implements ClassInjectableFactoryTemplate<Type> {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);
  private static final TypeAnalysis<Type> FAILURE_ABSTRACT = TypeAnalysis.negative("Type cannot be abstract: %1$s");

  private final BindingProvider bindingProvider;
  private final ResolvableInjectableFactory injectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param bindingProvider a {@link BindingProvider}, cannot be null
   * @param injectableFactory a {@link ResolvableInjectableFactory}, cannot be null
   */
  public ConcreteClassInjectableFactoryTemplate(BindingProvider bindingProvider, ResolvableInjectableFactory injectableFactory) {
    this.bindingProvider = bindingProvider;
    this.injectableFactory = injectableFactory;
  }

  @Override
  public TypeAnalysis<Type> analyze(Type type) {
    Class<?> cls = TypeUtils.getRawType(type, null);

    if(Modifier.isAbstract(cls.getModifiers())) {
      return FAILURE_ABSTRACT;
    }

    return TypeAnalysis.positive(type);
  }

  @Override
  public ResolvableInjectable create(TypeAnalysis<Type> analysis) {
    Type type = analysis.getData();
    Class<?> cls = TypeUtils.getRawType(type, null);
    Constructor<?> constructor = bindingProvider.getConstructor(cls);
    List<Binding> bindings = bindingProvider.ofConstructorAndMembers(constructor, cls);

    return injectableFactory.create(
      type,
      Annotations.findDirectlyMetaAnnotatedAnnotations(cls, QUALIFIER),
      bindings,
      AnnotationExtractor.findScopeAnnotation(cls),
      null,
      new ClassObjectFactory(constructor)
    );
  }
}