package com.almis.awe.test.performance;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Performance Suite")
@SelectClasses({
  InitializationTest.class,
  PerformanceTestGroup.class
})
public class PerformanceTestSuite {
}
