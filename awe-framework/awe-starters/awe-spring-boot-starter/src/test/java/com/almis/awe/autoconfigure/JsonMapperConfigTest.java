package com.almis.awe.autoconfigure;

import com.almis.awe.model.type.MaintainType;
import com.almis.awe.model.util.data.DataListUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the JsonMapper Spring Boot auto-configures (HTTP message converters, injected {@link JsonMapper})
 * follows the AWE 5 JSON contract guarded by {@code DataListUtilJsonContractTest}, while keeping the Spring Boot
 * date handling (ISO strings instead of timestamps).
 */
class JsonMapperConfigTest {

  /** A plain bean: unlike record components, its properties are subject to MapperFeature.SORT_PROPERTIES_ALPHABETICALLY. */
  public static class OrderedBean {
    public String zeta = "z";
    public String alpha = "a";
  }

  record Counter(int count) {
  }

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, JsonMapperConfig.class))
    .withPropertyValues("spring.jackson.use-jackson2-defaults=true");

  @Test
  @DisplayName("Spring Boot keeps its own JsonMapper instead of the DataListUtil one")
  void bootMapperIsKept() {
    runner.run(context -> {
      assertThat(context).hasSingleBean(JsonMapper.class);
      assertThat(context.getBean(JsonMapper.class)).isNotSameAs(DataListUtil.getMapper());
    });
  }

  @Test
  @DisplayName("Enums are written and read by name")
  void enumsByName() {
    runner.run(context -> {
      JsonMapper mapper = context.getBean(JsonMapper.class);
      assertThat(mapper.writeValueAsString(MaintainType.INSERT)).isEqualTo("\"INSERT\"");
      assertThat(mapper.readValue("\"INSERT\"", MaintainType.class)).isEqualTo(MaintainType.INSERT);
    });
  }

  @Test
  @DisplayName("Properties keep their declaration order instead of being sorted alphabetically")
  void propertiesKeepDeclarationOrder() {
    runner.run(context -> assertThat(context.getBean(JsonMapper.class).writeValueAsString(new OrderedBean()))
      .isEqualTo("{\"zeta\":\"z\",\"alpha\":\"a\"}"));
  }

  @Test
  @DisplayName("A null value for a primitive is read as its default value")
  void nullForPrimitiveIsAccepted() {
    runner.run(context -> assertThat(context.getBean(JsonMapper.class).readValue("{\"count\":null}", Counter.class).count())
      .isZero());
  }

  @Test
  @DisplayName("Trailing content after the root value is rejected")
  void trailingContentIsRejected() {
    runner.run(context -> {
      JsonMapper mapper = context.getBean(JsonMapper.class);
      assertThatThrownBy(() -> mapper.readValue("{\"count\":1},", Counter.class)).isInstanceOf(JacksonException.class);
    });
  }

  @Test
  @DisplayName("Dates are written as strings, not as timestamps")
  void datesAreWrittenAsStrings() {
    runner.run(context -> assertThat(context.getBean(JsonMapper.class).writeValueAsString(new Date(0L)))
      .startsWith("\""));
  }

  @Test
  @DisplayName("An application JsonMapper bean takes precedence over the auto-configured one")
  void applicationMapperWins() {
    JsonMapper applicationMapper = JsonMapper.builder().build();
    runner.withBean(JsonMapper.class, () -> applicationMapper).run(context -> {
      assertThat(context).hasSingleBean(JsonMapper.class);
      assertThat(context.getBean(JsonMapper.class)).isSameAs(applicationMapper);
    });
  }
}
