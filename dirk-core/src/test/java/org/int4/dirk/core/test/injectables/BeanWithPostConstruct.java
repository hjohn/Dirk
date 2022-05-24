package org.int4.dirk.core.test.injectables;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class BeanWithPostConstruct extends ParentBeanWithPostConstruct {
  @Inject private String test;

  private boolean postConstructCalled;
  private boolean privatePostConstructCalled;

  @PostConstruct
  public void postConstruct2() {
    assertNotNull(test);  // Ensures post construct is called *after* injection

    postConstructCalled = true;
    postConstructOrderVerifier *= 5;
  }

  @PostConstruct
  private void privatePostConstruct() {
    privatePostConstructCalled = true;
    privatePostConstructOrderVerifier *= 5;
  }

  public boolean isPostConstructCalled() {
    return postConstructCalled;
  }

  public boolean isPrivatePostConstructCalled() {
    return privatePostConstructCalled;
  }

  public boolean isPostConstructOrderCorrect() {
    return postConstructOrderVerifier == (2 + 3) * 5;  // If wrong order it would be (2 * 5) + 3 = 13;
  }

  public boolean isPrivatePostConstructOrderCorrect() {
    return privatePostConstructOrderVerifier == (2 + 3) * 5;  // If wrong order it would be (2 * 5) + 3 = 13;
  }
}
