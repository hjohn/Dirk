package org.int4.dirk.plugins;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.int4.dirk.api.CandidateRegistry;
import org.int4.dirk.api.definition.AutoDiscoveryException;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.api.definition.DependencyException;
import org.reflections.Reflections;
import org.reflections.Store;
import org.reflections.util.QueryFunction;
import org.reflections.util.ReflectionUtilsPredicates;

/**
 * Provides methods to scan packages for injectables.
 */
public class ComponentScanner {
  private static final Logger LOGGER = Logger.getLogger(ComponentScanner.class.getName());

  private final Reflections reflections;
  private final QueryFunction<Store, String> scanDefinition;
  private final Predicate<Class<?>> filter;

  ComponentScanner(Reflections reflections, QueryFunction<Store, String> scanDefinition, Predicate<Class<?>> filter) {
    this.reflections = reflections;
    this.scanDefinition = scanDefinition;
    this.filter = filter;
  }

  /**
   * Scans for annotated types and adds them to the given {@link CandidateRegistry}.
   *
   * @param registry a {@link CandidateRegistry} to add found types, cannot be {@code null}
   * @throws AutoDiscoveryException when auto discovery fails to find all required types
   * @throws DefinitionException when a definition problem was encountered
   * @throws DependencyException when dependencies between registered types cannot be resolved
   */
  public void scan(CandidateRegistry registry) throws AutoDiscoveryException, DefinitionException, DependencyException {
    List<Type> types = findComponentTypes(ComponentScanner.class.getClassLoader());

    LOGGER.fine("Registering types: " + types);

    registry.register(types);
  }

  List<Type> findComponentTypes(ClassLoader classLoader) {
    return
      reflections.get(
        scanDefinition
          .asClass(classLoader)
          .filter(ReflectionUtilsPredicates.withClassModifier(Modifier.ABSTRACT).negate())
      )
      .stream()
      .filter(filter)
      .sorted(Comparator.comparing(Type::getTypeName))
      .collect(Collectors.toList());
  }
}
