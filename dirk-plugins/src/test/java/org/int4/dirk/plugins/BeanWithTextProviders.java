package org.int4.dirk.plugins;

import java.util.Set;

import javax.inject.Inject;

import org.int4.dirk.test.plugin.TextProvider;

public class BeanWithTextProviders {

  @Inject
  private Set<TextProvider> textProviders;

  public Set<TextProvider> getTextProviders() {
    return textProviders;
  }
}
