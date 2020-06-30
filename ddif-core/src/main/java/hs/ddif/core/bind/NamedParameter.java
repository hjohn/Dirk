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

  public NamedParameter(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public Object getValue() {
    return value;
  }
}
