package hs.ddif.core.inject.store;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.Key;
import hs.ddif.core.inject.consistency.ScopedInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

public class BindingExplorer {

  public static <T extends ScopedInjectable> List<T> discover(Resolver<T> baseResolver, Class<?> cls) {
    if(!Modifier.isAbstract(cls.getModifiers())) {
      @SuppressWarnings("unchecked")
      T injectable = (T)new ClassInjectable(cls);
      List<T> discovered = discover(baseResolver, Set.of(injectable));

      discovered.add(injectable);

      return discovered;
    }

    return List.of();
  }

  public static <T extends ScopedInjectable> List<T> discover(Resolver<T> baseResolver, Set<T> injectables) {
    InjectableStore<T> store = new InjectableStore<>();
    Resolver<T> resolver = new IncludingResolver<>(baseResolver, store);

    store.putAll(injectables);

    List<T> allDiscovered = new ArrayList<>();
    Set<T> unexplored = injectables;

    for(;;) {
      Set<T> discovered = discoverInternal(resolver, unexplored);

      if(discovered.isEmpty()) {
        break;
      }

      unexplored = discovered;
      allDiscovered.addAll(discovered);
      store.putAll(discovered);
    }

    return allDiscovered;
  }

  private static <T extends ScopedInjectable> Set<T> discoverInternal(Resolver<T> resolver, Set<T> injectables) {
    Set<T> discovered = new HashSet<>();

    for(T injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        Key requiredKey = binding.getRequiredKey();

        if(requiredKey != null) {
          Type type = requiredKey.getType();
          Object[] qualifiersAsArray = requiredKey.getQualifiersAsArray();
          Set<T> resolvedInjectables = resolver.resolve(type, qualifiersAsArray);

          if(resolvedInjectables.size() == 0 && qualifiersAsArray.length == 0) {
            Class<?> typeClass = TypeUtils.getRawType(type, null);

            if(!Modifier.isAbstract(typeClass.getModifiers())) {
              @SuppressWarnings("unchecked")
              T classInjectable = (T)new ClassInjectable(type);

              discovered.add(classInjectable);
            }
          }
        }
      }
    }

    return discovered;
  }

  private static class IncludingResolver<T extends ScopedInjectable> implements Resolver<T> {
    final Resolver<T> base;
    final Resolver<T> include;

    IncludingResolver(Resolver<T> base, Resolver<T> include) {
      this.base = base;
      this.include = include;
    }

    @Override
    public Set<T> resolve(Type type, Object... criteria) {
      Set<T> set = new HashSet<>(base.resolve(type, criteria));

      set.addAll(include.resolve(type, criteria));

      return set;
    }
  }
}
