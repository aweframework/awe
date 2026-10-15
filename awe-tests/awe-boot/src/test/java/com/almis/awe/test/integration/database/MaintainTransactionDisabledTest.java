package com.almis.awe.test.integration.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Maintain transaction boundaries disabled: the previous behavior, where a maintain launched from a Java service
 * runs on its own connection and commits independently of the outer maintain (S5)
 */
@DisplayName("Maintain transaction disabled tests")
@TestPropertySource(properties = "awe.maintain.transaction.enabled=false")
public abstract class MaintainTransactionDisabledTest extends AbstractMaintainTransactionTest {

  /**
   * The outer maintain is rolled back, but the nested maintain keeps its row committed
   */
  @Test
  void nestedMaintainFromServiceCommitsIndependently() throws Exception {
    launchMaintain("TxNestedServiceThenFail", MAINTAIN_ERROR);

    assertEquals(0, countHISAweDbs("AWEBOOT-TEST-TX-NESTED"));
    assertEquals(1, countHISAweThm("AWEBOOT-TEST-TX-NESTED"));
  }

  /**
   * A maintain launched inside an application transaction commits on its own connection, as before
   */
  @Test
  void maintainCommitsOutsideApplicationTransaction() throws Exception {
    launchInRolledBackApplicationTransaction("TxNestedInsert", MAINTAIN_OK);

    assertEquals(1, countHISAweThm("AWEBOOT-TEST-TX-NESTED"));
  }
}
