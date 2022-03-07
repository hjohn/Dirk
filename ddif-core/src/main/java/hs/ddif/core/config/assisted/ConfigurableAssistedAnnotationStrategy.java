package hs.ddif.core.config.assisted;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of {@link AssistedAnnotationStrategy} which can be configured with
 * custom annotations and which extract argument names via the given argument annotation
 * or determines them by parameter, field or method name.
 *
 * @param <A> the argument annotation type
 * @param <P> the provider type
 */
public class ConfigurableAssistedAnnotationStrategy<A extends Annotation, P> implements AssistedAnnotationStrategy<P> {
  private static final Pattern GETTER = Pattern.compile("get([A-Z])(.*)");

  private final Class<? extends Annotation> assisted;
  private final Class<A> argument;
  private final Function<AnnotatedElement, String> argumentNameGetter;
  private final Annotation inject;
  private final Class<P> provider;
  private final Function<P, Object> providerGetter;

  /**
   * Constructs a new instance.
   *
   * @param assisted an assisted annotation {@link Class} to use as marker annotation, cannot be {@code null}
   * @param argument an argument annotation {@link Class} to use to mark arguments, cannot be {@code null}
   * @param argumentNameGetter a getter {@link Function} to extract the argument name from an argument annotation, cannot be {@code null}
   * @param inject an inject {@link Annotation} supported by the associated injector to mark producers with, cannot be {@code null}
   * @param provider a provider class supported by the associated injector to use for producer fields, cannot be {@code null}
   * @param providerGetter a getter {@link Function} to extract the value from the provider class, cannot be {@code null}
   */
  public ConfigurableAssistedAnnotationStrategy(Class<? extends Annotation> assisted, Class<A> argument, Function<AnnotatedElement, String> argumentNameGetter, Annotation inject, Class<P> provider, Function<P, Object> providerGetter) {
    this.assisted = Objects.requireNonNull(assisted, "assisted cannot be null");
    this.argument = Objects.requireNonNull(argument, "argument cannot be null");
    this.argumentNameGetter = Objects.requireNonNull(argumentNameGetter, "argumentNameGetter cannot be null");
    this.inject = Objects.requireNonNull(inject, "inject cannot be null");
    this.provider = Objects.requireNonNull(provider, "provider cannot be null");
    this.providerGetter = Objects.requireNonNull(providerGetter, "providerGetter cannot be null");
  }

  @Override
  public Class<? extends Annotation> assistedAnnotationClass() {
    return assisted;
  }

  @Override
  public boolean isArgument(AnnotatedElement annotatedElement) {
    return annotatedElement.isAnnotationPresent(argument);
  }

  @Override
  public String determineArgumentName(AccessibleObject accessibleObject) {
    String name = argumentNameGetter.apply(accessibleObject);

    return name != null && !name.isEmpty() ? name
      : accessibleObject instanceof Field ? ((Field)accessibleObject).getName()
      : stripMethodName(((Method)accessibleObject).getName());
  }

  @Override
  public String determineArgumentName(Parameter parameter) throws MissingArgumentException {
    String name = argumentNameGetter.apply(parameter);

    if((name == null || name.isEmpty()) && !parameter.isNamePresent()) {
      throw new MissingArgumentException("Unable to determine argument name for [" + parameter + "]; specify one with " + argument + " or compile classes with parameter name information");
    }

    return name != null && !name.isEmpty() ? name : parameter.getName();
  }

  @Override
  public Annotation injectAnnotation() {
    return inject;
  }

  @Override
  public Class<P> providerClass() {
    return provider;
  }

  @Override
  public Object provision(P provider) {
    return providerGetter.apply(provider);
  }

  private static final String stripMethodName(String name) {
    Matcher matcher = GETTER.matcher(name);

    if(matcher.matches()) {
      return matcher.group(1).toLowerCase() + matcher.group(2);
    }

    return name;
  }
}
