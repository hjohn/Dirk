package hs.ddif.core.config.standard;

import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.inject.store.InstantiatorBindingMap;
import hs.ddif.core.inject.store.ScopeResolverManager;
import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.domain.MultipleInstances;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Default implementation of an {@link InstantiationContext}.
 */
public class DefaultInstantiationContext implements InstantiationContext {
  private static final Logger LOGGER = Logger.getLogger(DefaultInstantiationContext.class.getName());

  private final Resolver<Injectable> resolver;
  private final InstantiatorBindingMap instantiatorBindingMap;
  private final ScopeResolverManager scopeResolverManager;

  /**
   * Constructs a new instance.
   *
   * @param instantiatorBindingMap an {@link InstantiatorBindingMap}, cannot be {@code null}
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param scopeResolverManager a {@link ScopeResolver}, cannot be {@code null}
   */
  public DefaultInstantiationContext(Resolver<Injectable> resolver, InstantiatorBindingMap instantiatorBindingMap, ScopeResolverManager scopeResolverManager) {
    this.resolver = resolver;
    this.instantiatorBindingMap = instantiatorBindingMap;
    this.scopeResolverManager = scopeResolverManager;
  }

  @Override
  public <T> T create(Key key) throws InstanceCreationFailure, MultipleInstances {
    Set<Injectable> injectables = resolver.resolve(key);

    if(injectables.size() > 1) {
      throw new MultipleInstances(key, injectables);
    }

    if(injectables.size() == 0) {
      return null;
    }

    return createInstance(injectables.iterator().next());
  }

  @Override
  public <T> List<T> createAll(Key key, Predicate<Type> typePredicate) throws InstanceCreationFailure {
    List<T> instances = new ArrayList<>();

    for(Injectable injectable : resolver.resolve(key)) {
      if(typePredicate == null || typePredicate.test(injectable.getType())) {
        T instance = createInstanceInScope(injectable);

        if(instance != null) {
          instances.add(instance);
        }
      }
    }

    return instances;
  }

  private <T> T createInstance(Injectable injectable) throws InstanceCreationFailure {
    return createInstance(injectable, false);
  }

  private <T> T createInstanceInScope(Injectable injectable) throws InstanceCreationFailure {
    return createInstance(injectable, true);
  }

  private <T> T createInstance(Injectable injectable, boolean allowOutOfScope) throws InstanceCreationFailure {
    ScopeResolver scopeResolver = scopeResolverManager.getScopeResolver(injectable);

    try {
      if(!allowOutOfScope || scopeResolver.isScopeActive(injectable)) {
        return scopeResolver.get(injectable, () -> createInstanceInternal(injectable));
      }

      return null;
    }
    catch(OutOfScopeException e) {
      if(!allowOutOfScope) {
        throw new InstanceCreationFailure(injectable.getType(), "could not be created", e);
      }

      /*
       * Scope was checked to be active (to avoid exception cost), but it still occurred...
       */

      LOGGER.warning("Scope " + scopeResolver.getScopeAnnotationClass() + " should have been active: " + e.getMessage());

      return null;  // same as if scope hadn't been active in the first place
    }
    catch(InstanceCreationFailure e) {
      throw e;
    }
    catch(Exception e) {
      throw new InstanceCreationFailure(injectable.getType(), "could not be created", e);
    }
  }

  private <T> T createInstanceInternal(Injectable injectable) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance {
    List<Injection> injections = new ArrayList<>();

    for(Binding binding : injectable.getBindings()) {
      Instantiator<T> instantiator = instantiatorBindingMap.getInstantiator(binding);

      injections.add(new Injection(binding.getAccessibleObject(), instantiator.getInstance(this)));
    }

    @SuppressWarnings("unchecked")
    T instance = (T)injectable.createInstance(injections);

    return instance;
  }
}
