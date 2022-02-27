package hs.ddif.core.instantiation;

import hs.ddif.core.SupplierTypeExtension;
import hs.ddif.core.config.ListTypeExtension;
import hs.ddif.core.config.SetTypeExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class TypeExtensionStores {

  public static TypeExtensionStore create() {
    Map<Class<?>, TypeExtension<?>> typeExtensions = new HashMap<>();

    typeExtensions.put(List.class, new ListTypeExtension<>());
    typeExtensions.put(Set.class, new SetTypeExtension<>());
    typeExtensions.put(Supplier.class, new SupplierTypeExtension<>());

    return new TypeExtensionStore(typeExtensions);
  }
}

