package hs.ddif.core.instantiation;

/**
 * Traits of an {@link Instantiator}. These traits can be used to ensure that a set
 * of injectables can always be correctly resolved by rejecting any additions or
 * removals to such a set that would break these rules.
 */
public enum TypeTrait {

  /**
   * Indicates that the instance produced is a provider which will only resolve the
   * key upon use. This allows for an indirection that will allow avoiding a circular
   * dependency during the construction of instances.
   *
   * <p>Note that this does not allow for the key to be completely unavailable in
   * the underlying store unless this instantiator does not require at least one match.
   * In other words, a lazy dependency allows to break a circular dependency during
   * instantiation, but not during registration.
   */
  LAZY,

  /**
   * Indicates that this instantiator requires at least one dependency matching the key
   * to be available.
   */
  REQUIRES_AT_LEAST_ONE,

  /**
   * Indicates that this instantiator requires at most one dependency matching the key
   * to be available.
   */
  REQUIRES_AT_MOST_ONE
}

