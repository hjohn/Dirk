package hs.ddif.plugins;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Interface implemented by {@code PluginModule} classes which supplies which
 * annotated types are part of a jar is loaded at runtime.<p>
 *
 * Jars that wish to make use of this mechanism (which avoids scanning the jar)
 * should place a class named {@code PluginModule} implementing this interface
 * in their default package.
 */
public interface Module {

  /**
   * Returns a list of {@link Type}s that should be loaded as part of this module.
   *
   * @return a list of {@link Type}s that should be loaded as part of this module, never null and never contains nulls
   */
  List<Type> getTypes();
}
