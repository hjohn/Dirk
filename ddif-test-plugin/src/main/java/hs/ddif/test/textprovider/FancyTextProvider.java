package hs.ddif.test.textprovider;

import hs.ddif.test.plugin.TextProvider;

import javax.inject.Named;

@Named
public class FancyTextProvider implements TextProvider {

  @Override
  public String provideText() {
    return "Fancy Text";
  }

}
