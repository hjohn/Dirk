package hs.ddif.plugins;

import hs.ddif.annotations.Produces;
import hs.ddif.core.api.CandidateRegistry;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.ReflectionUtilsPredicates;

/**
 * Provides methods to scan packages for injectables.
 */
public class ComponentScanner {
  private static final Logger LOGGER = Logger.getLogger(ComponentScanner.class.getName());
  private static final Scanner[] SCANNERS = {
    Scanners.TypesAnnotated,
    Scanners.FieldsAnnotated,
    Scanners.MethodsAnnotated,
    Scanners.ConstructorsAnnotated
  };

  /**
   * Scans the given packages for annotated types and adds them to the given
   * {@link CandidateRegistry}.
   *
   * @param registry a {@link CandidateRegistry} to add found types, cannot be {@code null}
   * @param packageNamePrefixes zero or more package name prefixes to scan
   */
  public static void scan(CandidateRegistry registry, String... packageNamePrefixes) {
    LOGGER.fine("Scanning packages: " + Arrays.toString(packageNamePrefixes));

    Reflections reflections = createReflections(packageNamePrefixes);

    List<Type> types = findComponentTypes(reflections, ComponentScanner.class.getClassLoader());

    LOGGER.fine("Registering types: " + types);

    registry.register(types);
  }

  static List<Type> findComponentTypes(Reflections reflections, ClassLoader classLoader) {
    return
      reflections.get(
        Scanners.TypesAnnotated.with(Named.class, Singleton.class)
          .add(
            Scanners.FieldsAnnotated.with(Inject.class, Produces.class)
              .add(Scanners.MethodsAnnotated.with(Inject.class, Produces.class))
              .add(Scanners.ConstructorsAnnotated.with(Inject.class))
              .map(ComponentScanner::reduceToClassName)
          )
          .asClass(classLoader)
          .filter(ReflectionUtilsPredicates.withClassModifier(Modifier.ABSTRACT).negate())
      )
      .stream()
      .sorted(Comparator.comparing(Type::getTypeName))
      .collect(Collectors.toList());
  }

  static Reflections createReflections(String... packageNamePrefixes) {
    Pattern filterPattern = Pattern.compile(
      Arrays.stream(packageNamePrefixes)
        .map(pnp -> pnp.replace(".", "/"))
        .collect(Collectors.joining("|", "(", ").*?"))
    );

    Configuration configuration = new ConfigurationBuilder()
      .forPackages(packageNamePrefixes)
      .filterInputsBy(s -> filterPattern.matcher(s).matches())
      .setScanners(SCANNERS);

    return new Reflections(configuration);
  }

  static Reflections createReflections(URL... urls) {
    Configuration configuration = new ConfigurationBuilder()
      .addUrls(urls)
      .setScanners(SCANNERS);

    return new Reflections(configuration);
  }

  private static String reduceToClassName(String name) {
    int methodParametersStart = name.lastIndexOf('(');
    int memberNameStart = name.lastIndexOf('.', methodParametersStart == -1 ? name.length() : methodParametersStart);

    return name.substring(0, memberNameStart);
  }
}
