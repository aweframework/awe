package com.almis.awe.rest.autoconfigure;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import com.almis.awe.rest.controller.AweRestController;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the OpenAPI description of the AWE REST API, which feeds the REST reference page of the website.
 * <p>
 * The description is fetched through MockMvc, normalised (sorted keys, no run-dependent data) and compared with the
 * committed snapshot website/scripts/rest-reference/data/openapi.json. A stale snapshot fails the build; rewrite it with
 * <pre>mvn test -pl awe-framework/awe-starters/awe-rest-spring-boot-starter -Dtest=OpenApiSnapshotTest -Dawe.rest.reference.refresh=true</pre>
 */
class OpenApiSnapshotTest {

  static final String REFRESH_PROPERTY = "awe.rest.reference.refresh";
  static final Path SNAPSHOT = Path.of(System.getProperty("basedir", System.getProperty("user.dir")))
    .resolve(Path.of("..", "..", "..", "website", "scripts", "rest-reference", "data", "openapi.json")).normalize();

  private static final ObjectMapper MAPPER = new JsonMapper();

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(
      JacksonAutoConfiguration.class,
      WebMvcAutoConfiguration.class,
      SpringDocConfiguration.class,
      SpringDocConfigProperties.class,
      SpringDocWebMvcConfiguration.class,
      MultipleOpenApiSupportConfiguration.class))
    .withUserConfiguration(OpenAPIConfiguration.class, TestConfig.class);

  @Test
  @DisplayName("The committed OpenAPI snapshot matches the API as it is now")
  void snapshotMatchesTheApi() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
      String body = mockMvc.perform(get("/v3/api-docs/awe-api"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

      String current = normalize(body);
      assertThat(current).contains("\"/api/authenticate\"", "\"/api/data/{queryId}\"");

      if (Boolean.getBoolean(REFRESH_PROPERTY)) {
        Files.createDirectories(SNAPSHOT.getParent());
        Files.writeString(SNAPSHOT, current);
      }
      assertThat(SNAPSHOT).as("OpenAPI snapshot (create it with -D%s=true)", REFRESH_PROPERTY).exists();
      assertThat(withUnixLineEndings(Files.readString(SNAPSHOT)))
        .as("The REST API changed: refresh the snapshot with -D%s=true and commit it", REFRESH_PROPERTY)
        .isEqualTo(current);
    });
  }

  @Test
  @DisplayName("Normalisation sorts keys, drops servers and keeps array order")
  void normalizeIsStable() throws IOException {
    String normalized = normalize("{\"servers\":[{\"url\":\"http://localhost\"}],\"b\":1,\"a\":{\"z\":[3,1],\"y\":null}}");
    assertThat(normalized).isEqualTo("{\n  \"a\" : {\n    \"y\" : null,\n    \"z\" : [\n      3,\n      1\n    ]\n  },\n  \"b\" : 1\n}\n");
  }

  @Test
  @DisplayName("Line endings of a Windows checkout do not make the snapshot stale")
  void crlfIsIgnored() {
    assertThat(withUnixLineEndings("{\r\n  \"a\" : 1\r\n}\r\n")).isEqualTo("{\n  \"a\" : 1\n}\n");
  }

  @Test
  @DisplayName("Normalisation sorts the top level tags by name")
  void normalizeSortsTags() throws IOException {
    String normalized = normalize("{\"tags\":[{\"name\":\"b\"},{\"name\":\"a\"}]}");
    assertThat(normalized.indexOf("\"a\"")).isLessThan(normalized.indexOf("\"b\""));
  }

  static String withUnixLineEndings(String text) {
    return text.replace("\r\n", "\n");
  }

  static String normalize(String json) throws IOException {
    JsonNode root = MAPPER.readTree(json);
    if (root instanceof ObjectNode object) {
      object.remove("servers");
    }
    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
    printer.indentObjectsWith(new DefaultIndenter("  ", "\n"));
    printer.indentArraysWith(new DefaultIndenter("  ", "\n"));
    return MAPPER.writer().with(printer).writeValueAsString(withSortedTags(sorted(root))) + "\n";
  }

  /** Springdoc collects the tags in a set, so their order is not stable between runs. */
  private static JsonNode withSortedTags(JsonNode root) {
    if (root.path("tags") instanceof ArrayNode tags) {
      List<JsonNode> items = new ArrayList<>();
      tags.forEach(items::add);
      items.sort(Comparator.comparing((JsonNode tag) -> tag.path("name").asString()).thenComparing(JsonNode::toString));
      ((ObjectNode) root).set("tags", MAPPER.createArrayNode().addAll(items));
    }
    return root;
  }

  private static JsonNode sorted(JsonNode node) {
    if (node.isObject()) {
      List<String> names = new ArrayList<>();
      for (Iterator<String> it = node.propertyNames().iterator(); it.hasNext(); ) {
        names.add(it.next());
      }
      names.sort(String::compareTo);
      ObjectNode result = MAPPER.createObjectNode();
      names.forEach(name -> result.set(name, sorted(node.get(name))));
      return result;
    }
    if (node.isArray()) {
      ArrayNode result = MAPPER.createArrayNode();
      node.forEach(item -> result.add(sorted(item)));
      return result;
    }
    return node;
  }

  @Configuration
  @EnableConfigurationProperties(AweRestConfigProperties.class)
  static class TestConfig {

    @Bean
    AweRestController aweRestController(ObjectMapper objectMapper) {
      return new AweRestController(mock(QueryService.class), mock(MaintainService.class), mock(AweRequest.class),
        mock(JWTTokenService.class), new ModelMapper(), objectMapper);
    }
  }
}
