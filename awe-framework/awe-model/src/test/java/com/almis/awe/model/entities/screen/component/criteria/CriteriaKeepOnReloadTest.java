package com.almis.awe.model.entities.screen.component.criteria;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keep on reload (#993): a criterion can ask the clients to keep its value in the browser tab, so a reload of the
 * browser sends it again with the screen request
 */
class CriteriaKeepOnReloadTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static Criteria parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(new Class[]{Criteria.class});
    return (Criteria) xstream.fromXML(xml);
  }

  @Test
  void theFlagReachesTheClientsAndACopyKeepsIt() throws Exception {
    Criteria kept = parse("<criteria id=\"IdeTsk.selected\" component=\"hidden\" variable=\"IdeTsk.selected\" "
      + "keep-on-reload=\"true\"/>");
    Criteria plain = parse("<criteria id=\"IdeTsk\" component=\"hidden\" variable=\"IdeTsk.selected\"/>");

    assertThat(mapper.valueToTree(kept).get("keepOnReload").asBoolean()).isTrue();
    assertThat(mapper.valueToTree(kept.copy()).get("keepOnReload").asBoolean()).isTrue();
    // A criterion without it sends nothing new to the clients
    assertThat(mapper.valueToTree(plain).has("keepOnReload")).isFalse();
  }
}
