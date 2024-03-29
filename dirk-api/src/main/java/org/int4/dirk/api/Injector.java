package org.int4.dirk.api;

/**
 * An injector is a combination of a {@link CandidateRegistry} and an {@link InstanceResolver},
 * providing the functionality of both interfaces in a single type.
 *
 * <p>An injector provides instances of classes or interfaces which have been registered with
 * it or which can be auto discovered (if enabled). Each instance returned is injected with
 * further dependencies if any of its fields or constructors are annotated with an inject annotation.
 *
 * <p>The potential candidates for injection can be registered with the injector in various ways,
 * for example by providing classes or existing instances. In order to successfully register a type,
 * all its dependencies must be met as well, and all dependencies it provides must not conflict with
 * any types already registered. If a conflict or cycle is detected during registration, an
 * exception will be thrown.
 *
 * <p>For example, consider an injector for which a class A and B are registered and where class A has a
 * field that requires an instance of class B. Any of the following actions will throw an exception:
 *
 * <ul>
 * <li>Removing class B; this would make construction of A impossible as B is a requirement.</li>
 * <li>Registering a subclass of B; this would make construction of A ambiguous as there are two
 *     possible candidates for the required B.</li>
 * <li>Registering a class which either provides or produces instances of B (or a subclass); again
 *     this would make construction of A ambiguous.</li>
 * </ul>
 */
public interface Injector extends CandidateRegistry, InstanceResolver {

  /**
   * Returns an {@link InstanceResolver}, which can be shared instead of this class
   * to share only methods that can be used to instantiate objects.
   *
   * @return an {@link InstanceResolver}, never {@code null}
   */
  InstanceResolver getInstanceResolver();

  /**
   * Returns a {@link CandidateRegistry}, which can be shared instead of this class
   * to share only methods that can be used to register and remove objects.
   *
   * @return a {@link CandidateRegistry}, never {@code null}
   */
  CandidateRegistry getCandidateRegistry();
}
