package hs.ddif.core;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.definition.DiscoveryExtension;
import hs.ddif.api.definition.DiscoveryExtension.Registry;
import hs.ddif.api.instantiation.InstantiatorFactory;
import hs.ddif.api.instantiation.domain.Key;
import hs.ddif.api.util.Types;
import hs.ddif.core.definition.Binding;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.discovery.Discoverer;
import hs.ddif.core.discovery.DiscovererFactory;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * An implementation of {@link DiscovererFactory} which optionally does auto discovery
 * through bindings when gathering injectables for a {@link Type}.
 */
class DefaultDiscovererFactory implements DiscovererFactory {
  private static final Map<Type, List<Injectable<?>>> DERIVED_INJECTABLES = new WeakHashMap<>();
  private static final Discoverer EMPTY = new Discoverer() {
    @Override
    public Set<Injectable<?>> discover() {
      return Set.of();
    }

    @Override
    public List<String> getProblems() {
      return List.of();
    }
  };

  private final boolean autoDiscovery;
  private final List<DiscoveryExtension> extensions;
  private final InstantiatorFactory instantiatorFactory;
  private final ClassInjectableFactory classInjectableFactory;
  private final MethodInjectableFactory methodInjectableFactory;
  private final FieldInjectableFactory fieldInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param extensions a list of {@link DiscoveryExtension}s, cannot be {@code null} or contain {@code null}s but can be empty
   * @param autoDiscovery {@code true} when auto discovery should be used, otherwise {@code false}
   * @param instantiatorFactory an {@link InstantiatorFactory}, cannot be {@code null}
   * @param classInjectableFactory a {@link ClassInjectableFactory}, cannot be {@code null}
   * @param methodInjectableFactory a {@link MethodInjectableFactory}, cannot be {@code null}
   * @param fieldInjectableFactory a {@link FieldInjectableFactory}, cannot be {@code null}
   */
  public DefaultDiscovererFactory(boolean autoDiscovery, List<DiscoveryExtension> extensions, InstantiatorFactory instantiatorFactory, ClassInjectableFactory classInjectableFactory, MethodInjectableFactory methodInjectableFactory, FieldInjectableFactory fieldInjectableFactory) {
    this.autoDiscovery = autoDiscovery;
    this.extensions = extensions;
    this.instantiatorFactory = instantiatorFactory;
    this.classInjectableFactory = classInjectableFactory;
    this.methodInjectableFactory = methodInjectableFactory;
    this.fieldInjectableFactory = fieldInjectableFactory;
  }

  @Override
  public Discoverer create(Resolver<Injectable<?>> resolver, List<Type> types) {  // used during normal registration
    return new SimpleDiscoverer(resolver, types.stream().map(Key::new).collect(Collectors.toList()));
  }

  @Override
  public Discoverer create(Resolver<Injectable<?>> resolver, Injectable<?> injectable) {  // used for registering instances
    return new SimpleDiscoverer(resolver, injectable);
  }

  @Override
  public Discoverer create(Resolver<Injectable<?>> resolver, Key key) {  // used during instantiation
    if(!autoDiscovery || !resolver.resolve(key).isEmpty()) {
      return EMPTY;
    }

    return new SimpleDiscoverer(resolver, List.of(key));
  }

  private class SimpleDiscoverer implements Discoverer {
    private final QualifiedTypeStore<Injectable<?>> tempStore = new QualifiedTypeStore<>(i -> new Key(i.getType(), i.getQualifiers()), i -> i.getTypes());

    /**
     * When auto discovery is on, keeps track of unresolved bindings. The map contains
     * the key which is already processed through an Instantiator (and so refers to the
     * actual Injectable that may need to be created, and not to a wrapper like Provider) and
     * the binding from which it was derived.
     *
     * <p>A linked hash map is used to get deterministic behavior for the exception messages.
     */
    private final Map<Key, Binding> unresolvedBindings = new LinkedHashMap<>();

    /**
     * Keys that must always be discovered without the use of auto discovery through
     * bindings.
     */
    private final List<Key> requiredKeys = new ArrayList<>();

    private final Map<Key, Key> via = new HashMap<>();
    private final Set<Type> visitedTypes = new HashSet<>();
    private final Set<Type> visitTypes = new HashSet<>();
    private final List<String> encounteredProblems = new ArrayList<>();

    private final IncludingResolver includingResolver;

    SimpleDiscoverer(Resolver<Injectable<?>> resolver, List<Key> keys) {
      this.includingResolver = new IncludingResolver(resolver::resolve, tempStore);

      for(Key key : keys) {
        visitTypes.add(key.getType());
        requiredKeys.add(key);
      }
    }

    SimpleDiscoverer(Resolver<Injectable<?>> resolver, Injectable<?> injectable) {
      this.includingResolver = new IncludingResolver(resolver::resolve, tempStore);

      tempStore.put(injectable);
      visitTypes.add(injectable.getType());
    }

    @Override
    public List<String> getProblems() {
      return encounteredProblems;
    }

    @Override
    public Set<Injectable<?>> discover() {

      /*
       * Discover new injectables via various mechanisms, in a very specific order.
       *
       * Discovery via Extensions is always done first, and rerun if any of the later
       * discovery mechanism discovers new injectables as more might get discovered
       * via extensions again. Discovery via extensions is stateless as it works on
       * types, the exact same injectables will always be derived given the same input
       * type. They are never optional and an injectable cannot be gathered without also
       * including any injectables derived via its type.
       *
       * Discovery of the input types is the second mechanism. This only needs to run
       * once as either all input types are correctly discovered or an exception is
       * thrown. This runs after extensions because it is possible for a type to
       * provide itself if an extension supports this. For example, a type could
       * produce itself through a static producer method or field; this is an
       * acceptable replacement for discovering an input type as it is deterministic.
       * The injectable types created through this mechanism will already have been
       * processed by extensions, but any types resulting from new bindings of the
       * created injectables will not yet have, so discovery via extensions must
       * rerun after this mechanism.
       *
       * Finally there is discovery though bindings. This is only done if auto
       * discovery through bindings is allowed. As it is possible that a discovery
       * made here can resolve other bindings as well if processed by extensions, this
       * mechanism must stop after each each discovery for extension discovery to run
       * again. When there are multiple unresolved bindings, this process is
       * deterministic in that it will always use the same order. However, use of a
       * different order might lead to different results. It is however highly
       * unlikely that this would lead to a set of injectables that could be registered
       * without it being rejected for containing duplicates.
       */

      while(discoverViaExtensions() || discoverInputTypes() || discoverBindings()) {}

      return tempStore.toSet();
    }

    private boolean addInjectables(List<Injectable<?>> injectables) {
      if(injectables.isEmpty()) {
        return false;
      }

      tempStore.putAll(injectables);

      for(Injectable<?> injectable : injectables) {
        Key injectableKey = new Key(injectable.getType(), injectable.getQualifiers());

        visitTypes.add(injectable.getType());

        for(Binding binding : injectable.getBindings()) {
          Key key = instantiatorFactory.getInstantiator(binding.getKey(), binding.getAnnotatedElement()).getKey();

          if(includingResolver.resolve(key).isEmpty()) {
            via.put(key, injectableKey);
            visitTypes.add(key.getType());

            if(autoDiscovery) {
              unresolvedBindings.put(key, binding);
            }
          }
        }
      }

      return true;
    }

    private boolean discoverViaExtensions() {
      List<Injectable<?>> extensionDiscoveries = new ArrayList<>();

      for(Type type : visitTypes) {
        if(visitedTypes.add(type)) {
          extensionDiscoveries.addAll(DERIVED_INJECTABLES
            .computeIfAbsent(type, k -> {
              DerivedRegistry registry = new DerivedRegistry();

              for(DiscoveryExtension injectableExtension : extensions) {
                // extension don't necessarily resolve the type examined; they might though through for example a static producer which produces itself
                injectableExtension.deriveTypes(registry, type);
              }

              return registry.derivedInjectables;
            }));
        }
      }

      visitTypes.clear();

      return addInjectables(extensionDiscoveries);
    }

    private boolean discoverInputTypes() {
      List<Injectable<?>> injectables = new ArrayList<>();
      DefinitionException throwable = null;

      for(Key key : requiredKeys) {
        boolean alreadyDerived = tempStore.contains(key);  // was this key already derived through another mechanism?

        try {
          injectables.add(attemptCreateInjectable(key));

          if(alreadyDerived) {  // if creation succeeded but it was also derived another way then creation is ambiguous, signal this
            alreadyDerived = false;  // clear flag so exception is not ignored

            throw new DefinitionException(Types.raw(key.getType()), "creation is ambiguous, there are multiple ways to create it");
          }
        }
        catch(Exception e) {
          if(!alreadyDerived) {  // if creation failed but it was derived through other means, ignore the exception, this is acceptable as it is not ambiguous
            if(throwable == null) {
              throwable = new DefinitionException("Exception occurred during discovery via path: " + toChain(key), null);
            }

            throwable.addSuppressed(e);
          }
        }
      }

      if(throwable == null) {
        requiredKeys.clear();

        return addInjectables(injectables);
      }

      throw throwable;
    }

    private boolean discoverBindings() {
      for(Iterator<Entry<Key, Binding>> iterator = unresolvedBindings.entrySet().iterator(); iterator.hasNext();) {
        Entry<Key, Binding> entry = iterator.next();
        Key key = entry.getKey();  // this Key is the actual type required, not necessarily the same as Binding#getKey

        iterator.remove();

        if(includingResolver.resolve(key).isEmpty()) {
          try {
            return addInjectables(List.of(attemptCreateInjectable(key)));
          }
          catch(Exception e) {
            encounteredProblems.add("Auto discovery of binding unsuccessful: " + entry.getValue() + ": " + e.getMessage());
          }
        }
      }

      return false;
    }

    private Injectable<?> attemptCreateInjectable(Key key) {
      Type type = key.getType();
      Injectable<?> injectable = classInjectableFactory.create(type);

      if(!injectable.getQualifiers().containsAll(key.getQualifiers())) {
        throw new DefinitionException(Types.raw(type), "found during auto discovery is missing qualifiers required by: [" + key + "]");
      }

      return injectable;
    }

    private String toChain(Key key) {
      if(via.containsKey(key)) {
        return toChain(via.get(key)) + " -> [" + key + "]";
      }

      return "[" + key + "]";
    }
  }

  private class DerivedRegistry implements Registry {
    final List<Injectable<?>> derivedInjectables = new ArrayList<>();

    @Override
    public void add(Field field, Type ownerType) {
      derivedInjectables.add(fieldInjectableFactory.create(field, ownerType));
    }

    @Override
    public void add(Method method, Type ownerType) {
      derivedInjectables.add(methodInjectableFactory.create(method, ownerType));
    }

    @Override
    public void add(Type type) {
      derivedInjectables.add(classInjectableFactory.create(type));
    }
  }

  private static class IncludingResolver implements Resolver<Injectable<?>> {
    final Resolver<Injectable<?>> base;
    final Resolver<Injectable<?>> include;

    IncludingResolver(Resolver<Injectable<?>> base, Resolver<Injectable<?>> include) {
      this.base = base;
      this.include = include;
    }

    @Override
    public Set<Injectable<?>> resolve(Key key) {
      Set<Injectable<?>> set = new HashSet<>(base.resolve(key));

      set.addAll(include.resolve(key));

      return set;
    }
  }
}
