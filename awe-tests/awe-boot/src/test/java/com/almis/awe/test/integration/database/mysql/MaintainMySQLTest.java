package com.almis.awe.test.integration.database.mysql;

import com.almis.awe.test.integration.database.MaintainTest;
import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ExcludeTags;
import org.springframework.test.context.TestPropertySource;

@Tag("Mysql")
@ExcludeTags({"NotCIDatabase", "NotMySQLDatabase"})
@TestPropertySource("classpath:mysql.properties")
class MaintainMySQLTest extends MaintainTest {
}
