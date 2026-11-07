package com.almis.awe.test.integration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the query descriptors of the framework: the queries that only Java launches ({@code launchPrivateQuery}) and
 * return credentials or the data of other users are marked {@code server-only}, so a client request for them is
 * refused.
 * A server-only query is never public, and every query that returns a password hash, a 2FA secret or a decrypted
 * value is server-only unless an administration screen protected by screen access needs it.
 */
@Tag("integration")
@DisplayName("Query descriptor guard tests")
class QueryDescriptorGuardTest {

  private static final String SERVER_ONLY_ATTRIBUTE = "server-only";

  private static final List<String> FRAMEWORK_QUERY_FILES = List.of(
    "application/awe-boot/global/Queries.xml",
    "application/awe/global/Queries.xml",
    "application/awe-notifier/global/Queries.xml",
    "application/awe-scheduler/global/Queries.xml",
    "application/awe-developer/global/Queries.xml",
    "application/awe-tools/global/Queries.xml");

  /**
   * Queries that the framework only launches from Java with launchPrivateQuery (verified one by one: no screen, action,
   * service or client code names them).
   */
  private static final Set<String> JAVA_ONLY_QUERIES = Set.of(
    // Login and SSO: user details with the password hash and the 2FA secret, profile lookups
    "userDetails", "userDetailsByEmail", "userDetailsByProfile", "checkProfileExists",
    // Login lockout and security audit
    "UsrGetNumLog", "UsrGetSecSta",
    // Access window services: users of a profile and their own rules
    "access-subject-user", "access-compare-users", "access-profile-users", "access-profile-user-rules",
    // Database and JMS connections with the decrypted password (AweDatabaseContextHolder, AweJmsDestination)
    "DatabaseConnections", "JmsConnections",
    // Scheduler server with the decrypted SSH password, key and passphrase (ServerDAO)
    "serverData",
    // Users subscribed to a notification (NotifierService)
    "get-interested-users",
    // Password change date, login attempts, information and mail server password of any user by name: unused by the
    // framework, an application can still launch them from Java
    "GetChgPwdUsr", "AutUsr", "UsrInf", "GetEmlSrvByOpe",
    "userAdditionalProfiles", "UsrIdeOpe", "SsoUsrIdeOpe");

  /** Fields that hold a password hash or a 2FA secret */
  private static final Set<String> CREDENTIAL_FIELDS = Set.of("l1_pas", "secret2fa");

  /**
   * Queries that return a credential or a decrypted value to a client and are not server-only, with the reason.
   */
  private static final Set<String> CLIENT_CREDENTIAL_QUERIES = Set.of(
    // awe-boot test fixture: decrypts a constant it encrypts itself (QueryTest), no stored credential
    "TransformEncryptDecrypt");

  @Test
  @DisplayName("The queries that only Java launches are server-only")
  void javaOnlyQueries_areServerOnly() throws Exception {
    Map<String, Element> queries = loadQueries();

    List<String> notServerOnly = new ArrayList<>();
    for (String id : new TreeSet<>(JAVA_ONLY_QUERIES)) {
      Element query = queries.get(id);
      assertThat(query).as("Query %s must exist in a framework descriptor", id).isNotNull();
      if (!"true".equals(query.getAttribute(SERVER_ONLY_ATTRIBUTE))) {
        notServerOnly.add(id);
      }
    }

    assertThat(notServerOnly).as("Java-only queries without server-only=\"true\"").isEmpty();
  }

  @Test
  @DisplayName("The queries that return a credential or a decrypted value are server-only")
  void credentialQueries_areServerOnly() throws Exception {
    List<String> notServerOnly = new ArrayList<>();
    loadQueries().forEach((id, query) -> {
      if (returnsCredential(query) && !"true".equals(query.getAttribute(SERVER_ONLY_ATTRIBUTE))
        && !CLIENT_CREDENTIAL_QUERIES.contains(id)) {
        notServerOnly.add(id);
      }
    });

    assertThat(notServerOnly).as("Queries with a credential or a decrypted field without server-only=\"true\"")
      .isEmpty();
  }

  @Test
  @DisplayName("A server-only query is not public")
  void serverOnlyQueries_areNotPublic() throws Exception {
    List<String> publicServerOnly = new ArrayList<>();
    loadQueries().forEach((id, query) -> {
      if ("true".equals(query.getAttribute(SERVER_ONLY_ATTRIBUTE)) && "true".equals(query.getAttribute("public"))) {
        publicServerOnly.add(id);
      }
    });

    assertThat(publicServerOnly).as("Queries that are public and server-only").isEmpty();
  }

  private static boolean returnsCredential(Element query) {
    NodeList fields = query.getElementsByTagName("field");
    for (int i = 0; i < fields.getLength(); i++) {
      Element field = (Element) fields.item(i);
      if (CREDENTIAL_FIELDS.contains(field.getAttribute("id")) || "DECRYPT".equals(field.getAttribute("transform"))) {
        return true;
      }
    }
    return false;
  }

  private Map<String, Element> loadQueries() throws Exception {
    Map<String, Element> queries = new LinkedHashMap<>();
    for (String file : FRAMEWORK_QUERY_FILES) {
      try (InputStream stream = getClass().getClassLoader().getResourceAsStream(file)) {
        assertThat(stream).as("Descriptor %s must be on the classpath", file).isNotNull();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document document = factory.newDocumentBuilder().parse(stream);
        NodeList nodes = document.getElementsByTagName("query");
        for (int i = 0; i < nodes.getLength(); i++) {
          Element query = (Element) nodes.item(i);
          if (query.getParentNode() == document.getDocumentElement()) {
            queries.put(query.getAttribute("id"), query);
          }
        }
      }
    }
    return queries;
  }
}
