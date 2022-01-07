package hs.ddif.core.inject.store;

import hs.ddif.annotations.Opt;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.InstanceResolutionFailure;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.store.Key;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Provides {@link Binding}s for constructors, methods and fields.
 */
public class BindingProvider {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);

  private final BindingFactory bindingFactory;

  /**
   * Constructs a new instance.
   *
   * @param bindingFactory a {@link BindingFactory}, cannot be {@code null}
   */
  public BindingProvider(BindingFactory bindingFactory) {
    this.bindingFactory = bindingFactory;
  }

  /**
   * Returns all bindings for the given {@link Constructor} and all member bindings
   * for the given class.
   *
   * @param constructor a {@link Constructor} to examine for bindings, cannot be null
   * @param cls a {@link Class} to examine for bindings, cannot be null
   * @return a list of bindings, never null and never contains nulls, but can be empty
   */
  public List<Binding> ofConstructorAndMembers(Constructor<?> constructor, Class<?> cls) {
    List<Binding> bindings = ofConstructor(constructor);

    bindings.addAll(ofMembers(cls));

    return bindings;
  }

  /**
   * Returns all bindings for the given {@link Constructor}.
   *
   * @param constructor a {@link Constructor} to examine for bindings, cannot be null
   * @return a list of bindings, never null and never contains nulls, but can be empty
   */
  public List<Binding> ofConstructor(Constructor<?> constructor) {
    return ofExecutable(constructor, constructor.getDeclaringClass());
  }

  /**
   * Returns all member bindings for the given class. These are inject annotated
   * methods and fields, but not constructors.
   *
   * @param cls a {@link Class} to examine for bindings, cannot be null
   * @return a list of bindings, never null and never contains nulls, but can be empty
   */
  public List<Binding> ofMembers(Class<?> cls) {
    List<Binding> bindings = new ArrayList<>();
    Class<?> currentInjectableClass = cls;
    Map<TypeVariable<?>, Type> typeArguments = null;

    while(currentInjectableClass != null) {
      for(final Field field : currentInjectableClass.getDeclaredFields()) {
        if(field.isAnnotationPresent(Inject.class)) {
          if(Modifier.isFinal(field.getModifiers())) {
            throw new BindingException("Cannot inject final field: " + field + " in: " + cls);
          }

          if(typeArguments == null) {
            typeArguments = TypeUtils.getTypeArguments(cls, currentInjectableClass);  // pretty sure that you can re-use these even for when are examining fields of a super class later

            if(typeArguments == null) {
              throw new IllegalArgumentException("ownerType must be assignable to field's declaring class: " + cls + "; declaring class: " + currentInjectableClass);
            }
          }

          Type type = TypeUtils.unrollVariables(typeArguments, field.getGenericType());

          try {
            bindings.add(createBinding(
              field,
              null,
              type,
              isOptional(field),
              Annotations.findDirectlyMetaAnnotatedAnnotations(field, QUALIFIER)
            ));
          }
          catch(BindingException e) {
            throw new BindingException("Unable to create binding for: " + field + " in: " + cls, e);
          }
        }
      }

      currentInjectableClass = currentInjectableClass.getSuperclass();
    }

    for(Binding binding : bindings) {
      binding.getAccessibleObject().setAccessible(true);
    }

    return bindings;
  }

  /**
   * Returns all bindings for the given {@link Method}.
   *
   * @param method a {@link Method} to examine for bindings, cannot be null
   * @param ownerType a {@link Type} in which this method is declared, cannot be null
   * @return a list of bindings, never null and never contains nulls, but can be empty
   */
  public List<Binding> ofMethod(Method method, Type ownerType) {
    return ofExecutable(method, ownerType);
  }

  /**
   * Returns all bindings for the given {@link Field}.
   *
   * @param field a {@link Field} to examine for bindings, cannot be null
   * @param ownerType a {@link Type} in which this executable is declared, cannot be null
   * @return an immutable list of bindings, never null and never contains nulls, but can be empty
   */
  public List<Binding> ofField(Field field, Type ownerType) {

    /*
     * Fields don't have any bindings, unless it is a non-static field in which case the
     * declaring class is required before the field can be accessed.
     */

    return Modifier.isStatic(field.getModifiers()) ? List.of() : List.of(ownerBinding(ownerType));
  }

  /**
   * Returns an annotated {@link Constructor} suitable for injection.
   *
   * @param cls a {@link Class}, cannot be null
   * @return a {@link Constructor} suitable for injection, never null
   * @throws BindingException when no suitable constructor is found
   */
  public Constructor<?> getAnnotatedConstructor(Class<?> cls) {
    return getConstructor(cls, true);
  }

  /**
   * Returns a {@link Constructor} suitable for injection. A public empty
   * constructor is considered suitable if no other constructors are annotated.
   *
   * @param cls a {@link Class}, cannot be null
   * @return a {@link Constructor} suitable for injection, never null
   * @throws BindingException when no suitable constructor is found
   */
  public Constructor<?> getConstructor(Class<?> cls) {
    return getConstructor(cls, false);
  }

  private static Constructor<?> getConstructor(Class<?> cls, boolean annotatedOnly) {
    Constructor<?> suitableConstructor = null;
    Constructor<?> defaultConstructor = null;

    for(Constructor<?> constructor : cls.getDeclaredConstructors()) {
      if(constructor.isAnnotationPresent(Inject.class)) {
        if(suitableConstructor != null) {
          throw new BindingException("Multiple @Inject annotated constructors found, but only one allowed: " + cls);
        }

        suitableConstructor = constructor;
      }
      else if(!annotatedOnly && constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
        defaultConstructor = constructor;
      }
    }

    if(suitableConstructor == null && defaultConstructor == null) {
      throw new BindingException("No suitable constructor found; annotate a constructor" + (annotatedOnly ? "" : " or provide an empty public constructor") + ": " + cls);
    }

    return suitableConstructor == null ? defaultConstructor : suitableConstructor;
  }

  private List<Binding> ofExecutable(Executable executable, Type ownerType) {
    Parameter[] parameters = executable.getParameters();
    List<Binding> bindings = new ArrayList<>();

    for(int i = 0; i < parameters.length; i++) {
      Type type = parameters[i].getParameterizedType();

      try {
        Binding binding = createBinding(
          executable,
          parameters[i],
          type,
          isOptional(parameters[i]),
          Annotations.findDirectlyMetaAnnotatedAnnotations(parameters[i], QUALIFIER)
        );

        bindings.add(binding);
      }
      catch(BindingException e) {
        throw new BindingException("Unable to create binding for Parameter " + i + " [" + type + " " + parameters[i].getName() + "] of: " + executable + " in: " + ownerType, e);
      }
    }

    if(!Modifier.isStatic(executable.getModifiers()) && executable instanceof Method) {
      // For a non-static method, the class itself is also a required binding:
      bindings.add(ownerBinding(ownerType));
    }

    return bindings;
  }

  private Binding createBinding(AccessibleObject accessibleObject, Parameter parameter, Type type, boolean optional, Set<Annotation> qualifiers) {
    return createBinding(accessibleObject, parameter, false, type, optional, qualifiers);
  }

  private Binding createBinding(AccessibleObject accessibleObject, Parameter parameter, boolean isProviderAlready, Type type, boolean optional, Set<Annotation> qualifiers) {
    final Class<?> cls = TypeUtils.getRawType(type, null);

    if(Set.class.isAssignableFrom(cls)) {
      return hashSetBinding(accessibleObject, parameter, requireNotProvider(getFirstTypeParameter(type, Set.class)), qualifiers, optional);
    }
    if(List.class.isAssignableFrom(cls)) {
      return arrayListBinding(accessibleObject, parameter, requireNotProvider(getFirstTypeParameter(type, List.class)), qualifiers, optional);
    }
    if(Provider.class.isAssignableFrom(cls) && !isProviderAlready) {
      return providerBinding(accessibleObject, parameter, createBinding(accessibleObject, parameter, true, requireNotProvider(getFirstTypeParameter(type, Provider.class)), optional, qualifiers));
    }

    return directBinding(accessibleObject, parameter, new Key(type, qualifiers), optional);
  }

  private static Type requireNotProvider(Type type) {
    if(TypeUtils.getRawType(type, null) == Provider.class) {
      throw new BindingException("Nested Provider not allowed: " + type);
    }

    return type;
  }

  private static Type getFirstTypeParameter(Type type, Class<?> cls) {
    return TypeUtils.getTypeArguments(type, cls).get(cls.getTypeParameters()[0]);
  }

  private static boolean isOptional(AnnotatedElement element) {
    for(Annotation annotation : element.getAnnotations()) {
      Class<? extends Annotation> annotationType = annotation.annotationType();
      String simpleName = annotationType.getName();

      if(simpleName.endsWith(".Nullable") || annotationType.equals(Opt.class)) {
        return true;
      }
    }

    return false;
  }

  private Binding hashSetBinding(AccessibleObject accessibleObject, Parameter parameter, Type elementType, Set<Annotation> qualifiers, boolean optional) {
    Key elementKey = new Key(elementType, qualifiers);

    return bindingFactory.create(
      new Key(TypeUtils.parameterize(Set.class, elementType), qualifiers),
      accessibleObject,
      parameter,
      true,
      true,
      true,
      instantiator -> {
        List<Object> instances = instantiator.getInstances(elementKey);

        return instances.isEmpty() && optional ? null : new HashSet<>(instances);
      }
    );
  }

  private Binding arrayListBinding(AccessibleObject accessibleObject, Parameter parameter, Type elementType, Set<Annotation> qualifiers, boolean optional) {
    Key elementKey = new Key(elementType, qualifiers);

    return bindingFactory.create(
      new Key(TypeUtils.parameterize(List.class, elementType), qualifiers),
      accessibleObject,
      parameter,
      true,
      true,
      true,
      instantiator -> {
        List<Object> instances = instantiator.getInstances(elementKey);

        return instances.isEmpty() && optional ? null : instances;
      }
    );
  }

  private Binding providerBinding(AccessibleObject accessibleObject, Parameter parameter, Binding binding) {
    return bindingFactory.create(
      binding.getKey(),
      accessibleObject,
      parameter,
      false,
      false,
      binding.isOptional(),
      instantiator -> {

        /*
         * Although it is possible to attempt to inject an external Provider that is known, it is currently hard
         * to find the correct instance when qualifiers on the provided type are in play. The store does not
         * allow searching for a Provider<@Qualifier X>, and searching for @Qualifier Provider<X> (which previous
         * implementations did) is not at all the same thing.
         *
         * To allow for this kind of matching one could instead look for the provided type directly, then use a
         * custom Matcher. The Matcher interface would need to expose more information (Injectable instead of just
         * the Class), and Injectable would have to expose its owner type. In that case a Matcher could check the
         * method (see if it is called "get") and the owner type (see if it is a Provider).
         *
         * However, injecting an externally supplied Provider has one disadvantage: a badly behaving provider (which
         * returns null) can't be prevented. The wrapper approach (which is at the moment always used) can however
         * check for nulls and throw an exception.
         */

        return new Provider<>() {
          @Override
          public Object get() {
            try {
              return binding.getValue(instantiator);
            }
            catch(InstanceResolutionFailure f) {
              throw f.toRuntimeException();
            }
            catch(OutOfScopeException e) {
              throw new NoSuchInstanceException(e.getMessage(), e);
            }
          }
        };
      }
    );
  }

  private Binding directBinding(AccessibleObject accessibleObject, Parameter parameter, Key key, boolean optional) {
    return bindingFactory.create(
      key,
      accessibleObject,
      parameter,
      false,
      true,
      optional,
      instantiator -> {
        if(optional) {
          return instantiator.findInstance(key);
        }

        return instantiator.getInstance(key);
      }
    );
  }

  private Binding ownerBinding(Type ownerType) {
    Key key = new Key(ownerType);

    return bindingFactory.create(
      key,
      null,
      null,
      false,
      true,
      false,
      instantiator -> instantiator.getInstance(key)
    );
  }
}
