package com.almis.awe.test.integration.controller;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test /settings rest controller
 */
@Tag("integration")
@DisplayName("Settings controller Tests")
@WithAnonymousUser
class SettingsControllerTest extends AbstractSpringAppIntegrationTest {

  /**
   * Test of getSettings method, of class SettingsController.
   *
   * @throws Exception Test error
   */
  @Test
  void testGetSettings() throws Exception {
    String expected = "{\"pathServer\":\"\",\"initialURL\":\"\",\"language\":\"en\",\"theme\":\"sky\",\"charset\":\"UTF-8\",\"applicationName\":\"awe-boot\",\"dataSuffix\":\".data\",\"homeScreen\":\"screen/home\",\"recordsPerPage\":30,\"recordsPerPageOnCriteria\":100,\"pixelsPerCharacter\":8,\"defaultComponentSize\":\"sm\",\"reloadCurrentScreen\":false,\"suggestTimeout\":300,\"connectionProtocol\":\"COMET\",\"connectionTransport\":\"websocket\",\"connectionBackup\":\"streaming\",\"connectionTimeout\":60000000,\"uploadIdentifier\":\"u\",\"downloadIdentifier\":\"d\",\"uploadMaxSize\":500,\"addressIdentifier\":\"address\",\"passwordPattern\":\".*\",\"minlengthPassword\":4,\"encodeTransmission\":false,\"encodeKey\":\"p\",\"tokenKey\":\"t\",\"actionsStack\":0,\"debug\":\"INFO\",\"loadingTimeout\":10000,\"helpTimeout\":1000,\"messageTimeout\":{\"info\":0,\"error\":0,\"validate\":2000,\"help\":5000,\"warning\":4000,\"ok\":2000,\"wrong\":0,\"chat\":0},\"numericOptions\":{\"pSign\":\"s\",\"aDec\":\",\",\"vMin\":-1.0E10,\"dGroup\":3,\"vMax\":1.0E10,\"mDec\":5,\"mRound\":\"S\",\"aPad\":false,\"wEmpty\":\"empty\",\"aSep\":\".\",\"aSign\":\"\"},\"pivotOptions\":{\"numGroup\":5000},\"chartOptions\":{\"limitPointsSerie\":1000000}}";
    mockMvc.perform(post("/settings")
            .with(csrf())
            .content("{\"view\":\"base\"}")
            .accept("application/json"))
            .andExpect(status().isOk())
            .andExpect(content().json(expected));
  }
}
