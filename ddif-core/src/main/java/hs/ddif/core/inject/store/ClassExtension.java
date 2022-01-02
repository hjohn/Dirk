package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ClassInjectableFactory.Extension;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Extension which will make concrete classes available in the injector.
 *
 * <p>For a class to qualify for injection:<ul>
 * <li>it must be a concrete (not abstract) class</li>
 * <li>it must have an empty public constructor or a single constructor annotated with {@literal @}Inject</li>
 * <li>it cannot have any {@code final} fields annotated with {@literal @}Inject</li>
 * <li>it cannot have any unresolved generic type parameters</li>
 * </ul>
 */
public class ClassExtension implements Extension {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

  private final ResolvableInjectableFactory factory;

  /**
   * Constructs a new instance.
   *
   * @param factory a {@link ResolvableInjectableFactory}, cannot be null
   */
  public ClassExtension(ResolvableInjectableFactory factory) {
    this.factory = factory;
  }

  @Override
  public String getPreconditionText() {
    return "a concrete class";
  }

  @Override
  public ResolvableInjectable create(Type type) {
    Class<?> cls = TypeUtils.getRawType(type, null);

    if(Modifier.isAbstract(cls.getModifiers())) {
      return null;
    }

    Constructor<?> constructor = BindingProvider.getConstructor(cls);
    List<Binding> bindings = BindingProvider.ofConstructorAndMembers(constructor, cls);

    return factory.create(
      type,
      Annotations.findDirectlyMetaAnnotatedAnnotations(cls, QUALIFIER),
      bindings,
      AnnotationExtractor.findScopeAnnotation(cls),
      null,
      new ClassObjectFactory(constructor)
    );
  }
}