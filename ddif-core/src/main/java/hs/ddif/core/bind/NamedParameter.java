package hs.ddif.core.bind;

import hs.ddif.annotations.Parameter;

import javax.inject.Inject;

/**
 * Represents a parameter that can be injected in a class which has
 * fields annotated with both {@link Inject} and {@link Parameter}.<p>
 *
 * The name of the parameter must match the name of the field or
 * constructor or setter parameter name which was annotated with {@link Parameter}.
 */
public class NamedParameter {
  private final String name;
  private final Object value;

  /**
   * Constructs a new instance.
   *
   * @param name a parameter name, cannot be null or empty
   * @param value a parameter value
   */
  public NamedParameter(String name, Object value) {
    if(name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty: " + name);
    }

    this.name = name;
    this.value = value;
  }

  /**
   * Returns the name of the parameter.
   *
   * @return the name of the parameter, never null or empty
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the value of the parameter.
   *
   * @return the value of the parameter
   */
  public Object getValue() {
    return value;
  }
}
