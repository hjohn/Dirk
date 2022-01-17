package hs.ddif.core.config.standard;

import hs.ddif.core.config.gather.DiscoveryFailure;
import hs.ddif.core.config.gather.Gatherer;
import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.injectable.ClassInjectableFactory;
import hs.ddif.core.inject.injectable.DefinitionException;
import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.store.Resolver;
import hs.ddif.core.util.Types;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * An implementation of {@link Gatherer} which optionally does auto discovery
 * through bindings when gathering injectables for a {@link Type}.
 */
public class AutoDiscoveringGatherer implements Gatherer {

  /**
   * Allows simple extensions to a {@link AutoDiscoveringGatherer}.
   */
  public interface Extension {

    /**
     * Returns zero or more {@link Injectable}s which are derived from the
     * given {@link Type}. For example, the given type could have special
     * annotations which supply further injectables. These in turn could require
     * dependencies (as parameters) that may need to be auto discovered first.
     *
     * @param type a {@link Type} use as base for derivation, never {@code null}
     * @return a list of {@link Injectable}, never {@code null} and never contains {@code null}s
     */
    List<Injectable> getDerived(Type type);
  }

  private final boolean autoDiscovery;
  private final List<Extension> extensions;
  private final ClassInjectableFactory classInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param extensions a list of {@link Extension}s, cannot be {@code null} or contain {@code null}s but can be empty
   * @param autoDiscovery {@code true} when auto discovery should be used, otherwise {@code false}
   * @param classInjectableFactory a {@link ClassInjectableFactory}, cannot be {@code null}
   */
  public AutoDiscoveringGatherer(boolean autoDiscovery, List<Extension> extensions, ClassInjectableFactory classInjectableFactory) {
    this.autoDiscovery = autoDiscovery;
    this.extensions = extensions;
    this.classInjectableFactory = classInjectableFactory;
  }

  @Override
  public Set<Injectable> gather(Resolver<Injectable> resolver, List<Type> types) {  // used during normal registration
    Executor<DefinitionException> executor = new Executor<>(resolver, DefinitionException::new);

    return executor.execute(types.stream().map(Key::new).collect(Collectors.toList()));
  }

  @Override
  public Set<Injectable> gather(Resolver<Injectable> resolver, Injectable injectable) {  // used for registering instances
    Executor<DefinitionException> executor = new Executor<>(resolver, DefinitionException::new);

    return executor.execute(injectable);
  }

  @Override
  public Set<Injectable> gather(Resolver<Injectable> resolver, Key key) throws DiscoveryFailure {  // used during instantiation
    if(!autoDiscovery || !resolver.resolve(key).isEmpty()) {
      return Set.of();
    }

    Executor<DiscoveryFailure> executor = new Executor<>(resolver, DiscoveryFailure::new);

    return executor.execute(List.of(key));
  }

  class Executor<T extends Exception> {
    private final QualifiedTypeStore<Injectable> tempStore = new QualifiedTypeStore<>();
    private final IncludingResolver includingResolver;

    /**
     * When auto discovery is on, keeps track of unresolved bindings. A linked hash map
     * is used to get deterministic behavior for the exception messages.
     */
    private final Map<Binding, Exception> unresolvedBindings = new LinkedHashMap<>();

    /**
     * Keys that must always be discovered without the use of auto discovery through
     * bindings.
     */
    private final List<Key> requiredKeys = new ArrayList<>();

    private final Map<Key, Key> via = new HashMap<>();
    private final Set<Type> visitedTypes = new HashSet<>();
    private final Set<Type> visitTypes = new HashSet<>();
    private final BiFunction<String, Exception, T> exceptionFactory;

    Executor(Resolver<Injectable> resolver, BiFunction<String, Exception, T> exceptionFactory) {
      this.includingResolver = new IncludingResolver(resolver::resolve, tempStore);
      this.exceptionFactory = exceptionFactory;
    }

    Set<Injectable> execute(List<Key> keys) throws T {
      for(Key key : keys) {
        visitTypes.add(key.getType());
        requiredKeys.add(key);
      }

      return gather();
    }

    Set<Injectable> execute(Injectable injectable) throws T {
      tempStore.put(injectable);
      visitTypes.add(injectable.getType());

      return gather();
    }

    private String toChain(Binding binding) {
      if(via.containsKey(binding.getKey())) {
        return toChain(via.get(binding.getKey())) + " -> " + binding;
      }

      return "Path " + binding.toString();
    }

    private String toChain(Key key) {
      if(via.containsKey(key)) {
        return toChain(via.get(key)) + " -> " + key;
      }

      return "Path " + key.toString();
    }

    private Set<Injectable> gather() throws T {

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

    private boolean addInjectables(List<Injectable> injectables) {
      if(injectables.isEmpty()) {
        return false;
      }

      tempStore.putAll(injectables);

      for(Injectable injectable : injectables) {
        Key injectableKey = new Key(injectable.getType(), injectable.getQualifiers());

        visitTypes.add(injectable.getType());

        for(Binding binding : injectable.getBindings()) {
          Key key = binding.getKey();

          if(!binding.isCollection() && !binding.isOptional() && includingResolver.resolve(key).isEmpty()) {
            via.put(key, injectableKey);
            visitTypes.add(key.getType());

            if(autoDiscovery) {
              unresolvedBindings.put(binding, null);
            }
          }
        }
      }

      return true;
    }

    private boolean discoverViaExtensions() {
      List<Injectable> extensionDiscoveries = new ArrayList<>();

      for(Type type : visitTypes) {
        if(visitedTypes.add(type)) {
          for(Extension extension : extensions) {
            extensionDiscoveries.addAll(extension.getDerived(type));  // extension don't necessarily resolve the type examined; they might though through for example a static producer which produces itself
          }
        }
      }

      visitTypes.clear();

      return addInjectables(extensionDiscoveries);
    }

    private boolean discoverInputTypes() throws T {
      List<Injectable> injectables = new ArrayList<>();
      T throwable = null;

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
              throwable = exceptionFactory.apply(toChain(key) + ": " + e.getMessage(), e);
            }
            else {
              throwable.addSuppressed(new DefinitionException(toChain(key) + ": " + e.getMessage(), e));
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

    private boolean discoverBindings() throws T {
      for(Iterator<Binding> iterator = unresolvedBindings.keySet().iterator(); iterator.hasNext();) {
        Binding binding = iterator.next();
        Key key = binding.getKey();

        if(!includingResolver.resolve(key).isEmpty()) {
          iterator.remove();
        }
        else {
          try {
            Injectable injectable = attemptCreateInjectable(binding);

            iterator.remove();  // only remove if creation was successful

            return addInjectables(List.of(injectable));
          }
          catch(Exception e) {
            unresolvedBindings.put(binding, e);  // add the exception
          }
        }
      }

      T throwable = null;

      for(Binding binding : unresolvedBindings.keySet()) {
        Exception exception = unresolvedBindings.get(binding);

        if(throwable == null) {
          throwable = exceptionFactory.apply(toChain(binding) + ": " + exception.getMessage(), exception);
        }
        else {
          throwable.addSuppressed(new DefinitionException(toChain(binding) + ": " + exception.getMessage(), exception));
        }
      }

      if(throwable == null) {
        return false;
      }

      throw throwable;
    }

    private Injectable attemptCreateInjectable(Key key) {
      Type type = key.getType();
      Injectable injectable = classInjectableFactory.create(type);

      if(!injectable.getQualifiers().containsAll(key.getQualifiers())) {
        throw new DefinitionException(Types.raw(type), "found during auto discovery is missing qualifiers required by: " + key);
      }

      return injectable;
    }

    private Injectable attemptCreateInjectable(Binding binding) {
      Key key = binding.getKey();
      Type type = key.getType();
      Injectable injectable = classInjectableFactory.create(type);

      if(!injectable.getQualifiers().containsAll(key.getQualifiers())) {
        throw new DefinitionException(Types.raw(binding.getType()), "found during auto discovery is missing qualifiers required by: " + binding);
      }

      return injectable;
    }
  }

  private static class IncludingResolver implements Resolver<Injectable> {
    final Resolver<Injectable> base;
    final Resolver<Injectable> include;

    IncludingResolver(Resolver<Injectable> base, Resolver<Injectable> include) {
      this.base = base;
      this.include = include;
    }

    @Override
    public Set<Injectable> resolve(Key key) {
      Set<Injectable> set = new HashSet<>(base.resolve(key));

      set.addAll(include.resolve(key));

      return set;
    }
  }
}
