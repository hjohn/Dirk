package hs.ddif.plugins;

import hs.ddif.test.plugin.TextProvider;

import java.util.Set;

import javax.inject.Inject;

public class BeanWithTextProviders {

  @Inject
  private Set<TextProvider> textProviders;

  public Set<TextProvider> getTextProviders() {
    return textProviders;
  }
}
