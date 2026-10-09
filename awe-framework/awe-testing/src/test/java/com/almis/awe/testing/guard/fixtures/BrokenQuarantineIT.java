package com.almis.awe.testing.guard.fixtures;

import com.almis.awe.testing.annotations.Quarantine;

/**
 * Fixture of the guard tests: a quarantined class without an issue reference. It has no tests, so no test run executes it.
 */
@Quarantine(issue = "later", reason = "Fixture")
public class BrokenQuarantineIT {
}
