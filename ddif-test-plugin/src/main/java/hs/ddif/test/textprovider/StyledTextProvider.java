package hs.ddif.test.textprovider;

import hs.ddif.test.plugin.TextProvider;
import hs.ddif.test.plugin.TextStyler;

import javax.inject.Inject;

public class StyledTextProvider implements TextProvider {

  @Inject
  private TextStyler textStyler;

  public String provideText() {
    return textStyler.styleText("Styled Text");
  }

}
