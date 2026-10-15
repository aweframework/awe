package com.almis.awe.test.integration.database.h2;

import com.almis.awe.test.integration.database.MaintainTransactionDisabledTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

/**
 * Own in-memory database: the H2 data script stops on error, so it cannot run again on the database of another context
 */
@Tag("H2")
@TestPropertySource(value = "classpath:h2.properties",
  properties = "spring.datasource.url=jdbc:h2:mem:awe-test-tx-disabled;NON_KEYWORDS=value,user,day,year,month,hour,minute,second")
class MaintainTransactionDisabledH2Test extends MaintainTransactionDisabledTest {
}
