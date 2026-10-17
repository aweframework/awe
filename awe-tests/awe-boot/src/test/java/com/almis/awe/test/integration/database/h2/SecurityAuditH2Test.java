package com.almis.awe.test.integration.database.h2;

import com.almis.awe.test.integration.database.SecurityAuditTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("H2")
@TestPropertySource("classpath:h2.properties")
class SecurityAuditH2Test extends SecurityAuditTest {
}
