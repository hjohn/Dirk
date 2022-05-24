package org.int4.dirk.test.textprovider;

import javax.inject.Inject;

import org.int4.dirk.test.plugin.TextProvider;
import org.int4.dirk.test.plugin.TextStyler;

public class StyledTextProvider implements TextProvider {

  @Inject
  private TextStyler textStyler;

  @Override
  public String provideText() {
    return textStyler.styleText("Styled Text");
  }

}
