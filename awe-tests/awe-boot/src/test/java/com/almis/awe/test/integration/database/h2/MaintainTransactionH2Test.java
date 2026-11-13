package com.almis.awe.test.integration.database.h2;

import com.almis.awe.test.integration.database.MaintainTransactionTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("H2")
@TestPropertySource("classpath:h2.properties")
class MaintainTransactionH2Test extends MaintainTransactionTest {
}
