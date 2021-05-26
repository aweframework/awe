package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.database.MaintainTest;
import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ExcludeTags;

@Tag("integration")
@ExcludeTags({"CIDatabase", "NotHSQLDatabase"})
public class MaintainHSQLTest extends MaintainTest {
}
