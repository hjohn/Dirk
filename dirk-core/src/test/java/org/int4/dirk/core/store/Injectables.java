package org.int4.dirk.core.store;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.core.InjectableFactories;
import org.int4.dirk.core.definition.Injectable;

public class Injectables {
  private static final InjectableFactories injectableFactories = new InjectableFactories();

  public static Injectable<Object> create(String text) throws DefinitionException {
    return injectableFactories.forInstance().create(text);
  }
}
