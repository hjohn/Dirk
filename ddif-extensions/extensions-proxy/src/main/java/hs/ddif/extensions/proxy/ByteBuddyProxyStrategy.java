package hs.ddif.extensions.proxy;

import hs.ddif.api.annotation.ProxyStrategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.TypeCache.Sort;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * An implementation of {@link ProxyStrategy} which uses Byte Buddy to implement
 * the proxy.
 */
public class ByteBuddyProxyStrategy implements ProxyStrategy {
  private static final String FIELD_NAME = "__instanceSupplier__";
  private static final TypeCache<Class<?>> TYPE_CACHE = new TypeCache<>(Sort.WEAK);

  @Override
  public <T> Function<InstanceSupplier<T>, T> createProxy(Class<T> cls) throws Exception {
    @SuppressWarnings("unchecked")
    Class<T> proxy = (Class<T>)TYPE_CACHE.findOrInsert(cls.getClassLoader(), cls, () -> {
      return new ByteBuddy()
        .subclass(cls)
        .defineField(FIELD_NAME, InstanceSupplier.class, Visibility.PUBLIC)
        .method(ElementMatchers.any()).intercept(MethodDelegation.to(Interceptor.class))
        .make()
        .load(cls.getClassLoader())
        .getLoaded();
    });

    Constructor<T> constructor = proxy.getConstructor();
    Field declaredField = proxy.getDeclaredField(FIELD_NAME);

    return delegate -> {
      try {
        T instance = constructor.newInstance();

        declaredField.set(instance, delegate);

        return instance;
      }
      catch(Exception e) {
        throw new IllegalStateException(e);
      }
    };
  }

  /**
   * Interceptor class to call the underlying delegate object when the proxy is accessed.
   *
   * <p>This class is public in order for the proxy to be able to call it from the
   * package it was created in.
   */
  public static class Interceptor {
    @RuntimeType
    public static Object intercept(@Origin Method method, @AllArguments Object[] args, @FieldValue(FIELD_NAME) InstanceSupplier<?> instanceSupplier) throws Throwable {
      try {
        method.setAccessible(true);

        return method.invoke(instanceSupplier.get(), args);
      }
      catch(InvocationTargetException e) {
        throw e.getCause();
      }
    }
  }
}
