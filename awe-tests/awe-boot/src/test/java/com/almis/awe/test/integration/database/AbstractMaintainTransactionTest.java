package com.almis.awe.test.integration.database;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Support of the maintain transaction tests: launches maintains through the maintain action and counts the resulting rows.
 * Not transactional on purpose: a test-managed transaction would hide the commits and rollbacks of the maintains.
 */
@WithMockUser
public abstract class AbstractMaintainTransactionTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private PlatformTransactionManager transactionManager;

  protected static final String MAINTAIN_ERROR = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"There was a problem calling a maintain process\",\"title\":\"Error in maintain operation\",\"type\":\"error\"}},{\"type\":\"cancel\"}]";
  protected static final String MAINTAIN_OK = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\"}}]";
  protected static final String SERVICE_ERROR = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The service returned an error\",\"title\":\"Transaction test error\",\"type\":\"error\"}},{\"type\":\"cancel\"}]";

  @AfterEach
  void cleanData() throws Exception {
    // Removes every AWEBOOT-TEST row of HISAweDbs and HISAweThm
    launchMaintain("CleanUp", MAINTAIN_OK);
  }

  /**
   * Launch a maintain and check its response
   *
   * @param maintainName Maintain name
   * @param expected     Expected response (lenient JSON)
   * @throws Exception Test error
   */
  protected void launchMaintain(String maintainName, String expected) throws Exception {
    mockMvc.perform(post("/action/maintain/" + maintainName)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(expected));
  }

  /**
   * Launch a maintain inside an application transaction of the default datasource, then roll that transaction back
   *
   * @param maintainName Maintain name
   * @param expected     Expected response (lenient JSON)
   */
  protected void launchInRolledBackApplicationTransaction(String maintainName, String expected) {
    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
      try {
        launchMaintain(maintainName, expected);
      } catch (Exception exc) {
        throw new IllegalStateException(exc);
      }
      status.setRollbackOnly();
    });
  }

  /**
   * Count the HISAweDbs rows whose description starts with a prefix
   *
   * @param prefix Description prefix
   * @return Row count
   * @throws Exception Test error
   */
  protected int countHISAweDbs(String prefix) throws Exception {
    return countRows("TxCheckHISAweDbs", prefix);
  }

  /**
   * Count the HISAweThm rows whose name starts with a prefix
   *
   * @param prefix Name prefix
   * @return Row count
   * @throws Exception Test error
   */
  protected int countHISAweThm(String prefix) throws Exception {
    return countRows("TxCheckHISAweThm", prefix);
  }

  private int countRows(String queryName, String prefix) throws Exception {
    MvcResult result = mockMvc.perform(post("/action/data/" + queryName)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"prefix\":\"" + prefix + "\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();
    JsonNode actions = objectMapper.readTree(result.getResponse().getContentAsString());
    return actions.get(0).get("parameters").get("datalist").get("records").asInt();
  }
}
