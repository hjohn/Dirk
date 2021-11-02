package hs.ddif.plugins;

import hs.ddif.annotations.PluginScoped;
import hs.ddif.annotations.Producer;
import hs.ddif.annotations.Produces;
import hs.ddif.annotations.WeakSingleton;
import hs.ddif.core.inject.store.BeanDefinitionStore;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

public class ComponentScanner {
  private static final Logger LOGGER = Logger.getLogger(ComponentScanner.class.getName());
  private static final Scanner[] SCANNERS = {
    Scanners.TypesAnnotated,
    Scanners.FieldsAnnotated,
    Scanners.MethodsAnnotated,
    Scanners.ConstructorsAnnotated
  };

  public static void scan(BeanDefinitionStore store, String... packageNamePrefixes) {
    LOGGER.fine("Scanning packages: " + Arrays.toString(packageNamePrefixes));

    Reflections reflections = createReflections(packageNamePrefixes);

    List<Type> types = findComponentTypes(reflections, ComponentScanner.class.getClassLoader());

    LOGGER.fine("Registering types: " + types);

    store.register(types);
  }

  public static List<Type> findComponentTypes(Reflections reflections, ClassLoader classLoader) {
    Set<String> classNames = new HashSet<>();

    classNames.addAll(reflections.get(Scanners.TypesAnnotated.with(Named.class)));
    classNames.addAll(reflections.get(Scanners.TypesAnnotated.with(Singleton.class)));
    classNames.addAll(reflections.get(Scanners.TypesAnnotated.with(WeakSingleton.class)));
    classNames.addAll(reflections.get(Scanners.TypesAnnotated.with(Producer.class)));
    classNames.addAll(reflections.get(Scanners.TypesAnnotated.with(PluginScoped.class)));

    for(String name : reflections.get(Scanners.FieldsAnnotated.with(Inject.class))) {
      classNames.add(name.substring(0, name.lastIndexOf('.')));
    }

    for(String name : reflections.get(Scanners.FieldsAnnotated.with(Produces.class))) {
      classNames.add(name.substring(0, name.lastIndexOf('.')));
    }

    for(String name : reflections.get(Scanners.ConstructorsAnnotated.with(Inject.class))) {
      name = name.substring(0, name.lastIndexOf('('));
      classNames.add(name.substring(0, name.lastIndexOf('.')));
    }

    for(String name : reflections.get(Scanners.MethodsAnnotated.with(Inject.class))) {
      name = name.substring(0, name.lastIndexOf('('));
      classNames.add(name.substring(0, name.lastIndexOf('.')));
    }

    for(String name : reflections.get(Scanners.MethodsAnnotated.with(Produces.class))) {
      name = name.substring(0, name.lastIndexOf('('));
      classNames.add(name.substring(0, name.lastIndexOf('.')));
    }

    List<Type> types = new ArrayList<>();

    for(String className : classNames) {
      try {
        Class<?> cls = classLoader.loadClass(className);

        if(!Modifier.isAbstract(cls.getModifiers())) {
          types.add(cls);
        }
      }
      catch(ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }

    return types;
  }

  public static Reflections createReflections(String... packageNamePrefixes) {
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

  public static Reflections createReflections(URL... urls) {
    Configuration configuration = new ConfigurationBuilder()
      .addUrls(urls)
      .setScanners(SCANNERS);

    return new Reflections(configuration);
  }
}
