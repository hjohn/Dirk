package org.int4.dirk.plugins;

import java.lang.reflect.AnnotatedElement;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.int4.dirk.annotations.Produces;
import org.int4.dirk.plugins.test.project.NotThisOne;

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
