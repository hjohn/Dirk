package hs.ddif.core.test.injectables;

import jakarta.annotation.PostConstruct;

public class ParentBeanWithPostConstruct {
  protected int postConstructOrderVerifier = 2;
  protected int privatePostConstructOrderVerifier = 2;

  @PostConstruct
  public void postConstruct() {
    postConstructOrderVerifier += 3;
  }

  @PostConstruct
  private void privatePostConstruct() {
    privatePostConstructOrderVerifier += 3;
  }
}
