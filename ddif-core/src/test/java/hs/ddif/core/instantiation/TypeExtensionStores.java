package hs.ddif.core.instantiation;

import hs.ddif.core.SupplierTypeExtension;
import hs.ddif.core.config.DirectTypeExtension;
import hs.ddif.core.config.ListTypeExtension;
import hs.ddif.core.config.SetTypeExtension;
import hs.ddif.core.definition.bind.AnnotationStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class TypeExtensionStores {

  public static TypeExtensionStore create(AnnotationStrategy annotationStrategy) {
    Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

    typeExtensions.put(List.class, new ListTypeExtension<>(annotationStrategy));
    typeExtensions.put(Set.class, new SetTypeExtension<>(annotationStrategy));
    typeExtensions.put(Supplier.class, new SupplierTypeExtension<>());

    return new TypeExtensionStore(new DirectTypeExtension<>(annotationStrategy), typeExtensions);
  }
}

