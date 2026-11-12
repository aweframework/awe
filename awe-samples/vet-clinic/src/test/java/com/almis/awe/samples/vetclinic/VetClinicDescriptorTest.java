package com.almis.awe.samples.vetclinic;

import com.almis.awe.testing.descriptor.AbstractApplicationDescriptorTest;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Descriptors of the veterinary clinic: they parse, every locale key exists in English, Spanish and French, every
 * target of a screen exists, and only the intended targets are public
 */
class VetClinicDescriptorTest extends AbstractApplicationDescriptorTest {

  @Override
  protected Path moduleDirectory() {
    return Path.of("src/main/resources/application/vetclinic");
  }

  /**
   * Public targets: the species and reason lists of the appointment request form, and the request itself, which
   * only inserts a pending request
   */
  @Override
  protected Set<String> publicTargets() {
    return Set.of("VetSpcPub", "VetReaPub", "VetReqNew");
  }

  /**
   * Staff write internal notes in the appointments: the list of the owner area must not select the raw note
   * (<code>Nte</code>) but the column that only holds what the owner wrote
   */
  @Test
  void ownerAppointmentListDoesNotExposeStaffNotes() throws Exception {
    NodeList queries = parse("global/Queries.xml").getElementsByTagName("query");
    Element query = null;
    for (int index = 0; index < queries.getLength(); index++) {
      if ("VetMyAptLst".equals(((Element) queries.item(index)).getAttribute("id"))) {
        query = (Element) queries.item(index);
      }
    }
    assertThat(query).as("query VetMyAptLst").isNotNull();
    NodeList fields = query.getElementsByTagName("field");
    for (int index = 0; index < fields.getLength(); index++) {
      assertThat(((Element) fields.item(index)).getAttribute("id")).as("field of VetMyAptLst").isNotEqualTo("Nte");
    }
  }

  /**
   * A target that updates many rows cannot use a list variable in an "in" filter (it fails from the second state)
   */
  @Test
  void multipleUpdatesDoNotUseListVariables() throws Exception {
    NodeList targets = parse("global/Maintain.xml").getElementsByTagName("target");
    for (int index = 0; index < targets.getLength(); index++) {
      Element target = (Element) targets.item(index);
      NodeList updates = target.getElementsByTagName("update");
      if (updates.getLength() > 0 && "true".equals(((Element) updates.item(0)).getAttribute("multiple"))) {
        NodeList variables = target.getElementsByTagName("variable");
        for (int variable = 0; variable < variables.getLength(); variable++) {
          assertThat(((Element) variables.item(variable)).getAttribute("type"))
            .as("variable of " + target.getAttribute("name")).isNotEqualTo("STRING_TO_LIST");
        }
      }
    }
  }

  private org.w3c.dom.Document parse(String file) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(moduleDirectory().resolve(file).toFile());
  }
}
