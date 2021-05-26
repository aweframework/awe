package com.almis.awe.test.performance;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@SuiteDisplayName("Performance Suite")
@SelectClasses({
  InitializationTest.class,
  PerformanceTestGroup.class
})
public class PerformanceTestSuite {
}