package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the {@code SchSrvSshLst} query that backs the task screen's remote
 * server selector (SDD 623-remote-command, Slice 5, design Decision 6).
 * <p>
 * Proves the selector edit round-trip edge case: a server that is deactivated (or switched
 * away from SSH) must disappear from the NORMAL active-SSH option list, but must still be
 * returned when it is the currently bound {@code IdeSrvExe} — otherwise editing a task whose
 * server was later deactivated would silently blank the selection on save.
 * <p>
 * NOTE: this test deliberately does NOT create {@code AweSchTsk} rows via
 * {@code NewSchedulerTask}/{@code UpdateSchedulerTask}. Those maintain targets commit for
 * real against the shared file-based HSQLDB test database (see
 * {@code MaintainService#manageMaintainQueries}), and the {@code AweKey} keygen seed for
 * {@code SchTskKey} in {@code testdata-hsqldb.sql} makes the FIRST generated task id come out
 * as {@code 2} — which collides with the pre-seeded {@code AweSchExe} "orphaned task"
 * fixture used by {@code SchedulerQueriesTest} (gitlab #685 regression) and, worse, gets its
 * execution-history rows deleted outright by {@code UpdateSchedulerTask}'s execution-log
 * purge step. That collision is a pre-existing test-fixture limitation, out of scope for this
 * slice; the {@code Rmt}/{@code IdeSrvExe} task-screen wiring itself is verified by design
 * review and by the {@code taskData}/task-maintain wiring already covered by the full
 * scheduler module test suite.
 */
@Slf4j
@Tag("integration")
@WithMockUser(username = "test", password = "test")
@Transactional
class SchedulerRemoteServerSelectorTest extends AbstractSpringAppIntegrationTest {

  private String performRequest(String type, String name, String variables) throws Exception {
    MvcResult mvcResult = mockMvc.perform(post("/action/" + type + "/" + name)
            .with(csrf())
            .content("{" + variables + "}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();
    return mvcResult.getResponse().getContentAsString();
  }

  private ArrayNode dataRows(String result) throws Exception {
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").textValue());
    ObjectNode dataList = (ObjectNode) ((ObjectNode) fillAction.get("parameters")).get("datalist");
    return (ArrayNode) dataList.get("rows");
  }

  private int findServerIdByName(String name) throws Exception {
    String result = performRequest("data", "SchSrvLst", "\"CrtSrv\":\"" + name + "\"");
    ArrayNode rows = dataRows(result);
    assertEquals(1, rows.size(), "Expected exactly one server named " + name);
    return rows.get(0).get("Ide").asInt();
  }

  private boolean containsServerId(ArrayNode rows, int serverId) {
    for (JsonNode row : rows) {
      if (row.get("value").asInt() == serverId) {
        return true;
      }
    }
    return false;
  }

  @Test
  void deactivatedServerStaysSelectableOnlyWhenBound() throws Exception {
    String serverName = "SSH-SEL-" + System.nanoTime();

    // 1. Create an active SSH server
    performRequest("maintain", "NewSchedulerServer",
        "\"Nom\":\"" + serverName + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.3\",\"Prt\":22,\"Act\":1,"
            + "\"SshUsr\":\"admin\",\"SshPwd\":\"secret\",\"SshKey\":\"\"");
    int serverId = findServerIdByName(serverName);

    // 2. SchSrvSshLst must include the active SSH server in the normal option list
    ArrayNode activeListRows = dataRows(performRequest("data", "SchSrvSshLst", ""));
    assertTrue(containsServerId(activeListRows, serverId),
        "Active SSH server must appear in the selector's normal option list");

    // 3. Deactivate the server: it must disappear from the normal active-SSH listing...
    performRequest("maintain", "DeaSchSrv", "\"Ide.selected\":" + serverId);
    ArrayNode afterDeactivateRows = dataRows(performRequest("data", "SchSrvSshLst", ""));
    assertFalse(containsServerId(afterDeactivateRows, serverId),
        "A deactivated server must not appear in the normal active-SSH listing");

    // 4. ...but it must still appear when it is the currently bound IdeSrvExe (selector
    //    edit round-trip edge case: editing the task must not blank out the selection)
    ArrayNode boundRows = dataRows(performRequest("data", "SchSrvSshLst", "\"IdeSrvExe\":" + serverId));
    assertTrue(containsServerId(boundRows, serverId),
        "The currently bound server must remain selectable even if deactivated, "
            + "so editing the task never drops the stored reference");

    // 5. And an unrelated, never-bound id must NOT be pulled in by the union filter
    ArrayNode unboundQueryRows = dataRows(performRequest("data", "SchSrvSshLst", "\"IdeSrvExe\":-1"));
    assertFalse(containsServerId(unboundQueryRows, serverId),
        "The deactivated server must stay excluded when it is not the bound IdeSrvExe");
  }
}
