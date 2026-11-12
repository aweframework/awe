package com.almis.awe.testing.descriptor;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules of {@link AbstractApplicationDescriptorTest}, run against the fixture modules of
 * src/test/resources/descriptor: a valid one and one broken module per rule. The framework module of the fixtures
 * is src/test/resources/application/fwk, which is on the test classpath like the AWE modules are on the one of an
 * application.
 */
class ApplicationDescriptorRulesTest {

  private static AbstractApplicationDescriptorTest module(String name, Set<String> publicTargets) {
    return new AbstractApplicationDescriptorTest() {
      @Override
      protected Path moduleDirectory() {
        return Path.of("src/test/resources/descriptor", name);
      }

      @Override
      protected Set<String> publicTargets() {
        return publicTargets;
      }
    };
  }

  private static AbstractApplicationDescriptorTest withProperties(String file) {
    return new AbstractApplicationDescriptorTest() {
      @Override
      protected Path moduleDirectory() {
        return Path.of("src/test/resources/descriptor/valid");
      }

      @Override
      protected Path propertiesFile() {
        return Path.of("src/test/resources/properties", file);
      }
    };
  }

  private static AbstractApplicationDescriptorTest withSeed(String module, String seed) {
    return new AbstractApplicationDescriptorTest() {
      @Override
      protected Path moduleDirectory() {
        return Path.of("src/test/resources/descriptor", module);
      }

      @Override
      protected Path seedDirectory() {
        return Path.of(seed);
      }
    };
  }

  private static AbstractApplicationDescriptorTest valid() {
    return module("valid", Set.of("FixSelPub", "FixPublicInsert"));
  }

  @Test
  void aValidModuleParsesIntoItsModel() {
    assertThatCode(() -> valid().descriptorsParseIntoTheirModel()).doesNotThrowAnyException();
  }

  @Test
  void anUnknownElementFailsTheParse() {
    assertThatThrownBy(() -> module("unparsable", Set.of()).descriptorsParseIntoTheirModel())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("screen/bad.xml");
  }

  @Test
  void aDirectoryWithoutDescriptorsFails() {
    assertThatThrownBy(() -> module("no-xml", Set.of()).descriptorsParseIntoTheirModel())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("XML descriptors of")
      .hasMessageContaining("not to be empty");
  }

  @Test
  void anEnumeratedGroupRequestedAsAQueryFails() {
    assertThatThrownBy(() -> module("enum-as-query", Set.of()).screenTargetsExist())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("query FixEnum");
  }

  @Test
  void theModuleOfAClasspathResourceIsTheSegmentAfterTheLastApplicationFolder() {
    assertThat(AbstractApplicationDescriptorTest.moduleOf(
      "file:/home/application/locale/build/target/classes/application/awe-react/locale/Locale-en-GB.xml"))
      .isEqualTo("awe-react");
    assertThat(AbstractApplicationDescriptorTest.moduleOf(
      "jar:file:/repo/global/awe.jar!/application/awe/global/Queries.xml")).isEqualTo("awe");
  }

  @Test
  void aValidModuleHasEveryLocaleKeyInEveryLanguage() {
    assertThatCode(() -> valid().localeKeysExistInEveryLanguage()).doesNotThrowAnyException();
  }

  @Test
  void aKeyMissingInOneLanguageFails() {
    assertThatThrownBy(() -> module("missing-key", Set.of()).localeKeysExistInEveryLanguage())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("es-ES")
      .hasMessageContaining("FIX_TITLE");
  }

  @Test
  void aValidModuleSendsOnlyToExistingTargets() {
    assertThatCode(() -> valid().screenTargetsExist()).doesNotThrowAnyException();
  }

  @Test
  void aTargetThatDoesNotExistFails() {
    assertThatThrownBy(() -> module("unknown-target", Set.of()).screenTargetsExist())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("NoSuchQuery");
  }

  @Test
  void aValidModuleDeclaresItsPublicTargets() {
    assertThatCode(() -> valid().publicTargetsAreDeclared()).doesNotThrowAnyException();
  }

  @Test
  void aPublicTargetThatIsNotDeclaredFails() {
    assertThatThrownBy(() -> module("undeclared-public", Set.of()).publicTargetsAreDeclared())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("Leak")
      .hasMessageContaining("LeakWrite");
  }

  @Test
  void aDeclaredTargetThatIsNotPublicFails() {
    assertThatThrownBy(() -> module("valid", Set.of("FixSelPub", "FixPublicInsert", "FixSave")).publicTargetsAreDeclared())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("FixSave");
  }

  @Test
  void externalisedSecretsPass() {
    assertThatCode(() -> withProperties("good.properties").secretsAreExternalised()).doesNotThrowAnyException();
  }

  @Test
  void aLiteralSecretFails() {
    assertThatThrownBy(() -> withProperties("bad.properties").secretsAreExternalised())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("awe.rest.api.jwt.secret")
      .hasMessageNotContaining("awe.security.master-key");
  }

  @Test
  void demoUsersThatTheSeedInsertsPass() {
    assertThatCode(() -> module("demo-ok", Set.of()).demoUsersExistInTheSeed()).doesNotThrowAnyException();
  }

  @Test
  void aDemoUserMissingFromTheSeedFails() {
    assertThatThrownBy(() -> module("demo-missing", Set.of()).demoUsersExistInTheSeed())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("ghost");
  }

  @Test
  void aDemoUserThatOnlyAppearsOutsideTheOpeTableFails() {
    assertThatThrownBy(() -> withSeed("demo-elsewhere", "src/test/resources/seed/elsewhere").demoUsersExistInTheSeed())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("ana");
  }

  @Test
  void aMissingMigrationDirectoryFailsWithAClearMessage() {
    assertThatThrownBy(() -> withSeed("demo-ok", "src/test/resources/seed/none").demoUsersExistInTheSeed())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("migration directory");
  }

  @Test
  void aModuleWithoutDemoKeysIsSkippedWithAMessage() {
    assertThatThrownBy(() -> valid().demoUsersExistInTheSeed())
      .isInstanceOf(org.opentest4j.TestAbortedException.class)
      .hasMessageContaining("no demo users");
  }

  @Test
  void menusThatDeclareTheShellOptionsPass() {
    AbstractApplicationDescriptorTest module = new AbstractApplicationDescriptorTest() {
      @Override
      protected Path moduleDirectory() {
        return Path.of("src/test/resources/descriptor/valid");
      }

      @Override
      protected Map<String, Set<String>> shellOptions() {
        return Map.of("menu/private.xml", Set.of("home"), "menu/public.xml", Set.of("request"));
      }
    };
    assertThatCode(module::shellOptionsAreDeclared).doesNotThrowAnyException();
  }

  @Test
  void aShellOptionMissingFromTheMenusFails() {
    assertThatThrownBy(() -> valid().shellOptionsAreDeclared())
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("menu/private.xml: user-settings")
      .hasMessageContaining("menu/public.xml: error-4xx")
      .hasMessageContaining("menu/public.xml: error-5xx");
  }

  @Test
  void aSelectedRowOrAMultipleOperationPasses() {
    assertThatCode(() -> module("selected-row", Set.of()).rowFieldsReadTheSelectedRow()).doesNotThrowAnyException();
    assertThatCode(() -> valid().rowFieldsReadTheSelectedRow()).doesNotThrowAnyException();
  }

  @Test
  void aGridColumnStoredInASingleRowFails() {
    AbstractApplicationDescriptorTest listInRow = module("list-in-row", Set.of());
    assertThatThrownBy(listInRow::rowFieldsReadTheSelectedRow)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("screen/queue.xml")
      .hasMessageContaining("FixReply")
      .hasMessageContaining("field IdeTkt")
      .hasMessageContaining("variable IdeTkt")
      .hasMessageContaining("IdeTkt.selected");
  }
}
