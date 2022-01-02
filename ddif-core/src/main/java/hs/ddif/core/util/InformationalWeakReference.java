package hs.ddif.core.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * A {@link WeakReference} implementation that caches the value of {@link Object#toString()}
 * of the original referent so it can be used even after the referent is garbage collected.
 *
 * @param <T> the referent type
 */
public class InformationalWeakReference<T> extends WeakReference<T> {
  private final String info;

  /**
   * Constructs a new instance.
   *
   * @param referent a referent
   * @param queue a {@link ReferenceQueue}
   */
  public InformationalWeakReference(T referent, ReferenceQueue<T> queue) {
    super(referent, queue);

    this.info = referent.toString();
  }

  @Override
  public String toString() {
    return info;
  }
}