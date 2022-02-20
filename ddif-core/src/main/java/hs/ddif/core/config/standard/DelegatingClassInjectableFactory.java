package hs.ddif.core.config.standard;

import hs.ddif.core.definition.ClassInjectableFactory;
import hs.ddif.core.definition.ClassInjectableFactoryTemplate;
import hs.ddif.core.definition.ClassInjectableFactoryTemplate.TypeAnalysis;
import hs.ddif.core.definition.DefinitionException;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.bind.BindingException;
import hs.ddif.core.util.Types;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Constructs {@link Injectable}s for {@link Type}s delegating to
 * {@link ClassInjectableFactoryTemplate}s for the actual construction.
 */
public class DelegatingClassInjectableFactory implements ClassInjectableFactory {
  private final List<ClassInjectableFactoryTemplate<Object>> templates;

  /**
   * Constructs a new instance.
   *
   * @param templates a list of {@link ClassInjectableFactoryTemplate}s, cannot be {@code null} or contain {@code null}s but can be empty
   */
  @SuppressWarnings("unchecked")
  public DelegatingClassInjectableFactory(List<ClassInjectableFactoryTemplate<?>> templates) {
    this.templates = (List<ClassInjectableFactoryTemplate<Object>>)(List<?>)new ArrayList<>(templates);
  }

  @Override
  public Injectable create(Type type) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }

    Class<?> cls = Types.raw(type);

    if(TypeUtils.containsTypeVariables(type)) {
      throw new DefinitionException(cls, "cannot have unresolvable type variables: " + Arrays.toString(cls.getTypeParameters()));
    }

    List<TypeAnalysis<?>> failedAnalyses = new ArrayList<>();

    for(ClassInjectableFactoryTemplate<Object> template : templates) {
      TypeAnalysis<Object> analysis = template.analyze(type);

      if(analysis.isNegative()) {
        failedAnalyses.add(analysis);
        continue;
      }

      try {
        return template.create(analysis);
      }
      catch(BindingException e) {
        throw new DefinitionException(cls, "cannot be injected", e);
      }
    }

    throw new DefinitionException(cls, "cannot be injected; failures:" + failedAnalyses.stream().map(ta -> ta.getUnsuitableReason(type)).collect(Collectors.joining("\n - ", "\n - ", "")));
  }
}
