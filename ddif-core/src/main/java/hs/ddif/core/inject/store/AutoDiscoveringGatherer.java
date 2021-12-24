package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.DiscoveryFailure;
import hs.ddif.core.inject.instantiator.Gatherer;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An implementation of {@link Gatherer} which optionally does auto discovery
 * when gathering injectables for a {@link Type}.
 */
public class AutoDiscoveringGatherer implements Gatherer {

  /**
   * Allows simple extensions to a {@link AutoDiscoveringGatherer}.
   */
  public interface Extension {

    /**
     * Returns zero or more {@link ResolvableInjectable}s which are derived from the
     * given injectable. For example, the given injectable could have special
     * annotations which supply further injectables. These in turn could require
     * dependencies (as parameters) that may need to be auto discovered first.
     *
     * @param injectable a {@link ResolvableInjectable} use as base for derivation, never null
     * @return a list of {@link ResolvableInjectable}, never null and never contains nulls
     */
    List<ResolvableInjectable> getDerived(ResolvableInjectable injectable);
  }

  private final boolean autoDiscovery;
  private final List<Extension> extensions;
  private final ClassInjectableFactory classInjectableFactory;

  /**
   * Constructs a new instance.
   *
   * @param extensions a list of {@link Extension}s, cannot be null or contain null but can be empty
   * @param autoDiscovery {@code true} when auto discovery should be used, otherwise {@code false}
   * @param classInjectableFactory a {@link ClassInjectableFactory}, cannot be null
   */
  public AutoDiscoveringGatherer(boolean autoDiscovery, List<Extension> extensions, ClassInjectableFactory classInjectableFactory) {
    this.autoDiscovery = autoDiscovery;
    this.extensions = extensions;
    this.classInjectableFactory = classInjectableFactory;
  }

  @Override
  public Set<ResolvableInjectable> gather(Resolver<ResolvableInjectable> resolver, Collection<ResolvableInjectable> inputInjectables) {
    return new Executor(resolver, inputInjectables).executor();
  }

  @Override
  public Set<ResolvableInjectable> gather(Resolver<ResolvableInjectable> resolver, Key key) throws DiscoveryFailure {
    if(!autoDiscovery || !resolver.resolve(key).isEmpty()) {
      return Set.of();
    }

    try {
      ResolvableInjectable injectable = classInjectableFactory.create(key.getType());

      if(injectable.getQualifiers().containsAll(key.getQualifiers())) {
        return new Executor(resolver, List.of(injectable)).executor();
      }

      return Set.of();
    }
    catch(Exception e) {
      throw new DiscoveryFailure(key, "Exception during auto discovery", e);
    }
  }

  class Executor {
    private final InjectableStore<ResolvableInjectable> tempStore = new InjectableStore<>();
    private final Deque<Supplier<ResolvableInjectable>> suppliers = new ArrayDeque<>();
    private final IncludingResolver includingResolver;

    Executor(Resolver<ResolvableInjectable> resolver, Collection<ResolvableInjectable> inputInjectables) {
      this.includingResolver = new IncludingResolver(resolver::resolve, tempStore);

      inputInjectables.forEach(this::add);
    }

    Set<ResolvableInjectable> executor() {
      List<RuntimeException> surpressedExceptions = new ArrayList<>();

      while(surpressedExceptions.size() < suppliers.size()) {
        Supplier<ResolvableInjectable> supplier = suppliers.removeFirst();

        try {
          add(supplier.get());
          surpressedExceptions.clear();
        }
        catch(RuntimeException e) {
          suppliers.add(supplier);  // retry
          surpressedExceptions.add(e);
        }
      }

      if(surpressedExceptions.isEmpty()) {
        return tempStore.toSet();
      }

      if(surpressedExceptions.size() == 1) {
        throw surpressedExceptions.get(0);
      }

      BindingException bindingException = new BindingException("Unable to resolve " + surpressedExceptions.size() + " binding(s) while processing extensions");

      surpressedExceptions.forEach(bindingException::addSuppressed);

      throw bindingException;
    }

    private void add(ResolvableInjectable injectable) {
      if(injectable != null) {
        tempStore.put(injectable);

        for(Extension extension : extensions) {
          suppliers.addAll(extension.getDerived(injectable).stream().map(i -> (Supplier<ResolvableInjectable>)() -> i).collect(Collectors.toList()));
        }

        if(autoDiscovery) {
          suppliers.addAll(autoDiscover(includingResolver, injectable));
        }
      }
    }
  }

  private static class IncludingResolver implements Resolver<ResolvableInjectable> {
    final Resolver<ResolvableInjectable> base;
    final Resolver<ResolvableInjectable> include;

    IncludingResolver(Resolver<ResolvableInjectable> base, Resolver<ResolvableInjectable> include) {
      this.base = base;
      this.include = include;
    }

    @Override
    public Set<ResolvableInjectable> resolve(Key key) {
      Set<ResolvableInjectable> set = new HashSet<>(base.resolve(key));

      set.addAll(include.resolve(key));

      return set;
    }
  }

  private List<Supplier<ResolvableInjectable>> autoDiscover(Resolver<ResolvableInjectable> resolver, ResolvableInjectable injectable) {
    List<Supplier<ResolvableInjectable>> suppliers = new ArrayList<>();

    for(Binding binding : injectable.getBindings()) {
      if(!binding.isParameter() && !binding.isCollection() && !binding.isOptional() && binding.isDirect()) {
        Key key = binding.getKey();

        suppliers.add(() -> {
          if(!isResolvable(resolver, key)) {
            try {
              return attemptCreateInjectable(key);
            }
            catch(Exception e) {
              throw new BindingException("Unable to inject: " + binding + " with: " + key, e);
            }
          }

          return null;
        });
      }
    }

    return suppliers;
  }

  /*
   * Attempts to create a {@link ClassInjectable} for a given {@link Key}. This will
   * fail if the type is abstract or if the key has any qualifiers as a class injectable
   * cannot have qualifiers.
   */
  private ResolvableInjectable attemptCreateInjectable(Key key) {
    Type type = key.getType();

    if(key.getQualifiers().isEmpty()) {
      return classInjectableFactory.create(type);
    }

    throw new BindingException("Auto discovered class cannot be required to have qualifiers: " + key);
  }

  private static boolean isResolvable(Resolver<ResolvableInjectable> resolver, Key key) {
    return !resolver.resolve(key).isEmpty();
  }
}
