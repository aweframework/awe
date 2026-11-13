package com.almis.awe.testing.selenium;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Text supplied by a test (a label, a search) must be able to contain quotes without breaking the XPath that uses it
 */
class XpathLiteralsTest {

  @Test
  void shouldQuoteTextWithoutQuotesWithApostrophes() {
    assertThat(XpathLiterals.of("Text")).isEqualTo("'Text'");
    assertThat(XpathLiterals.of("")).isEqualTo("''");
  }

  @Test
  void shouldQuoteTextWithApostrophesWithDoubleQuotes() {
    assertThat(XpathLiterals.of("It's")).isEqualTo("\"It's\"");
  }

  @Test
  void shouldConcatenateTextWithBothKindsOfQuotes() {
    assertThat(XpathLiterals.of("a'b\"c")).isEqualTo("concat('a', \"'\", 'b\"c')");
    assertThat(XpathLiterals.of("'\"")).isEqualTo("concat(\"'\", '\"')");
  }

  @Test
  void shouldEvaluateToTheOriginalText() throws Exception {
    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(new ByteArrayInputStream("<a/>".getBytes(StandardCharsets.UTF_8)));

    for (String text : new String[]{"plain", "It's", "say \"hi\"", "a'b\"c", "'\"", "''", "\"'\"'"}) {
      assertThat(XPathFactory.newInstance().newXPath().evaluate(XpathLiterals.of(text), document))
        .as(text).isEqualTo(text);
    }
  }
}
