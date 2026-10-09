package com.almis.awe.testing.annotations;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a browser test class whose tests are one ordered chain: each test continues with the data that the previous
 * ones left (a test creates a record, the next one updates it and the last one deletes it). The rest of the browser test
 * classes are independent, and that is what allows to rerun one failed test alone.
 *
 * <p>A chain is never rerun test by test: the build runs it apart from the independent classes, without the per-test
 * rerun (the {@value #TAG} JUnit tag), because the rerun of one step would find the data that the failed attempt left
 * (a duplicate record, a record that was already deleted). The class keeps its {@code @TestMethodOrder}, and it can only be
 * quarantined as a whole class.</p>
 *
 * <p>The value says why the tests depend on each other. {@code BrowserTestDeclarationsGuard} checks the declaration.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Tag(DependentChain.TAG)
public @interface DependentChain {

  /**
   * JUnit tag that marks the dependent chains; the build runs them without the per-test rerun
   */
  String TAG = "dependent-chain";

  /**
   * Why the tests of the class depend on each other
   *
   * @return Reason of the dependency
   */
  String value();
}
