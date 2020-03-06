package hs.ddif.core.store;

import java.util.List;

/**
 * Interface for applying consistency checks on an {@link InjectableStore}.
 *
 * @param <T> the type of {@link Injectable} the store holds
 */
public interface StoreConsistencyPolicy<T extends Injectable> {

  /**
   * Adds the given {@link Injectable}s to this policy.  If the policy would be violated, then an
   * exception should be thrown and the changes rolled back.
   *
   * @param resolver a {@link Resolver}, cannot be null
   * @param injectables a list of injectables to add, cannot be null
   */
  void addAll(Resolver<T> resolver, List<T> injectables);

  /**
   * Removes the given {@link Injectable}s from this policy.  If the policy would be violated, then an
   * exception should be thrown and the changes rolled back.
   *
   * @param resolver a {@link Resolver}, cannot be null
   * @param injectables a list of injectables to remove, cannot be null
   */
  void removeAll(Resolver<T> resolver, List<T> injectables);
}
