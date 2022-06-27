package org.int4.dirk.library;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.Resolution;
import org.int4.dirk.util.TypeVariables;

/**
 * An {@link InjectionTargetExtension} for {@link Set}s.
 *
 * @param <T> the type of element in the collection
 */
public class SetInjectionTargetExtension<T> extends InjectionTargetExtension<Set<T>, T> {

  /**
   * Creates a new instance.
   */
  public SetInjectionTargetExtension() {
    super(TypeVariables.get(Set.class, 0), Resolution.EAGER_ANY, context -> {
      List<T> instances = context.createAll();

      return instances == null ? null : new HashSet<>(instances);
    });
  }
}
