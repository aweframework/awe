package com.almis.awe.test.integration.database.mysql;

import com.almis.awe.test.integration.database.MaintainTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Mysql")
@TestPropertySource("classpath:mysql.properties")
class MaintainMySQLTest extends MaintainTest {
}
