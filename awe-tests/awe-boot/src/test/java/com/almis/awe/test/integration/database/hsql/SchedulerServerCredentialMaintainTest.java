package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
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
 *   <li>{@code serverData} (executor read path) decrypts the stored secrets correctly via the
 *   {@code STRING_ENCRYPT}/{@code DECRYPT} transform pair.</li>
 * </ul>
 */
@Slf4j
@Tag("integration")
@WithMockUser(username = "test", password = "test")
@Transactional
class SchedulerServerCredentialMaintainTest extends AbstractSpringAppIntegrationTest {

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
    assertEquals("admin", schSrvRow.get("SshUsr").asText());
    assertNull(schSrvRow.get("SshPwd"), "SchSrv must never expose SshPwd to the edit form");
    assertNull(schSrvRow.get("SshKey"), "SchSrv must never expose SshKey to the edit form");
    assertNull(schSrvRow.get("SshKeyPass"), "SchSrv must never expose SshKeyPass to the edit form");

    // 3. serverData (executor read path) decrypts the secrets in memory
    String serverDataResult = performRequest("data", "serverData", "\"serverId\":" + serverId);
    ArrayNode serverDataRows = dataRows(serverDataResult);
    assertEquals("initialSecret", serverDataRows.get(0).get("password").asText());
    assertEquals("initialKey", serverDataRows.get(0).get("key").asText());
    assertEquals("initialPass", serverDataRows.get(0).get("keyPassphrase").asText());

    // 4. Update with blank secrets (all flags 0) -- SshUsr changes, secrets are preserved
    String preserveVars = "\"IdeSrv\":" + serverId + ",\"Nom\":\"" + name + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.1\","
        + "\"Prt\":22,\"Act\":1,\"SshUsr\":\"admin2\",\"SshPwd\":\"\",\"SshKey\":\"\",\"SshKeyPass\":\"\","
        + "\"FlgSshPwdUpd\":0,\"FlgSshKeyUpd\":0,\"FlgSshKeyPassUpd\":0";
    performRequest("maintain", "UpdSchSrv", preserveVars);

    String afterPreserveResult = performRequest("data", "serverData", "\"serverId\":" + serverId);
    ArrayNode afterPreserveRows = dataRows(afterPreserveResult);
    assertEquals("initialSecret", afterPreserveRows.get(0).get("password").asText(),
        "Blank submit (FlgSshPwdUpd=0) must preserve the previously-stored password");
    assertEquals("initialKey", afterPreserveRows.get(0).get("key").asText(),
        "Blank submit (FlgSshKeyUpd=0) must preserve the previously-stored key");
    assertEquals("initialPass", afterPreserveRows.get(0).get("keyPassphrase").asText(),
        "Blank submit (FlgSshKeyPassUpd=0) must preserve the previously-stored key passphrase");

    String afterPreserveSchSrv = performRequest("data", "SchSrv", "\"Ide.selected\":" + serverId);
    assertEquals("admin2", dataRows(afterPreserveSchSrv).get(0).get("SshUsr").asText(),
        "SshUsr must update normally regardless of the secret flags");

    // 5. Update with new secrets (all flags 1) -- secrets are re-encrypted and overwritten
    String overwriteVars = "\"IdeSrv\":" + serverId + ",\"Nom\":\"" + name + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.1\","
        + "\"Prt\":22,\"Act\":1,\"SshUsr\":\"admin2\",\"SshPwd\":\"newSecret\",\"SshKey\":\"newKey\",\"SshKeyPass\":\"newPass\","
        + "\"FlgSshPwdUpd\":1,\"FlgSshKeyUpd\":1,\"FlgSshKeyPassUpd\":1";
    performRequest("maintain", "UpdSchSrv", overwriteVars);

    String afterOverwriteResult = performRequest("data", "serverData", "\"serverId\":" + serverId);
    ArrayNode afterOverwriteRows = dataRows(afterOverwriteResult);
    assertEquals("newSecret", afterOverwriteRows.get(0).get("password").asText(),
        "FlgSshPwdUpd=1 must overwrite the stored password with the new value");
    assertEquals("newKey", afterOverwriteRows.get(0).get("key").asText(),
        "FlgSshKeyUpd=1 must overwrite the stored key with the new value");
    assertEquals("newPass", afterOverwriteRows.get(0).get("keyPassphrase").asText(),
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

    ArrayNode afterPwd = dataRows(performRequest("data", "serverData", "\"serverId\":" + serverId));
    assertEquals("pwd1", afterPwd.get(0).get("password").asText(),
        "Typing only a new password must overwrite the stored password");
    assertEquals("key0", afterPwd.get(0).get("key").asText(),
        "Typing only a new password (key blank) must PRESERVE the stored key");

    // (b) Type ONLY a new key (password blank) -> key overwritten, password preserved
    String keyOnlyVars = "\"IdeSrv\":" + serverId + ",\"Nom\":\"" + name + "\",\"Pro\":\"ssh\",\"Hst\":\"10.0.0.2\","
        + "\"Prt\":22,\"Act\":1,\"SshUsr\":\"admin\",\"SshPwd\":\"\",\"SshKey\":\"key1\",\"FlgSshPwdUpd\":0,\"FlgSshKeyUpd\":1";
    performRequest("maintain", "UpdSchSrv", keyOnlyVars);

    ArrayNode afterKey = dataRows(performRequest("data", "serverData", "\"serverId\":" + serverId));
    assertEquals("pwd1", afterKey.get(0).get("password").asText(),
        "Typing only a new key (password blank) must PRESERVE the stored password");
    assertEquals("key1", afterKey.get(0).get("key").asText(),
        "Typing only a new key must overwrite the stored key");
  }
}
