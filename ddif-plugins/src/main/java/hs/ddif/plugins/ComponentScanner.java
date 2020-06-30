package hs.ddif.plugins;

import hs.ddif.core.inject.store.BeanDefinitionStore;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

public class ComponentScanner {
  private static final Logger LOGGER = Logger.getLogger(ComponentScanner.class.getName());

  public static void scan(BeanDefinitionStore store, String... packageNamePrefixes) {

    LOGGER.fine("Scanning packages: " + Arrays.toString(packageNamePrefixes));

    Reflections reflections = new Reflections(
      packageNamePrefixes,
      new TypeAnnotationsScanner(),
      new FieldAnnotationsScanner(),
      new MethodAnnotationsScanner()
    );

    List<Type> types = findComponentTypes(reflections, ComponentScanner.class.getClassLoader());

    store.register(types);
  }

  public static List<Type> findComponentTypes(Reflections reflections, ClassLoader classLoader) {
    Set<String> classNames = new HashSet<>();

    classNames.addAll(reflections.getStore().get("TypeAnnotationsScanner").get("javax.inject.Named"));
    classNames.addAll(reflections.getStore().get("TypeAnnotationsScanner").get("javax.inject.Singleton"));
    classNames.addAll(reflections.getStore().get("TypeAnnotationsScanner").get("hs.ddif.annotations.Producer"));

    for(String name : reflections.getStore().get("FieldAnnotationsScanner").get("javax.inject.Inject")) {
      classNames.add(name.substring(0, name.lastIndexOf('.')));
    }
    for(String name : reflections.getStore().get("MethodAnnotationsScanner").get("javax.inject.Inject")) {
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
}
