package com.almis.awe.test.integration.database.h2;

import com.almis.awe.test.integration.database.QueryTest;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

@Tag("H2")
@TestPropertySource("classpath:h2.properties")
public class QueryH2Test extends QueryTest {
}
