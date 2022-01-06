package hs.ddif.core.inject.instantiator;

import hs.ddif.core.store.Key;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.inject.Qualifier;

/**
 * A parser for untyped criterions.
 */
public class CriteriaParser {
  private final Key key;
  private final List<Predicate<Type>> matchers;

  /**
   * Constructs a new instance. This takes an optional array of criterions which
   * can represent qualifier {@link Annotation}s, extended or implemented {@link Class}es
   * or a custom {@link Predicate}.<p>
   *
   * Annotations which are not meta-annotated with {@link Qualifier}s will be rejected,
   * as will any criterion type that is not supported.
   *
   * @param type a {@link Type}, cannot be null
   * @param criterions an optional array of criterions, containing one or more of {@link Class}, {@link Annotation} or {@link Predicate}
   */
  public CriteriaParser(Type type, Object... criterions) {
    List<Annotation> qualifiers = new ArrayList<>();
    List<Predicate<Type>> matchers = new ArrayList<>();

    for(Object criterion : criterions) {
      if(criterion instanceof Predicate) {
        @SuppressWarnings("unchecked")
        Predicate<Type> predicate = (Predicate<Type>)criterion;

        matchers.add(predicate);
      }
      else if(criterion instanceof Annotation) {
        qualifiers.add(requireQualifier((Annotation)criterion));
      }
      else {
        if(criterion instanceof Class) {
          Class<?> cls = (Class<?>)criterion;

          if(cls.isAnnotation()) {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)cls;

            qualifiers.add(requireQualifier(Annotations.of(annotationClass)));
            continue;
          }
        }

        throw new IllegalArgumentException("Unsupported criterion type, must be Class, Annotation or Matcher: " + criterion);
      }
    }

    this.key = new Key(type, qualifiers);
    this.matchers = Collections.unmodifiableList(matchers);
  }

  /**
   * Gets the {@link Key} which was parsed.
   *
   * @return a {@link Key}, never null
   */
  public Key getKey() {
    return key;
  }

  /**
   * Returns an immutable list of {@link Predicate}s.
   *
   * @return an immutable list of {@link Predicate}s, never {@code null} or contains {@code null}s but can be empty
   */
  public List<Predicate<Type>> getMatchers() {
    return matchers;
  }

  private static Annotation requireQualifier(Annotation annotation) {
    if(annotation.annotationType().getAnnotation(Qualifier.class) == null) {
      throw new IllegalArgumentException("Annotation criterion can only be Qualifiers: " + annotation);
    }

    return annotation;
  }
}
