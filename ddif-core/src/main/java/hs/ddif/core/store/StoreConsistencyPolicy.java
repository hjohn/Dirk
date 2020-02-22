package hs.ddif.core.store;

import hs.ddif.core.util.AnnotationDescriptor;

import java.util.Set;

/**
 * Interface for applying consistency checks on an {@link InjectableStore}.
 *
 * @param <T> the type of {@link Injectable} the store holds
 */
public interface StoreConsistencyPolicy<T extends Injectable> {

  /**
   * Called when an attempt is made to add a new {@link Injectable} to the store.  Implementors
   * can prevent the addition by throwing an exception.
   *
   * @param injectableStore the {@link InjectableStore}
   * @param injectable the injectable being considered for addition to the store
   * @param qualifiers the qualifiers of the injectable
   */
  void checkAddition(InjectableStore<T> injectableStore, T injectable, Set<AnnotationDescriptor> qualifiers);

  /**
   * Called when an attempt is made to remove an {@link Injectable} from the store.  Implementors
   * can prevent the removal by throwing an exception.
   *
   * @param injectableStore the {@link InjectableStore}
   * @param injectable the injectable being considered for removal from the store
   * @param qualifiers the qualifiers of the injectable
   */
  void checkRemoval(InjectableStore<T> injectableStore, T injectable, Set<AnnotationDescriptor> qualifiers);

  /**
   * Called to indicate the given {@link Injectable} was added to the store.  Always call
   * {@link #checkAddition(InjectableStore, Injectable, Set)} first before adding.
   *
   * @param injectable the injectable that was added to the store
   */
  void add(T injectable);

  /**
   * Called to indicate the given {@link Injectable} was removed from the store.  Always
   * call {@link #checkRemoval(InjectableStore, Injectable, Set)} first before removing.
   *
   * @param injectable the injectable that was remove from the store
   */
  void remove(T injectable);
}
