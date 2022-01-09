package hs.ddif.core.store;

import java.util.Collection;

/**
 * Interface for applying consistency checks on an {@link QualifiedTypeStore}.
 *
 * @param <T> the type of {@link QualifiedType}s the store holds
 */
public interface StoreConsistencyPolicy<T extends QualifiedType> {

  /**
   * Adds the given {@link QualifiedType}s to this policy.  If the policy would be violated, then an
   * exception should be thrown and the changes rolled back.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param qualifiedTypes a collection of {@link QualifiedType}s to add, cannot be {@code null}
   */
  void addAll(Resolver<T> resolver, Collection<T> qualifiedTypes);

  /**
   * Removes the given {@link QualifiedType}s from this policy.  If the policy would be violated, then an
   * exception should be thrown and the changes rolled back.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param qualifiedTypes a collection of {@link QualifiedType}s to remove, cannot be {@code null}
   */
  void removeAll(Resolver<T> resolver, Collection<T> qualifiedTypes);
}
