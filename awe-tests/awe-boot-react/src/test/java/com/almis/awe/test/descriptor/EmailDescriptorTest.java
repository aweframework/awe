package com.almis.awe.test.descriptor;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Email descriptors of the application: the labels of the screen report email need their variables
 */
// The surefire run of this module (Unit · Java) selects the "integration" tag (test.tags), so the tag is needed to run this unit test
@Tag("integration")
class EmailDescriptorTest {

  private static final String EMAIL_DESCRIPTOR = "application/app/global/Email.xml";

  @Test
  void screenReportEmailDeclaresTheVariablesOfItsLabels() throws Exception {
    Element sendReport = findEmail("SndRep");

    assertThat(variableIds(sendReport))
      .contains("ScrTit", "ScrTitFil", "PdfNam", "DocNam", "XlsNam", "CsvNam", "TxtNam");
  }

  private Element findEmail(String id) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(EMAIL_DESCRIPTOR)) {
      assertThat(input).as(EMAIL_DESCRIPTOR).isNotNull();
      Document document = factory.newDocumentBuilder().parse(input);
      NodeList emails = document.getElementsByTagName("email");
      for (int index = 0; index < emails.getLength(); index++) {
        Element email = (Element) emails.item(index);
        if (id.equals(email.getAttribute("id"))) {
          return email;
        }
      }
    }
    throw new AssertionError("Email " + id + " not found in " + EMAIL_DESCRIPTOR);
  }

  private List<String> variableIds(Element email) {
    List<String> ids = new ArrayList<>();
    NodeList variables = email.getElementsByTagName("variable");
    for (int index = 0; index < variables.getLength(); index++) {
      ids.add(((Element) variables.item(index)).getAttribute("id"));
    }
    return ids;
  }
}
