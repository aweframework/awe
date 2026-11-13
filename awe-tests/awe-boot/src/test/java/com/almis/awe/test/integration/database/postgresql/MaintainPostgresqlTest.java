package com.almis.awe.test.integration.database.postgresql;

import com.almis.awe.test.integration.database.MaintainTest;
import com.almis.awe.testing.database.AweDatabaseTest;
import com.almis.awe.testing.database.TestDatabase;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("Postgresql")
@AweDatabaseTest(TestDatabase.POSTGRESQL)
@TestPropertySource("classpath:postgresql.properties")
class MaintainPostgresqlTest extends MaintainTest {
}
