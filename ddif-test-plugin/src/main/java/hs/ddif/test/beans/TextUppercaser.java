package hs.ddif.test.beans;

import javax.inject.Named;

@Named
public class TextUppercaser {

  public String uppercase(String text) {
    return text.toUpperCase();
  }
}
