package org.int4.dirk.spi.discovery;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.int4.dirk.api.definition.DefinitionException;

/**
 * An extension called during registration of types to derive and register 
 * additional types for a given type.
 */
public interface TypeRegistrationExtension {

  /**
   * Allows registration of newly derived types.
   */
  interface Registry {

    /**
     * Adds a derived type given a {@link Field} and the field's owner {@link Type}.
     *
     * @param field a {@link Field}, cannot be {@code null}
     * @param ownerType an owner {@link Type}, cannot be {@code null}
     * @throws DefinitionException when a definition problem was encountered during registration
     */
    void add(Field field, Type ownerType) throws DefinitionException;

    /**
     * Adds a derived type given a {@link Method} and the method's owner {@link Type}.
     *
     * @param method a {@link Method}, cannot be {@code null}
     * @param ownerType an owner {@link Type}, cannot be {@code null}
     * @throws DefinitionException when a definition problem was encountered during registration
     */
    void add(Method method, Type ownerType) throws DefinitionException;

    /**
     * Adds a derived type given a {@link Type}.
     *
     * @param type a {@link Type} to add, cannot be {@code null}
     * @throws DefinitionException when a definition problem was encountered during registration
     */
    void add(Type type) throws DefinitionException;
  }

  /**
   * Called during registration of new types to allow the extension to register further
   * types that can be directly derived from the given {@link Type}. For example, the given
   * type could have special annotations which define further types.
   *
   * @param type a {@link Type} used as a base for derivation, never {@code null}
   * @param registry a {@link Registry} where derived types can be registered, never {@code null}
   * @throws DefinitionException when a definition problem was encountered during derivation
   */
  void deriveTypes(Registry registry, Type type) throws DefinitionException;
}