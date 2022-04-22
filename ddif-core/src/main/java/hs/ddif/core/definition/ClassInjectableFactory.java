package hs.ddif.core.definition;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.util.Types;
import hs.ddif.core.definition.factory.ClassObjectFactory;
import hs.ddif.spi.config.LifeCycleCallbacksFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * Factory interface for creating {@link Injectable}s given a {@link Type}.
 */
public class ClassInjectableFactory {
  private final BindingProvider bindingProvider;
  private final InjectableFactory injectableFactory;
  private final LifeCycleCallbacksFactory lifeCycleCallbacksFactory;

  /**
   * Constructs a new instance.
   *
   * @param bindingProvider a {@link BindingProvider}, cannot be {@code null}
   * @param injectableFactory a {@link InjectableFactory}, cannot be {@code null}
   * @param lifeCycleCallbacksFactory a {@link LifeCycleCallbacksFactory}, cannot be {@code null}
   */
  public ClassInjectableFactory(BindingProvider bindingProvider, InjectableFactory injectableFactory, LifeCycleCallbacksFactory lifeCycleCallbacksFactory) {
    this.bindingProvider = bindingProvider;
    this.injectableFactory = injectableFactory;
    this.lifeCycleCallbacksFactory = lifeCycleCallbacksFactory;
  }

  /**
   * Attempts to create a new {@link Injectable} from the given {@link Type}.
   *
   * @param <T> the type of the given type
   * @param type a {@link Type}, cannot be {@code null}
   * @return a {@link Injectable}, never {@code null}
   * @throws DefinitionException when a definition problem was encountered
   */
  public <T> Injectable<T> create(Type type) throws DefinitionException {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }

    Class<T> cls = Types.raw(type);

    if(Modifier.isAbstract(cls.getModifiers())) {
      throw new DefinitionException(cls, "cannot be abstract");
    }
    if(Types.containsTypeVariables(type)) {
      throw new DefinitionException(cls, "cannot have unresolvable type variables: " + Arrays.toString(cls.getTypeParameters()));
    }

    try {
      Constructor<T> constructor = bindingProvider.getConstructor(cls);
      List<Binding> bindings = bindingProvider.ofConstructorAndMembers(constructor, cls);

      return injectableFactory.create(type, null, cls, bindings, new ClassObjectFactory<>(constructor, lifeCycleCallbacksFactory.create(cls)));
    }
    catch(BindingException e) {
      throw new DefinitionException(cls, "could not be bound", e);
    }
  }
}
