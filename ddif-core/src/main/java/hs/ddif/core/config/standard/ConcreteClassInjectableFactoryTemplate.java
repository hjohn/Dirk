package hs.ddif.core.config.standard;

import hs.ddif.core.definition.InjectableFactory;
import hs.ddif.core.definition.ClassInjectableFactoryTemplate;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.definition.bind.BindingException;
import hs.ddif.core.definition.bind.BindingProvider;
import hs.ddif.core.instantiation.factory.ClassObjectFactory;
import hs.ddif.core.util.Types;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Template to construct {@link Injectable}s for concrete classes.
 *
 * <p>For a class to qualify for injection:<ul>
 * <li>it must be a concrete (not abstract) class</li>
 * <li>it must have an empty public constructor or a single constructor annotated with {@literal @}Inject</li>
 * <li>it cannot have any {@code final} fields annotated with {@literal @}Inject</li>
 * <li>it cannot have any unresolved generic type parameters</li>
 * </ul>
 */
public class ConcreteClassInjectableFactoryTemplate implements ClassInjectableFactoryTemplate<Type> {
  private static final TypeAnalysis<Type> FAILURE_ABSTRACT = TypeAnalysis.negative("Type cannot be abstract: %1$s");

  private final BindingProvider bindingProvider;
  private final InjectableFactory injectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param bindingProvider a {@link BindingProvider}, cannot be {@code null}
   * @param injectableFactory a {@link InjectableFactory}, cannot be {@code null}
   */
  public ConcreteClassInjectableFactoryTemplate(BindingProvider bindingProvider, InjectableFactory injectableFactory) {
    this.bindingProvider = bindingProvider;
    this.injectableFactory = injectableFactory;
  }

  @Override
  public TypeAnalysis<Type> analyze(Type type) {
    Class<?> cls = Types.raw(type);

    if(Modifier.isAbstract(cls.getModifiers())) {
      return FAILURE_ABSTRACT;
    }

    return TypeAnalysis.positive(type);
  }

  @Override
  public Injectable create(TypeAnalysis<Type> analysis) throws BindingException {
    Type type = analysis.getData();
    Class<?> cls = Types.raw(type);
    Constructor<?> constructor = bindingProvider.getConstructor(cls);
    List<Binding> bindings = bindingProvider.ofConstructorAndMembers(constructor, cls);

    return injectableFactory.create(type, cls, bindings, new ClassObjectFactory(constructor));
  }
}