package hs.ddif.test.textprovider;

import hs.ddif.test.beans.TextUppercaser;
import hs.ddif.test.plugin.TextProvider;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class UppercaseTextProvider implements TextProvider {

  @Inject
  private TextUppercaser uppercaser;

  public String provideText() {
    return uppercaser.uppercase("Normal Text");
  }

}
