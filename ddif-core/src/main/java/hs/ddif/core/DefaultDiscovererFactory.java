package hs.ddif.core;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.definition.DuplicateDependencyException;
import hs.ddif.core.definition.Binding;
import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.FieldInjectableFactory;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.MethodInjectableFactory;
import hs.ddif.core.discovery.Discoverer;
import hs.ddif.core.discovery.DiscovererFactory;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.store.Resolver;
import hs.ddif.spi.discovery.TypeRegistrationExtension;
import hs.ddif.spi.discovery.TypeRegistrationExtension.Registry;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.Key;
import hs.ddif.util.Types;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
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

  /**
   * Contains a cache of injectables derived with the given {@link TypeRegistrationExtension}s.
   * This must not be static as then it would be shared among multiple injectors which
   * may have a different set of extensions configured.
   */
  private final Map<Type, List<Injectable<?>>> derivedInjectables = new WeakHashMap<>();

  private final boolean autoDiscovery;
  private final Collection<TypeRegistrationExtension> extensions;
  private final InstantiatorFactory instantiatorFactory;
  private final ClassInjectableFactory classInjectableFactory;
  private final MethodInjectableFactory methodInjectableFactory;
  private final FieldInjectableFactory fieldInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param extensions a collection of {@link TypeRegistrationExtension}s, cannot be {@code null} or contain {@code null}s but can be empty
   * @param autoDiscovery {@code true} when auto discovery should be used, otherwise {@code false}
   * @param instantiatorFactory an {@link InstantiatorFactory}, cannot be {@code null}
   * @param classInjectableFactory a {@link ClassInjectableFactory}, cannot be {@code null}
   * @param methodInjectableFactory a {@link MethodInjectableFactory}, cannot be {@code null}
   * @param fieldInjectableFactory a {@link FieldInjectableFactory}, cannot be {@code null}
   */
  public DefaultDiscovererFactory(boolean autoDiscovery, Collection<TypeRegistrationExtension> extensions, InstantiatorFactory instantiatorFactory, ClassInjectableFactory classInjectableFactory, MethodInjectableFactory methodInjectableFactory, FieldInjectableFactory fieldInjectableFactory) {
    this.autoDiscovery = autoDiscovery;
    this.extensions = extensions;
    this.instantiatorFactory = instantiatorFactory;
    this.classInjectableFactory = classInjectableFactory;
    this.methodInjectableFactory = methodInjectableFactory;
    this.fieldInjectableFactory = fieldInjectableFactory;
  }

  @Override
  public Discoverer create(Resolver<Injectable<?>> resolver, Collection<Type> types) {  // used during normal registration
    return new SimpleDiscoverer(resolver, types);
  }

  @Override
  public Discoverer create(Resolver<Injectable<?>> resolver, Injectable<?> injectable) {  // used for registering instances
    return new SimpleDiscoverer(resolver, injectable);
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

    private boolean discoveryCompleted;

    SimpleDiscoverer(Resolver<Injectable<?>> resolver, Collection<Type> types) {
      this.includingResolver = new IncludingResolver(resolver::resolve, tempStore);

      for(Key key : types.stream().map(Key::new).collect(Collectors.toList())) {
        visitTypes.add(key.getType());
        requiredKeys.add(key);
      }
    }

    SimpleDiscoverer(Resolver<Injectable<?>> resolver, Injectable<?> injectable) {
      try {
        this.includingResolver = new IncludingResolver(resolver::resolve, tempStore);

        tempStore.put(injectable);
        visitTypes.add(injectable.getType());
      }
      catch(DuplicateDependencyException e) {
        throw new IllegalStateException("Assertion error", e);
      }
    }

    @Override
    public List<String> getProblems() {
      if(!discoveryCompleted) {
        throw new IllegalStateException("Call discover first");
      }

      return encounteredProblems;
    }

    @Override
    public Set<Injectable<?>> discover() throws DefinitionException {

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

      if(!discoveryCompleted) {
        discoveryCompleted = true;

        while(discoverViaExtensions() || discoverInputTypes() || discoverBindings()) {}
      }

      return tempStore.toSet();
    }

    private boolean addInjectables(List<Injectable<?>> injectables) {
      if(injectables.isEmpty()) {
        return false;
      }

      try {
        tempStore.putAll(injectables);
      }
      catch(DuplicateDependencyException e) {
        throw new IllegalStateException("Assertion error", e);
      }

      for(Injectable<?> injectable : injectables) {
        Key injectableKey = new Key(injectable.getType(), injectable.getQualifiers());

        visitTypes.add(injectable.getType());

        for(Binding binding : injectable.getBindings()) {
          Key key = instantiatorFactory.getInstantiator(binding).getKey();

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

    private boolean discoverViaExtensions() throws DefinitionException {
      List<Injectable<?>> extensionDiscoveries = new ArrayList<>();

      for(Type type : visitTypes) {
        if(visitedTypes.add(type)) {
          if(!derivedInjectables.containsKey(type)) {
            DerivedRegistry registry = new DerivedRegistry();

            for(TypeRegistrationExtension extension : extensions) {
              // extension don't necessarily resolve the type examined; they might though through for example a static producer which produces itself
              extension.deriveTypes(registry, type);
            }

            derivedInjectables.put(type, registry.derivedInjectables);
          }

          extensionDiscoveries.addAll(derivedInjectables.get(type));
        }
      }

      visitTypes.clear();

      return addInjectables(extensionDiscoveries);
    }

    private boolean discoverInputTypes() throws DefinitionException {
      List<Injectable<?>> injectables = new ArrayList<>();
      DefinitionException throwable = null;

      for(Key key : requiredKeys) {
        boolean alreadyDerived = tempStore.contains(key);  // was this key already derived through another mechanism?

        try {
          injectables.add(attemptCreateInjectable(key));  // if alreadyDerived is true and the attempt succeeds, there will be multiple ways of creating this candidate (potentially with different qualifiers)
        }
        catch(DefinitionException e) {
          if(!alreadyDerived) {  // if creation failed but it was derived through other means, ignore the exception, this is acceptable as it is not ambiguous
            if(throwable == null) {
              throwable = e;
            }
            else {
              throwable.addSuppressed(e);  // gather all exceptions that may occur on the input types, to get a complete picture
            }
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
            encounteredProblems.add(toChain(key) + ", via " + entry.getValue() + ", is not registered and cannot be discovered (reason: " + e.getMessage() + (e.getCause() != null ? " because " + e.getCause().getMessage() : "") + ")");
          }
        }
      }

      return false;
    }

    private Injectable<?> attemptCreateInjectable(Key key) throws DefinitionException {
      Type type = key.getType();
      Injectable<?> injectable = classInjectableFactory.create(type);

      if(!injectable.getQualifiers().containsAll(key.getQualifiers())) {
        throw new DefinitionException(Types.raw(type), "is missing the required qualifiers: " + key.getQualifiers());
      }

      return injectable;
    }

    private String toChain(Key key) {
      if(via.containsKey(key)) {
        return "[" + key + "] required by " + toChain(via.get(key));
      }

      return "[" + key + "]";
    }
  }

  private class DerivedRegistry implements Registry {
    final List<Injectable<?>> derivedInjectables = new ArrayList<>();

    @Override
    public void add(Field field, Type ownerType) throws DefinitionException {
      derivedInjectables.add(fieldInjectableFactory.create(field, ownerType));
    }

    @Override
    public void add(Method method, Type ownerType) throws DefinitionException {
      derivedInjectables.add(methodInjectableFactory.create(method, ownerType));
    }

    @Override
    public void add(Type type) throws DefinitionException {
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
