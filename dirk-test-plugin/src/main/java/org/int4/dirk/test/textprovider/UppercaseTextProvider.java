package org.int4.dirk.test.textprovider;

import javax.inject.Inject;
import javax.inject.Named;

import org.int4.dirk.test.beans.TextUppercaser;
import org.int4.dirk.test.plugin.TextProvider;

@Named
public class UppercaseTextProvider implements TextProvider {

  @Inject
  private TextUppercaser uppercaser;

  @Override
  public String provideText() {
    return uppercaser.uppercase("Normal Text");
  }

}
