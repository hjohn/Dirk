package org.int4.dirk.library;

import java.util.List;

import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.Resolution;
import org.int4.dirk.util.TypeVariables;

/**
 * An {@link InjectionTargetExtension} for {@link List}s.
 *
 * @param <T> the type of element in the collection
 */
public class ListInjectionTargetExtension<T> extends InjectionTargetExtension<List<T>, T> {

  /**
   * Creates a new instance.
   */
  public ListInjectionTargetExtension() {
    super(TypeVariables.get(List.class, 0), Resolution.EAGER_ANY, context -> context.createAll());
  }

}
