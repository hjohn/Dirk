package hs.ddif.api.definition;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Extensions for discovering additional types given a newly registered type.
 */
public interface DiscoveryExtension {

  /**
   * Allows registration of newly derived types.
   */
  interface Registry {

    /**
     * Adds a derived type given a {@link Field} and the field's owner {@link Type}.
     *
     * @param field a {@link Field}, cannot be {@code null}
     * @param ownerType an owner {@link Type}, cannot be {@code null}
     */
    void add(Field field, Type ownerType);

    /**
     * Adds a derived type given a {@link Method} and the method's owner {@link Type}.
     *
     * @param method a {@link Method}, cannot be {@code null}
     * @param ownerType an owner {@link Type}, cannot be {@code null}
     */
    void add(Method method, Type ownerType);

    /**
     * Adds a derived type given a {@link Type}.
     *
     * @param type a {@link Type} to add, cannot be {@code null}
     */
    void add(Type type);
  }

  /**
   * Called during registration of newly discovered types to allow the extension
   * to register further types that can be directly derived from the given {@link Type}.
   * For example, the given type could have special annotations which supply further
   * types. These in turn could require dependencies (as parameters) that may need to
   * be auto discovered first.
   *
   * @param type a {@link Type} use as base for derivation, never {@code null}
   * @param registry a {@link Registry} where derived types can be registered, never {@code null}
   */
  void deriveTypes(Registry registry, Type type);
}