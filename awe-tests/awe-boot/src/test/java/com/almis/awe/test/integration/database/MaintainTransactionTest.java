package com.almis.awe.test.integration.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Transaction boundaries of the maintain operations (flag enabled, the default)
 */
@DisplayName("Maintain transaction tests")
public abstract class MaintainTransactionTest extends AbstractMaintainTransactionTest {

  /**
   * A failing statement rolls back the statements launched before it (S6)
   */
  @Test
  void failingStatementRollsBackWholeMaintain() throws Exception {
    launchMaintain("TxFailingStatement", MAINTAIN_ERROR);

    assertEquals(0, countHISAweDbs("AWEBOOT-TEST-TX-FAIL"));
    assertEquals(0, countHISAweThm("AWEBOOT-TEST-TX-FAIL"));
  }

  /**
   * The existing rollback target, outside a test-managed transaction (S6)
   */
  @Test
  void rollbackTargetLeavesNoRows() throws Exception {
    launchMaintain("TestRollback", MAINTAIN_ERROR);

    assertEquals(0, countHISAweDbs("AWEBOOT-TEST-ROLLBACK"));
  }

  /**
   * A maintain launched from a Java service joins the outer maintain and is rolled back with it (S7)
   */
  @Test
  void nestedMaintainFromServiceRollsBackWithOuterMaintain() throws Exception {
    launchMaintain("TxNestedServiceThenFail", MAINTAIN_ERROR);

    assertEquals(0, countHISAweDbs("AWEBOOT-TEST-TX-NESTED"));
    assertEquals(0, countHISAweThm("AWEBOOT-TEST-TX-NESTED"));
  }

  /**
   * A retrieve-data sees the rows written before it by the same maintain, not committed yet (S7)
   */
  @Test
  void retrieveDataSeesUncommittedWritesOfSameMaintain() throws Exception {
    launchMaintain("TxRetrieveDataSeesWrites", MAINTAIN_OK);

    assertEquals(1, countHISAweDbs("AWEBOOT-TEST-TX-READ-0"));
    assertEquals(1, countHISAweThm("AWEBOOT-TEST-TX-READ-0"));
  }

  /**
   * A step that returns an ERROR result without an exception rolls back the maintain (S7)
   */
  @Test
  void errorResultRollsBackMaintain() throws Exception {
    launchMaintain("TxErrorResult", SERVICE_ERROR);

    assertEquals(0, countHISAweDbs("AWEBOOT-TEST-TX-ERROR"));
  }

  /**
   * The work before a commit step persists although a later statement fails; the work after it is rolled back (S7)
   */
  @Test
  void commitStepKeepsPreviousWork() throws Exception {
    launchMaintain("TxCommitThenFail", MAINTAIN_ERROR);

    assertEquals(1, countHISAweDbs("AWEBOOT-TEST-TX-COMMIT"));
    assertEquals(0, countHISAweThm("AWEBOOT-TEST-TX-COMMIT"));
  }

  /**
   * A maintain launched inside an application transaction on the same datasource joins it and is rolled back with it (S8)
   */
  @Test
  void maintainJoinsApplicationTransaction() throws Exception {
    launchInRolledBackApplicationTransaction("TxNestedInsert", MAINTAIN_OK);

    assertEquals(0, countHISAweThm("AWEBOOT-TEST-TX-NESTED"));
  }
}
