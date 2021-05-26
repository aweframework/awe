package com.almis.awe.test.integration.database.oracle;

import com.almis.awe.test.integration.database.MaintainTest;
import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ExcludeTags;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource("classpath:oracle.properties")
@Tag("Oracle")
@ExcludeTags({"NotCIDatabase", "NotOracleDatabase"})
public class MaintainOracleTest extends MaintainTest {
}
