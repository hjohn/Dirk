package hs.ddif.core;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.Key;
import hs.ddif.core.inject.instantiator.DiscoveryException;
import hs.ddif.core.inject.instantiator.InjectableDiscoverer;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

public class RecursiveInjectableDiscoverer implements InjectableDiscoverer {

  @Override
  public List<ResolvableInjectable> discover(Resolver<ResolvableInjectable> baseResolver, Class<?> cls) throws DiscoveryException {
    if(!Modifier.isAbstract(cls.getModifiers())) {
      ResolvableInjectable injectable = new ClassInjectable(cls);
      List<ResolvableInjectable> discovered = discover(baseResolver, Set.of(injectable));

      discovered.add(injectable);

      return discovered;
    }

    throw new DiscoveryException("Cannot perform discovery from abstract type: " + cls);
  }

  public static List<ResolvableInjectable> discover(Resolver<ResolvableInjectable> baseResolver, Set<ResolvableInjectable> injectables) throws DiscoveryException {
    InjectableStore<ResolvableInjectable> store = new InjectableStore<>();
    Resolver<ResolvableInjectable> resolver = new IncludingResolver(baseResolver, store);

    store.putAll(injectables);

    List<ResolvableInjectable> allDiscovered = new ArrayList<>();
    Set<ResolvableInjectable> unexplored = injectables;

    for(;;) {
      Set<ResolvableInjectable> discovered = discoverInternal(resolver, unexplored);

      if(discovered.isEmpty()) {
        break;
      }

      unexplored = discovered;
      allDiscovered.addAll(discovered);
      store.putAll(discovered);
    }

    return allDiscovered;
  }

  private static Set<ResolvableInjectable> discoverInternal(Resolver<ResolvableInjectable> resolver, Set<ResolvableInjectable> injectables) throws DiscoveryException {
    Set<ResolvableInjectable> discovered = new HashSet<>();

    for(ResolvableInjectable injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        Key requiredKey = binding.getRequiredKey();

        if(requiredKey != null) {
          Type type = requiredKey.getType();
          Object[] qualifiersAsArray = requiredKey.getQualifiersAsArray();
          Set<ResolvableInjectable> resolvedInjectables = resolver.resolve(type, qualifiersAsArray);

          if(resolvedInjectables.size() == 0 && qualifiersAsArray.length == 0) {
            Class<?> typeClass = TypeUtils.getRawType(type, null);

            if(!Modifier.isAbstract(typeClass.getModifiers())) {
              discovered.add(new ClassInjectable(type));
              continue;
            }
          }

          throw new DiscoveryException("Unable to discover required binding: " + binding);
        }
      }
    }

    return discovered;
  }

  private static class IncludingResolver implements Resolver<ResolvableInjectable> {
    final Resolver<ResolvableInjectable> base;
    final Resolver<ResolvableInjectable> include;

    IncludingResolver(Resolver<ResolvableInjectable> base, Resolver<ResolvableInjectable> include) {
      this.base = base;
      this.include = include;
    }

    @Override
    public Set<ResolvableInjectable> resolve(Type type, Object... criteria) {
      Set<ResolvableInjectable> set = new HashSet<>(base.resolve(type, criteria));

      set.addAll(include.resolve(type, criteria));

      return set;
    }
  }
}
