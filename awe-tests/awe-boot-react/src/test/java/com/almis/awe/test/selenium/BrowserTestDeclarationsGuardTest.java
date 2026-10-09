package com.almis.awe.test.selenium;

import com.almis.awe.testing.guard.BrowserTestDeclarationsGuard;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the declarations that organise the browser tests of this application honest: a quarantined test names the issue
 * that tracks its flakiness and says why, because the blocking browser jobs do not run it. No class is a
 * {@code @DependentChain} today; a class that is one follows the rules of the guard too.
 *
 * <p>It is a unit test over the compiled test classes, so it runs with the unit tests and does not need a browser.</p>
 */
// The surefire run of this module (All UT) selects the "integration" tag (test.tags), so the tag is needed to run this unit test
@Tag("integration")
class BrowserTestDeclarationsGuardTest {

  private static final Path TEST_CLASSES = Path.of(System.getProperty("basedir", System.getProperty("user.dir")))
    .resolve("target/test-classes");

  @Test
  void shouldFollowTheDeclarationRulesOfQuarantinesAndChains() throws IOException {
    List<String> violations = BrowserTestDeclarationsGuard.scan(TEST_CLASSES, "com.almis.awe.test.selenium");

    assertThat(violations).as("Declarations of the browser tests of this application").isEmpty();
  }
}
