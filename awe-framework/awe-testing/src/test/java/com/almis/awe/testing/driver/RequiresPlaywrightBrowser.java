package com.almis.awe.testing.driver;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

/**
 * Runs a test class only when the Playwright browser of the tests (Chromium, or Firefox with {@code -Dawe.test.playwright.engine=firefox}) is installed and can be launched on this machine. The tests never
 * download it: where it is missing the class is skipped with the reason, so that a developer machine without browsers
 * stays green. CI installs it and passes {@code -Dawe.test.playwright.required=true}, which makes the missing browser a
 * failure, so the adapter is proven there and the tests cannot silently stop running.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(RequiresPlaywrightBrowser.Condition.class)
@interface RequiresPlaywrightBrowser {

  /**
   * Evaluates whether a Playwright browser is available
   */
  class Condition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
      return evaluate(PlaywrightTestBrowser.engineName(), PlaywrightTestBrowser.unavailableReason(), PlaywrightTestBrowser.isRequired());
    }

    /**
     * Decide whether the tests run
     *
     * @param engine            Engine that the tests run on
     * @param unavailableReason Why the browser cannot be used, or null if it can
     * @param required          Whether the run requires the browser
     * @return Result: enabled when the browser is there, disabled (skipped) when it is not
     * @throws ExtensionConfigurationException If the browser is not there and the run requires it
     */
    static ConditionEvaluationResult evaluate(String engine, String unavailableReason, boolean required) {
      if (unavailableReason == null) {
        return enabled("Playwright " + engine + " is available");
      }
      if (required) {
        throw new ExtensionConfigurationException("Playwright " + engine + " is required (awe.test.playwright.required=true) "
          + "but cannot be launched: " + unavailableReason);
      }
      return disabled(unavailableReason);
    }
  }
}
