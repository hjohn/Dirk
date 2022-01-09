package hs.ddif.core.inject.injection;

import java.lang.reflect.AccessibleObject;

/**
 * A target and value to be injected. A target can be a field, method or constructor
 * of the target class, and is left {@code null} when the value is a reference to the
 * owner class when injecting non-static fields or methods.<p>
 *
 * When the target is a method or constructor the order of the injections provided
 * determines the order of the parameters. The target can also be
 */
public class Injection {
  private final AccessibleObject target;
  private final Object value;

  /**
   * Constructs a new instance.
   *
   * @param target an {@link AccessibleObject} which will serve as the target, can be {@code null}
   * @param value a value to inject, can be {@code null}
   */
  public Injection(AccessibleObject target, Object value) {
    this.target = target;
    this.value = value;
  }

  /**
   * Returns the target. If {@code null} the value represents the owner class of a
   * non-static field or method.
   *
   * @return a target, can be {@code null}
   */
  public AccessibleObject getTarget() {
    return target;
  }

  /**
   * Returns the value.
   *
   * @return a value, can be {@code null}
   */
  public Object getValue() {
    return value;
  }
}
