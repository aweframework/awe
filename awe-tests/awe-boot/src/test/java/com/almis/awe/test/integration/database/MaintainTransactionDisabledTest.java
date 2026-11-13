package com.almis.awe.test.integration.database;

import com.almis.awe.config.MaintainConfigProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Maintain transaction boundaries disabled: the previous behavior, where a maintain launched from a Java service
 * runs on its own connection and commits independently of the outer maintain (S5).
 * The flag is switched off on the shared Spring context (MaintainService reads it on every maintain) instead of with
 * a test property: another context would run the data scripts again on the same database container, and the scripts
 * that do not clean their tables first would duplicate their rows for every test class that runs after it.
 */
@DisplayName("Maintain transaction disabled tests")
public abstract class MaintainTransactionDisabledTest extends AbstractMaintainTransactionTest {

  @Autowired
  private MaintainConfigProperties maintainConfigProperties;

  private boolean transactionEnabled;

  @BeforeEach
  void disableMaintainTransactions() {
    transactionEnabled = maintainConfigProperties.getTransaction().isEnabled();
    maintainConfigProperties.getTransaction().setEnabled(false);
  }

  @AfterEach
  void restoreMaintainTransactions() {
    maintainConfigProperties.getTransaction().setEnabled(transactionEnabled);
  }

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
