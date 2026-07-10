package com.almis.awe.service.hotreload;

import com.almis.awe.service.hotreload.XmlSchemaValidator.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XmlSchemaValidator tests. The validator resolves each XML's declared schema through the
 * OASIS catalog on the classpath (schemas/awe/catalog.xml) exactly like the compile-time
 * xml-maven-plugin, so a definition that is invalid against its schema can be caught during
 * hot reload without letting a parse error crash the watcher.
 */
class XmlSchemaValidatorTest {

  private static final String HEADER =
    "<widget xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
      + " xsi:noNamespaceSchemaLocation=\"https://awe.test/schemas/widget.xsd\"";

  private final XmlSchemaValidator validator = new XmlSchemaValidator();

  /**
   * A definition that satisfies its schema validates cleanly
   */
  @Test
  void validXmlPassesValidation(@TempDir Path dir) throws Exception {
    Path file = write(dir, "valid.xml", HEADER + " name=\"ok\"><size>10</size></widget>");

    ValidationResult result = validator.validate(file);

    assertTrue(result.isValidated(), "validation should have run");
    assertTrue(result.isValid(), () -> "expected valid, errors: " + result.getErrors());
    assertTrue(result.getErrors().isEmpty());
  }

  /**
   * A well-formed definition that breaks its schema (missing required attribute, wrong type)
   * is reported as invalid with the offending errors, no exception propagated
   */
  @Test
  void schemaInvalidXmlIsReportedWithErrors(@TempDir Path dir) throws Exception {
    Path file = write(dir, "schema-invalid.xml", HEADER + "><size>not-a-number</size></widget>");

    ValidationResult result = validator.validate(file);

    assertTrue(result.isValidated());
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());
    assertTrue(result.getErrors().get(0).line() > 0, "error should carry a line number");
  }

  /**
   * A malformed (not well-formed) definition is reported as invalid instead of throwing
   */
  @Test
  void malformedXmlIsReportedWithoutThrowing(@TempDir Path dir) throws Exception {
    Path file = write(dir, "malformed.xml", HEADER + " name=\"x\"><size>10</size>");

    ValidationResult result = validator.validate(file);

    assertTrue(result.isValidated());
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());
  }

  /**
   * A missing file cannot be validated: the validator fails open so the reload still proceeds
   */
  @Test
  void missingFileFailsOpen(@TempDir Path dir) {
    ValidationResult result = validator.validate(dir.resolve("does-not-exist.xml"));

    assertFalse(result.isValidated());
    assertTrue(result.isValid(), "fail-open must not report a not-validated file as invalid");
  }

  /**
   * When the catalog cannot be found the validator fails open (schema validation disabled)
   */
  @Test
  void missingCatalogFailsOpen(@TempDir Path dir) throws Exception {
    XmlSchemaValidator withoutCatalog = new XmlSchemaValidator("schemas/awe/does-not-exist-catalog.xml");
    Path file = write(dir, "valid.xml", HEADER + " name=\"ok\"><size>10</size></widget>");

    ValidationResult result = withoutCatalog.validate(file);

    assertFalse(result.isValidated());
    assertTrue(result.isValid());
  }

  /**
   * A definition declaring an external entity (XXE) must not have that entity expanded. The
   * entity is referenced in ELEMENT content of the {@code size} field (typed as an integer):
   * if the DOCTYPE were allowed and the entity expanded, the secret file would be read and its
   * content would surface in the integer type error. The hardened parser instead rejects the
   * DOCTYPE outright, so the file is reported invalid, the secret is never read, and the error
   * names the disallowed DOCTYPE. Referencing the entity in element content (not an attribute)
   * is what makes this test fail if the hardening is removed.
   */
  @Test
  void externalEntityIsNotExpanded(@TempDir Path dir) throws Exception {
    Path secret = dir.resolve("secret.txt");
    // Non-numeric so an expanded entity would surface in the <size> integer type error
    String secretContent = "SECRET-XXE-LEAK-8f3a";
    Files.writeString(secret, secretContent);

    Path file = dir.resolve("xxe.xml");
    Files.writeString(file,
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<!DOCTYPE widget [ <!ENTITY xxe SYSTEM \"" + secret.toUri() + "\"> ]>\n"
        + HEADER + " name=\"ok\"><size>&xxe;</size></widget>");

    ValidationResult result = validator.validate(file);

    assertTrue(result.isValidated(), "validation should have run");
    assertFalse(result.isValid(), "a DOCTYPE-bearing definition must be rejected");
    // The secret file must never be read (entity not expanded)
    for (XmlSchemaValidator.ValidationError error : result.getErrors()) {
      assertFalse(error.message() != null && error.message().contains(secretContent),
        () -> "secret content leaked through an expanded entity: " + error);
    }
    // Pin the assertion to the hardening: removing disallow-doctype-decl breaks this
    assertTrue(
      result.getErrors().stream()
        .anyMatch(error -> error.message() != null && error.message().contains("DOCTYPE is disallowed")),
      () -> "expected a 'DOCTYPE is disallowed' error, got: " + result.getErrors());
  }

  private Path write(Path dir, String name, String body) throws Exception {
    Path file = dir.resolve(name);
    Files.writeString(file, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body);
    return file;
  }
}
