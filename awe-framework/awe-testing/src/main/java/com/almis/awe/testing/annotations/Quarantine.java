package com.almis.awe.testing.annotations;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Quarantines a flaky browser test: the blocking jobs of the pipeline do not run it (the build excludes the
 * {@value #TAG} JUnit tag) and a separate job that never blocks the pipeline runs it and keeps its report and evidence.
 *
 * <p>A quarantine is a debt, not a fix: it needs the issue that tracks the flakiness and says why the test is flaky.
 * The test leaves the quarantine when the issue is fixed and the annotation is removed. Put it on a method to quarantine
 * that test, or on a class to quarantine all of its tests (the only way to quarantine a class that is a
 * {@link DependentChain}, because a chain cannot run with a step missing).</p>
 *
 * <pre>{@code
 * @Test
 * @Quarantine(issue = "#812", reason = "The suggest answers after the expected text has been read")
 * void t002_loadSuggestOnGrid() { ... }
 * }</pre>
 *
 * <p>{@code BrowserTestDeclarationsGuard} checks the declaration (the issue reference, the reason, no tag given by hand).</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Tag(Quarantine.TAG)
public @interface Quarantine {

  /**
   * JUnit tag that marks the quarantined tests; the blocking jobs exclude it and the quarantine job selects it
   */
  String TAG = "quarantine";

  /**
   * Issue that tracks the flakiness: its number ({@code #766}) or its URL
   *
   * @return Issue reference
   */
  String issue();

  /**
   * Why the test is flaky, as far as it is known
   *
   * @return Reason of the quarantine
   */
  String reason();
}
