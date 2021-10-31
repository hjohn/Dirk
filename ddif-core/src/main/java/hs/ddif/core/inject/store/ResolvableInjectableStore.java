package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.Resolver;
import hs.ddif.core.store.StoreConsistencyPolicy;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Enhances an {@link InjectableStore} with optional extensions and auto discovery.
 */
public class ResolvableInjectableStore extends InjectableStore<ResolvableInjectable> {

  /**
   * Allows simple extensions to a {@link ResolvableInjectableStore}.
   */
  public interface Extension {

    /**
     * Returns zero or more suppliers of {@link ResolvableInjectable}s which are
     * derived from the given injectable. For example, the given injectable could have
     * special annotations which supply further injectables. These in turn could
     * require dependencies (as parameters) that may need to be auto discovered first.<p>
     *
     * The suppliers of various extensions may be dependent on each other. The supplier
     * call therefore may be retried if it throws an exception after more resolved
     * injectables become available. A supplier which throws no exception will never
     * be retried. A supplier is allowed to return {@code null} to indicate its
     * injectable is already available.
     *
     * @param resolver a {@link Resolver} for known {@link ResolvableInjectable}s, never null
     * @param injectable a {@link ResolvableInjectable} use a base for derivation, never null
     * @return a list of suppliers of {@link ResolvableInjectable}, never null and never contains nulls
     */
    List<Supplier<ResolvableInjectable>> getDerived(Resolver<ResolvableInjectable> resolver, ResolvableInjectable injectable);
  }

  private final List<Extension> extensions;
  private boolean autoDiscovery;

  /**
   * Constructs a new instance.
   *
   * @param policy a {@link StoreConsistencyPolicy}, can be null
   * @param extensions a list of {@link Extension}s, cannot be null and cannot contain nulls
   * @param autoDiscovery set to {@code true} to enable auto discovery, otherwise leave {@code false}
   */
  public ResolvableInjectableStore(StoreConsistencyPolicy<ResolvableInjectable> policy, List<Extension> extensions, boolean autoDiscovery) {
    super(policy);

    this.extensions = new ArrayList<>(extensions);
    this.autoDiscovery = autoDiscovery;

    if(autoDiscovery) {
      this.extensions.add(new AutoDiscoveryStoreExtension());
    }
  }

  @Override
  public synchronized void putAll(Collection<ResolvableInjectable> injectables) {
    super.putAll(gatherInjectables(injectables));
  }

  @Override
  public synchronized void removeAll(Collection<ResolvableInjectable> injectables) {
    super.removeAll(gatherInjectables(injectables));
  }

  @Override
  public synchronized void put(ResolvableInjectable injectable) {
    putAll(List.of(injectable));
  }

  @Override
  public synchronized void remove(ResolvableInjectable injectable) {
    removeAll(List.of(injectable));
  }

  @Override
  public synchronized Set<ResolvableInjectable> resolve(Type type, Object... criteria) {
    Set<ResolvableInjectable> injectables = super.resolve(type, criteria);

    if(injectables.isEmpty() && autoDiscovery && criteria.length == 0) {
      put(new ClassInjectable(type));

      injectables = super.resolve(type, criteria);
    }

    return injectables;
  }

  private Set<ResolvableInjectable> gatherInjectables(Collection<ResolvableInjectable> inputInjectables) {
    return new Gatherer(inputInjectables).gather();
  }

  class Gatherer {
    private final InjectableStore<ResolvableInjectable> tempStore = new InjectableStore<>();
    private final IncludingResolver includingResolver = new IncludingResolver(ResolvableInjectableStore.super::resolve, tempStore);
    private final Deque<Supplier<ResolvableInjectable>> suppliers = new ArrayDeque<>();

    Gatherer(Collection<ResolvableInjectable> inputInjectables) {
      inputInjectables.forEach(this::add);
    }

    Set<ResolvableInjectable> gather() {
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
          suppliers.addAll(extension.getDerived(includingResolver, injectable));
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
    public Set<ResolvableInjectable> resolve(Type type, Object... criteria) {
      Set<ResolvableInjectable> set = new HashSet<>(base.resolve(type, criteria));

      set.addAll(include.resolve(type, criteria));

      return set;
    }
  }
}
