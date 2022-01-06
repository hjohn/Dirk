package hs.ddif.core.inject.store;

import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.inject.store.ClassInjectableFactoryTemplate.TypeAnalysis;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link ResolvableInjectable}s for {@link Type}s delegating to
 * {@link ClassInjectableFactoryTemplate}s for the actual construction.
 */
public class DelegatingClassInjectableFactory implements ClassInjectableFactory {
  private final List<ClassInjectableFactoryTemplate<Object>> templates;

  /**
   * Constructs a new instance.
   *
   * @param templates a list of {@link ClassInjectableFactoryTemplate}s, cannot be null or contain nulls but can be empty
   */
  @SuppressWarnings("unchecked")
  public DelegatingClassInjectableFactory(List<ClassInjectableFactoryTemplate<?>> templates) {
    this.templates = (List<ClassInjectableFactoryTemplate<Object>>)(List<?>)new ArrayList<>(templates);
  }

  @Override
  public ResolvableInjectable create(Type type) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }

    if(TypeUtils.containsTypeVariables(type)) {
      throw new BindingException("Unresolved type variables in " + type + " are not allowed: " + Arrays.toString(TypeUtils.getRawType(type, null).getTypeParameters()));
    }

    List<TypeAnalysis<?>> failedAnalyses = new ArrayList<>();

    for(ClassInjectableFactoryTemplate<Object> template : templates) {
      TypeAnalysis<Object> analysis = template.analyze(type);

      if(analysis.isNegative()) {
        failedAnalyses.add(analysis);
        continue;
      }

      return template.create(analysis);
    }

    throw new BindingException("Type cannot be injected: " + type + "; failures:" + failedAnalyses.stream().map(ta -> ta.getUnsuitableReason(type)).collect(Collectors.joining("\n - ", "\n - ", "")));
  }
}
