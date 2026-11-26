package com.almis.awe.model.entities.enumerated;

import com.almis.awe.model.entities.Global;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Icon of an enumerated option (#993): an option can declare an icon, which the components that draw the options with
 * an icon (the tabs) show next to its label. Without it the option is read as before
 */
class EnumeratedOptionIconTest {

  private static EnumeratedGroup parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(new Class[]{Enumerated.class});
    return ((Enumerated) xstream.fromXML(xml)).getBaseElementList().get(0);
  }

  @Test
  void anOptionReadsItsIconAndAnOptionWithoutItHasNone() {
    EnumeratedGroup group = parse("<enumerated><group id=\"TskPagTab\">"
      + "<option label=\"SCREEN_TEXT_TASK_PAGE_TAB_SUMMARY\" value=\"TskPagSum\" icon=\"lucide:layout-dashboard\"/>"
      + "<option label=\"SCREEN_TEXT_TASK_PAGE_TAB_HISTORY\" value=\"TskPagHis\"/>"
      + "</group></enumerated>");

    Global withIcon = group.getOptionList().get(0);
    Global withoutIcon = group.getOptionList().get(1);
    assertThat(withIcon.getIcon()).isEqualTo("lucide:layout-dashboard");
    assertThat(withIcon.getValue()).isEqualTo("TskPagSum");
    assertThat(withoutIcon.getIcon()).isNull();
    assertThat(withoutIcon.getLabel()).isEqualTo("SCREEN_TEXT_TASK_PAGE_TAB_HISTORY");
    // A copy of the group (the elements of a module are copied) keeps the icon
    assertThat(group.copy().getOptionList().get(0).getIcon()).isEqualTo("lucide:layout-dashboard");
  }
}
