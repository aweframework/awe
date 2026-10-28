package com.almis.awe.model.entities.screen.component;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Card: a metric tile filled from a query, able to host other components (#899)
 */
class CardTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static Card parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Card.class);
    return (Card) xstream.fromXML(xml);
  }

  @Test
  void isAComponentWithTheCardTag() {
    Card card = new Card();

    assertThat(card).isInstanceOf(Component.class);
    assertThat(card.getComponentTag()).isEqualTo("card");
  }

  @Test
  void attributesAreReadFromTheXml() {
    Card card = parse("<card id=\"CrdErr\" title=\"TITLE\" subtitle=\"SUB\" icon=\"bug\" icon-tone=\"warning\" " +
      "value=\"1.2\" unit=\"%\" delta=\"-0.4\" delta-unit=\"pt\" trend=\"down\" caption=\"CAPTION\" " +
      "description=\"DESCRIPTION\" footer=\"FOOTER\" target=\"2\" good-trend=\"down\" tone=\"info\" " +
      "number-decimals=\"1\" initial-load=\"query\" target-action=\"CardErrors\"/>");

    assertThat(card.getId()).isEqualTo("CrdErr");
    assertThat(card.getTitle()).isEqualTo("TITLE");
    assertThat(card.getSubtitle()).isEqualTo("SUB");
    assertThat(card.getIcon()).isEqualTo("bug");
    assertThat(card.getIconTone()).isEqualTo("warning");
    assertThat(card.getValue()).isEqualTo("1.2");
    assertThat(card.getUnit()).isEqualTo("%");
    assertThat(card.getDelta()).isEqualTo("-0.4");
    assertThat(card.getDeltaUnit()).isEqualTo("pt");
    assertThat(card.getTrend()).isEqualTo("down");
    assertThat(card.getCaption()).isEqualTo("CAPTION");
    assertThat(card.getDescription()).isEqualTo("DESCRIPTION");
    assertThat(card.getFooter()).isEqualTo("FOOTER");
    assertThat(card.getTarget()).isEqualTo("2");
    assertThat(card.getGoodTrend()).isEqualTo("down");
    assertThat(card.getTone()).isEqualTo("info");
    assertThat(card.getNumberDecimals()).isEqualTo(1);
    assertThat(card.getInitialLoad()).isEqualTo("query");
    assertThat(card.getTargetAction()).isEqualTo("CardErrors");
  }

  @Test
  void clientReceivesUpAsTheGoodTrendByDefault() {
    JsonNode json = mapper.valueToTree(new Card());

    assertThat(json.get("elementType").asString()).isEqualTo("Card");
    assertThat(json.get("goodTrend").asString()).isEqualTo("up");
    assertThat(json.has("value")).isFalse();
    assertThat(json.has("delta")).isFalse();
    assertThat(json.has("trend")).isFalse();
    assertThat(json.has("tone")).isFalse();
  }

  @Test
  void clientReceivesTheCardAttributesInCamelCase() {
    Card card = parse("<card id=\"Crd\" icon-tone=\"success\" delta-unit=\"pt\" good-trend=\"down\" " +
      "number-decimals=\"2\"/>");

    JsonNode json = mapper.valueToTree(card);

    assertThat(json.get("iconTone").asString()).isEqualTo("success");
    assertThat(json.get("deltaUnit").asString()).isEqualTo("pt");
    assertThat(json.get("goodTrend").asString()).isEqualTo("down");
    assertThat(json.get("numberDecimals").asInt()).isEqualTo(2);
  }

  @Test
  void sparklineAndProgressStyleAreReadFromTheXmlAndSentInCamelCase() {
    Card card = parse("<card id=\"Crd\" sparkline=\"[1,2,3]\" sparkline-type=\"bar\" progress-style=\"gauge\"/>");

    JsonNode json = mapper.valueToTree(card);

    assertThat(card.getSparkline()).isEqualTo("[1,2,3]");
    assertThat(card.getSparklineType()).isEqualTo("bar");
    assertThat(card.getProgressStyle()).isEqualTo("gauge");
    assertThat(json.get("sparkline").asString()).isEqualTo("[1,2,3]");
    assertThat(json.get("sparklineType").asString()).isEqualTo("bar");
    assertThat(json.get("progressStyle").asString()).isEqualTo("gauge");
  }

  @Test
  void childrenAndAttributesSurviveACopy() {
    Card card = parse("<card id=\"Crd\" value=\"10\" delta=\"1\" good-trend=\"down\">" +
      "<window id=\"Win\"/></card>");

    Card copy = card.copy();

    assertThat(copy.getValue()).isEqualTo("10");
    assertThat(copy.getDelta()).isEqualTo("1");
    assertThat(copy.getGoodTrend()).isEqualTo("down");
    assertThat(copy.getElementList()).hasSize(1);
    assertThat(copy.getElementList().getFirst()).isNotSameAs(card.getElementList().getFirst());
  }
}
