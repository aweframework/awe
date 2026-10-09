package com.almis.awe.testing.annotations;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.AnnotatedElement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the annotations that declare a quarantined test and a dependent chain
 */
class BrowserTestAnnotationsTest {

  @Quarantine(issue = "#1", reason = "Flaky")
  @Tag("Suite")
  static class QuarantinedClass {
    void test() {
      // Fixture of the test: only its annotations matter, so the method is empty on purpose
    }
  }

  static class QuarantinedMethod {
    @Quarantine(issue = "#1", reason = "Flaky")
    void test() {
      // Fixture of the test: only its annotations matter, so the method is empty on purpose
    }
  }

  @Tag("Suite")
  @TestMethodOrder(MethodOrderer.MethodName.class)
  @DependentChain("The update needs the record that the create made")
  static class Chain {
    void test() {
      // Fixture of the test: only its annotations matter, so the method is empty on purpose
    }
  }

  private static List<String> tagsOf(AnnotatedElement element) {
    return AnnotationSupport.findRepeatableAnnotations(element, Tag.class).stream().map(Tag::value).toList();
  }

  @Test
  void shouldTagAQuarantinedClassForJUnitBesidesItsOwnTags() {
    assertThat(tagsOf(QuarantinedClass.class)).containsExactlyInAnyOrder("Suite", "quarantine");
  }

  @Test
  void shouldTagAQuarantinedMethodForJUnit() throws NoSuchMethodException {
    assertThat(tagsOf(QuarantinedMethod.class.getDeclaredMethod("test"))).containsExactly("quarantine");
  }

  @Test
  void shouldTagADependentChainForJUnitBesidesItsOwnTags() {
    assertThat(tagsOf(Chain.class)).containsExactlyInAnyOrder("Suite", "dependent-chain");
  }

  @Test
  void shouldExposeTheTagNamesThatTheBuildSelects() {
    assertThat(Quarantine.TAG).isEqualTo("quarantine");
    assertThat(DependentChain.TAG).isEqualTo("dependent-chain");
  }
}
