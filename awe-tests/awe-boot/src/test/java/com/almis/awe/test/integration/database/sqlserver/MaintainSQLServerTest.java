package com.almis.awe.test.integration.database.sqlserver;

import com.almis.awe.test.integration.database.MaintainTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource("classpath:sqlserver.properties")
@Tag("Sqlserver")
class MaintainSQLServerTest extends MaintainTest {
}
