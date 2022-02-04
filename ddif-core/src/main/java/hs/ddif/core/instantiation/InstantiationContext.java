package hs.ddif.core.instantiation;

import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.Resolver;

/**
 * Context used to resolve {@link Key}s to {@link Injectable}s and create instances
 * from them.
 */
public interface InstantiationContext extends Resolver<Injectable> {

  /**
   * Creates an instance of the given {@link Injectable}. It is possible this
   * call returns {@code null}, for example when a provider or producer method
   * returns {@code null}.
   *
   * @param <T> the expected type
   * @param injectable an {@link Injectable}, cannot be {@code null}
   * @return an instance of the given {@link Injectable}, can be {@code null}
   * @throws InstanceCreationFailure when an instance could not be created
   */
  <T> T createInstance(Injectable injectable) throws InstanceCreationFailure;

  /**
   * Creates an instance of the given {@link Injectable}. This call returns {@code null}
   * if the required scope is currently not active. It is also possible this
   * call returns {@code null}, for example when a provider or producer method
   * returns {@code null}.
   *
   * @param <T> the expected type
   * @param injectable an {@link Injectable}, cannot be {@code null}
   * @return an instance of the given {@link Injectable}, can be {@code null}
   * @throws InstanceCreationFailure when an instance could not be created
   */
  <T> T createInstanceInScope(Injectable injectable) throws InstanceCreationFailure;
}

