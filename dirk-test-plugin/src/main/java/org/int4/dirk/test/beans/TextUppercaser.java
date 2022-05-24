package org.int4.dirk.test.beans;

import javax.inject.Named;

@Named
public class TextUppercaser {

  public String uppercase(String text) {
    return text.toUpperCase();
  }
}
