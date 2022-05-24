package org.int4.dirk.test.textprovider;

import javax.inject.Named;

import org.int4.dirk.test.plugin.TextProvider;

@Named
public class FancyTextProvider implements TextProvider {

  @Override
  public String provideText() {
    return "Fancy Text";
  }

}
