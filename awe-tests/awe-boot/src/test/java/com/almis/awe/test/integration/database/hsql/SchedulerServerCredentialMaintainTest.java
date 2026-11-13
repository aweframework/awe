package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.dao.ServerDAO;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration round-trip test for SSH server credential handling (SDD 623-remote-command, Slice 4).
 * <p>
 * Proves:
 * <ul>
 *   <li>{@code SchSrv} (edit-form preload) exposes {@code SshUsr} but never {@code SshPwd}/{@code SshKey}
 *   (secrets are never sent back to the client).</li>
 *   <li>{@code UpdSchSrv} is PRESERVE-WHEN-BLANK <b>per field</b>: {@code SshPwd} is gated by
 *   {@code FlgSshPwdUpd} and {@code SshKey} by {@code FlgSshKeyUpd}, so updating only one secret
 *   (leaving the other blank) never wipes the untouched secret.</li>
 *   <li>{@code serverData} (executor read path, {@link ServerDAO#findServer}) decrypts the stored secrets
 *   correctly via the {@code STRING_ENCRYPT}/{@code DECRYPT} transform pair, and is server-only: a client request
 *   for it is refused.</li>
 * </ul>
 */
@Slf4j
@Tag("integration")
@WithMockUser(username = "test", password = "test")
@Transactional
class SchedulerServerCredentialMaintainTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private ServerDAO serverDAO;

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

  private String performRefusedRequest(String type, String name, String variables) throws Exception {
    return mockMvc.perform(post("/action/" + type + "/" + name)
            .with(csrf())
            .content("{" + variables + "}")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andReturn().getResponse().getContentAsString();
  }

  private ArrayNode dataRows(String result) throws Exception {
    ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);
    ObjectNode fillAction = (ObjectNode) resultList.get(0);
    assertEquals("fill", fillAction.get("type").stringValue());
    ObjectNode dataList = (ObjectNode) ((ObjectNode) fillAction.get("parameters")).get("datalist");
    return (ArrayNode) dataList.get("rows");
  }

  private int findServerIdByName(String name) throws Exception {
    String result = performRequest("data", "SchSrvLst", "\"CrtSrv\":\"" + name + "\"");
    ArrayNode rows = dataRows(result);
    assertEquals(1, rows.size(), "Expected exactly one server named " + name);
    return rows.get(0).get("Ide").asInt();
  }

  @Test
  void credentialEditRoundTrip() throws Exception {
    String name = "AWEBOOT-TEST-SSH-CRED-" + System.nanoTime();

    // 1. Insert a new SSH server with initial credentials
    String insertVars = "\"Nom\":\"" + name + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.1\",\"Prt\":22,\"Act\":1,"
        + "\"SshUsr\":\"admin\",\"SshPwd\":\"initialSecret\",\"SshKey\":\"initialKey\",\"SshKeyPass\":\"initialPass\"";
    performRequest("maintain", "NewSchedulerServer", insertVars);

    int serverId = findServerIdByName(name);

    // 2. SchSrv (edit preload) must expose SshUsr but never the secret fields
    String schSrvResult = performRequest("data", "SchSrv", "\"Ide.selected\":" + serverId);
    ArrayNode schSrvRows = dataRows(schSrvResult);
    assertEquals(1, schSrvRows.size());
    ObjectNode schSrvRow = (ObjectNode) schSrvRows.get(0);
    assertEquals("admin", schSrvRow.get("SshUsr").asString());
    assertNull(schSrvRow.get("SshPwd"), "SchSrv must never expose SshPwd to the edit form");
    assertNull(schSrvRow.get("SshKey"), "SchSrv must never expose SshKey to the edit form");
    assertNull(schSrvRow.get("SshKeyPass"), "SchSrv must never expose SshKeyPass to the edit form");

    // 3. serverData (executor read path) decrypts the secrets in memory
    Server serverData = serverDAO.findServer(serverId);
    assertEquals("initialSecret", serverData.getPassword());
    assertEquals("initialKey", serverData.getKey());
    assertEquals("initialPass", serverData.getKeyPassphrase());

    // serverData is server-only: a client request is refused with the status of a refusal and gets no secret
    assertThat(performRefusedRequest("data", "serverData", "\"serverId\":" + serverId))
        .contains("You do not have permission to perform this action. Contact your administrator if you need it.")
        .doesNotContain("initialSecret")
        .doesNotContain("initialKey")
        .doesNotContain("initialPass");

    // 4. Update with blank secrets (all flags 0) -- SshUsr changes, secrets are preserved
    String preserveVars = "\"IdeSrv\":" + serverId + ",\"Nom\":\"" + name + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.1\","
        + "\"Prt\":22,\"Act\":1,\"SshUsr\":\"admin2\",\"SshPwd\":\"\",\"SshKey\":\"\",\"SshKeyPass\":\"\","
        + "\"FlgSshPwdUpd\":0,\"FlgSshKeyUpd\":0,\"FlgSshKeyPassUpd\":0";
    performRequest("maintain", "UpdSchSrv", preserveVars);

    Server afterPreserve = serverDAO.findServer(serverId);
    assertEquals("initialSecret", afterPreserve.getPassword(),
        "Blank submit (FlgSshPwdUpd=0) must preserve the previously-stored password");
    assertEquals("initialKey", afterPreserve.getKey(),
        "Blank submit (FlgSshKeyUpd=0) must preserve the previously-stored key");
    assertEquals("initialPass", afterPreserve.getKeyPassphrase(),
        "Blank submit (FlgSshKeyPassUpd=0) must preserve the previously-stored key passphrase");

    String afterPreserveSchSrv = performRequest("data", "SchSrv", "\"Ide.selected\":" + serverId);
    assertEquals("admin2", dataRows(afterPreserveSchSrv).get(0).get("SshUsr").asString(),
        "SshUsr must update normally regardless of the secret flags");

    // 5. Update with new secrets (all flags 1) -- secrets are re-encrypted and overwritten
    String overwriteVars = "\"IdeSrv\":" + serverId + ",\"Nom\":\"" + name + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.1\","
        + "\"Prt\":22,\"Act\":1,\"SshUsr\":\"admin2\",\"SshPwd\":\"newSecret\",\"SshKey\":\"newKey\",\"SshKeyPass\":\"newPass\","
        + "\"FlgSshPwdUpd\":1,\"FlgSshKeyUpd\":1,\"FlgSshKeyPassUpd\":1";
    performRequest("maintain", "UpdSchSrv", overwriteVars);

    Server afterOverwrite = serverDAO.findServer(serverId);
    assertEquals("newSecret", afterOverwrite.getPassword(),
        "FlgSshPwdUpd=1 must overwrite the stored password with the new value");
    assertEquals("newKey", afterOverwrite.getKey(),
        "FlgSshKeyUpd=1 must overwrite the stored key with the new value");
    assertEquals("newPass", afterOverwrite.getKeyPassphrase(),
        "FlgSshKeyPassUpd=1 must overwrite the stored key passphrase with the new value");
  }

  /**
   * Asymmetric per-field preserve-when-blank: typing ONLY a new password (key left blank) must
   * overwrite the password while preserving the stored key, and typing ONLY a new key (password
   * left blank) must overwrite the key while preserving the stored password. Regression guard for
   * the single-flag defect where either field being non-blank rewrote BOTH secrets, wiping the
   * untouched one.
   */
  @Test
  void asymmetricSecretUpdatePreservesUntouchedField() throws Exception {
    String name = "AWEBOOT-TEST-SSH-ASYM-" + System.nanoTime();

    // Insert a new SSH server with both initial credentials
    String insertVars = "\"Nom\":\"" + name + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.2\",\"Prt\":22,\"Act\":1,"
        + "\"SshUsr\":\"admin\",\"SshPwd\":\"pwd0\",\"SshKey\":\"key0\"";
    performRequest("maintain", "NewSchedulerServer", insertVars);
    int serverId = findServerIdByName(name);

    // (a) Type ONLY a new password (key blank) -> password overwritten, key preserved
    String pwdOnlyVars = "\"IdeSrv\":" + serverId + ",\"Nom\":\"" + name + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.2\","
        + "\"Prt\":22,\"Act\":1,\"SshUsr\":\"admin\",\"SshPwd\":\"pwd1\",\"SshKey\":\"\",\"FlgSshPwdUpd\":1,\"FlgSshKeyUpd\":0";
    performRequest("maintain", "UpdSchSrv", pwdOnlyVars);

    Server afterPwd = serverDAO.findServer(serverId);
    assertEquals("pwd1", afterPwd.getPassword(),
        "Typing only a new password must overwrite the stored password");
    assertEquals("key0", afterPwd.getKey(),
        "Typing only a new password (key blank) must PRESERVE the stored key");

    // (b) Type ONLY a new key (password blank) -> key overwritten, password preserved
    String keyOnlyVars = "\"IdeSrv\":" + serverId + ",\"Nom\":\"" + name + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.2\","
        + "\"Prt\":22,\"Act\":1,\"SshUsr\":\"admin\",\"SshPwd\":\"\",\"SshKey\":\"key1\",\"FlgSshPwdUpd\":0,\"FlgSshKeyUpd\":1";
    performRequest("maintain", "UpdSchSrv", keyOnlyVars);

    Server afterKey = serverDAO.findServer(serverId);
    assertEquals("pwd1", afterKey.getPassword(),
        "Typing only a new key (password blank) must PRESERVE the stored password");
    assertEquals("key1", afterKey.getKey(),
        "Typing only a new key must overwrite the stored key");
  }
}
