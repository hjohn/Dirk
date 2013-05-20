package hs.ddif.test.textprovider;

import javax.inject.Named;

import hs.ddif.test.plugin.TextProvider;

@Named
public class FancyTextProvider implements TextProvider {

  public String provideText() {
    return "Fancy Text";
  }

}
