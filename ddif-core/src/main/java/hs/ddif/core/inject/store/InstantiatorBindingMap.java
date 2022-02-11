package hs.ddif.core.inject.store;

import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.instantiation.InstantiatorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps {@link Binding}s to their associated {@link Instantiator}s.
 */
public class InstantiatorBindingMap {

  /**
   * Maps bindings to instantiators. A binding is mostly unique but
   * a binding to a field in a super class can be duplicated amongst child
   * classes. A weak hash map can't be used here as the bindings are different
   * instances (even though they're equal) but will still get removed if one
   * of them gets GC'd, removing the entire entry despite an equal binding still
   * existing.
   */
  private final Map<Binding, Reference<Instantiator<?>>> instantiators = new HashMap<>();

  private final InstantiatorFactory instantiatorFactory;

  /**
   * Constructs a new instance.
   *
   * @param instantiatorFactory an {@link InstantiatorFactory} cannot be {@code null}
   */
  public InstantiatorBindingMap(InstantiatorFactory instantiatorFactory) {
    this.instantiatorFactory = instantiatorFactory;
  }

  /**
   * Returns the {@link InstantiatorFactory} used by this map.
   *
   * @return an {@link InstantiatorFactory} used by this map, never {@code null}
   */
  public InstantiatorFactory getInstantiatorFactory() {
    return instantiatorFactory;
  }

  /**
   * Find an {@link Instantiator} associated with the given {@link Binding}.
   *
   * @param <T> the expected type
   * @param binding a {@link Binding}, cannot be {@code null}
   * @return an {@link Instantiator} or {@code null} if the binding matched none
   */
  public <T> Instantiator<T> getInstantiator(Binding binding) {
    Reference<Instantiator<?>> reference = instantiators.get(binding);

    if(reference == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    Instantiator<T> referent = (Instantiator<T>)reference.getReferent();

    return referent;
  }

  /**
   * Associates a {@link Binding} with an {@link Instantiator}.
   *
   * @param binding a {@link Binding} to associate, cannot be {@code null}
   */
  public void addBinding(Binding binding) {
    Reference<Instantiator<?>> ref = instantiators.get(binding);

    if(ref == null) {
      instantiators.put(binding, new Reference<>(instantiatorFactory.getInstantiator(binding.getKey(), binding.getAnnotatedElement())));
    }
    else {
      ref.increaseReferences();
    }
  }

  /**
   * Removes a {@link Binding} from the map.
   *
   * @param binding a {@link Binding} to remove, cannot be {@code null}
   */
  public void removeBinding(Binding binding) {
    Reference<Instantiator<?>> ref = instantiators.get(binding);

    if(ref == null) {
      throw new AssertionError("More bindings were removed than added: " + binding);
    }

    if(ref.decreaseReferences()) {
      instantiators.remove(binding);
    }
  }

  private static class Reference<T> {
    final T referent;

    int count = 1;

    Reference(T referent) {
      this.referent = referent;
    }

    T getReferent() {
      return referent;
    }

    void increaseReferences() {
      count++;
    }

    boolean decreaseReferences() {
      return --count == 0;
    }
  }

}
