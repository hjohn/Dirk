package hs.ddif.core.store;

import hs.ddif.core.api.Matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents additional criteria which can be provided when interacting with
 * an {@link InjectableStore} to further filter the resulting {@link Injectable}s.
 */
public class Criteria {

  /**
   * An empty {@link Criteria} instance.
   */
  public static final Criteria EMPTY = new Criteria(Set.of(), Set.of());

  private final Set<Class<?>> interfaces;
  private final List<Matcher> matchers;

  /**
   * Constructs a new instance.
   *
   * @param interfaces a collection of classes and interfaces that must be extended or implemented, cannot be null or contain nulls but can be empty
   * @param matchers a collection of {@link Matcher}s, cannot be null or contain nulls but can be empty
   */
  public Criteria(Collection<Class<?>> interfaces, Collection<Matcher> matchers) {
    this.interfaces = Collections.unmodifiableSet(new HashSet<>(interfaces));
    this.matchers = Collections.unmodifiableList(new ArrayList<>(matchers));
  }

  /**
   * Gets the classes and interfaces that be must be extended or implemented.
   *
   * @return a set of classes and interfaces that be must be extended or implemented, never null and never contains nulls but can be empty
   */
  public Set<Class<?>> getInterfaces() {
    return interfaces;
  }

  /**
   * Gets the {@link Matcher}s.
   *
   * @return a set of {@link Matcher}s, never null and never contains nulls but can be empty
   */
  public List<Matcher> getMatchers() {
    return matchers;
  }

  @Override
  public int hashCode() {
    return Objects.hash(interfaces, matchers);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Criteria other = (Criteria)obj;

    return Objects.equals(interfaces, other.interfaces) && Objects.equals(matchers, other.matchers);
  }
}
