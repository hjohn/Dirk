package hs.ddif.plugins;

import hs.ddif.annotations.Produces;
import hs.ddif.plugins.test.project.NotThisOne;

import java.lang.reflect.AnnotatedElement;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

public class DefaultComponentScannerFactory extends ComponentScannerFactory {

  public DefaultComponentScannerFactory() {
    super(
      new AnnotatedElement[] {Named.class, Singleton.class},
      new AnnotatedElement[] {Inject.class, Produces.class},
      new AnnotatedElement[] {Inject.class, Produces.class},
      new AnnotatedElement[] {Inject.class},
      c -> !c.isAnnotationPresent(NotThisOne.class)
    );
  }

}
