package hs.ddif.core;

import java.util.Set;

/**
 * Interface for applying consistency checks on an {@link InjectableStore}.
 */
public interface StoreConsistencyPolicy {

  /**
   * Called when an attempt is made to add a new {@link Injectable} to the store.  Implementors
   * can prevent the addition by throwing an exception.
   *
   * @param injectableStore the {@link InjectableStore}
   * @param injectable the injectable being considered for addition to the store
   * @param qualifiers the qualifiers of the injectable
   */
  void checkAddition(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers);

  /**
   * Called when an attempt is made to remove an {@link Injectable} from the store.  Implementors
   * can prevent the removal by throwing an exception.
   *
   * @param injectableStore the {@link InjectableStore}
   * @param injectable the injectable being considered for removal from the store
   * @param qualifiers the qualifiers of the injectable
   */
  void checkRemoval(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers);
}
