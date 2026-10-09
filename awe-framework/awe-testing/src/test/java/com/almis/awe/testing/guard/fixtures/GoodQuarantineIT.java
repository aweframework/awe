package com.almis.awe.testing.guard.fixtures;

import com.almis.awe.testing.annotations.Quarantine;

/**
 * Fixture of the guard tests: a quarantined class that is declared well. It has no tests, so no test run executes it.
 */
@Quarantine(issue = "#766", reason = "Fixture")
public class GoodQuarantineIT {
}
