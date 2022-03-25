package hs.ddif.plugins;

import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.Store;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.QueryFunction;

/**
 * A factory for {@link ComponentScanner}s which can be configured with relevant
 * annotations to scan for.
 */
public class ComponentScannerFactory {
  private static final Scanner[] SCANNERS = {
    Scanners.TypesAnnotated,
    Scanners.FieldsAnnotated,
    Scanners.MethodsAnnotated,
    Scanners.ConstructorsAnnotated
  };

  private final AnnotatedElement[] typeAnnotations;
  private final AnnotatedElement[] fieldAnnotations;
  private final AnnotatedElement[] methodAnnotations;
  private final AnnotatedElement[] constructorAnnotations;
  private final Predicate<Class<?>> filter;

  /**
   * Constructs a new instance.
   *
   * @param typeAnnotations an array of annotations to scan for on types, cannot be {@code null} or contain {@code null}s but can be empty
   * @param fieldAnnotations an array of annotations to scan for on fields, cannot be {@code null} or contain {@code null}s but can be empty
   * @param methodAnnotations an array of annotations to scan for on methods, cannot be {@code null} or contain {@code null}s but can be empty
   * @param constructorAnnotations an array of annotations to scan for on constructors, cannot be {@code null} or contain {@code null}s but can be empty
   * @param filter an inclusion filter for the classes detected, can be {@code null} in which case all detected classes are included
   */
  public ComponentScannerFactory(AnnotatedElement[] typeAnnotations, AnnotatedElement[] fieldAnnotations, AnnotatedElement[] methodAnnotations, AnnotatedElement[] constructorAnnotations, Predicate<Class<?>> filter) {
    this.typeAnnotations = typeAnnotations.clone();
    this.fieldAnnotations = fieldAnnotations.clone();
    this.methodAnnotations = methodAnnotations.clone();
    this.constructorAnnotations = constructorAnnotations.clone();
    this.filter = filter == null ? x -> true : filter;
  }

  /**
   * Constructs a new instance.
   *
   * @param typeAnnotations an array of annotations to scan for on types, cannot be {@code null} or contain {@code null}s but can be empty
   * @param fieldAnnotations an array of annotations to scan for on fields, cannot be {@code null} or contain {@code null}s but can be empty
   * @param methodAnnotations an array of annotations to scan for on methods, cannot be {@code null} or contain {@code null}s but can be empty
   * @param constructorAnnotations an array of annotations to scan for on constructors, cannot be {@code null} or contain {@code null}s but can be empty
   */
  public ComponentScannerFactory(AnnotatedElement[] typeAnnotations, AnnotatedElement[] fieldAnnotations, AnnotatedElement[] methodAnnotations, AnnotatedElement[] constructorAnnotations) {
    this(typeAnnotations, fieldAnnotations, methodAnnotations, constructorAnnotations, null);
  }

  /**
   * Creates a new {@link ComponentScanner} to scan for the given packages.
   *
   * @param packageNamePrefixes an array of package name prefixes, cannot be {@code null}
   * @return a {@link ComponentScanner}, never {@code null}
   */
  public ComponentScanner create(String... packageNamePrefixes) {
    Pattern filterPattern = Pattern.compile(
      Arrays.stream(packageNamePrefixes)
        .map(pnp -> pnp.replace(".", "/"))
        .collect(Collectors.joining("|", "(", ").*?"))
    );

    Configuration configuration = new ConfigurationBuilder()
      .forPackages(packageNamePrefixes)
      .filterInputsBy(s -> filterPattern.matcher(s).matches())
      .setScanners(SCANNERS);

    return new ComponentScanner(new Reflections(configuration), getScanDefinition(), filter);
  }

  /**
   * Creates a new {@link ComponentScanner} to scan for the given {@link URL}s.
   *
   * @param urls an array of {@link URL}s, cannot be {@code null}
   * @return a {@link ComponentScanner}, never {@code null}
   */
  public ComponentScanner create(URL... urls) {
    Configuration configuration = new ConfigurationBuilder()
      .addUrls(urls)
      .setScanners(SCANNERS);

    return new ComponentScanner(new Reflections(configuration), getScanDefinition(), filter);
  }

  private QueryFunction<Store, String> getScanDefinition() {
    return Scanners.TypesAnnotated.with(typeAnnotations)
      .add(
        Scanners.FieldsAnnotated.with(fieldAnnotations)
          .add(Scanners.MethodsAnnotated.with(methodAnnotations))
          .add(Scanners.ConstructorsAnnotated.with(constructorAnnotations))
          .map(ComponentScannerFactory::reduceToClassName)
      );
  }

  private static String reduceToClassName(String name) {
    int methodParametersStart = name.lastIndexOf('(');
    int memberNameStart = name.lastIndexOf('.', methodParametersStart == -1 ? name.length() : methodParametersStart);

    return name.substring(0, memberNameStart);
  }

}
