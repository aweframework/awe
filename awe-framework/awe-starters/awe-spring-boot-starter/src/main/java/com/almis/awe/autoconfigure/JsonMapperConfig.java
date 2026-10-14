package com.almis.awe.autoconfigure;

import com.almis.awe.model.util.data.DataListUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Applies the AWE JSON contract to the JsonMapper Spring Boot auto-configures, so HTTP message converters and injected
 * mappers keep the AWE wire format while the rest of the Spring Boot configuration (dates, spring.jackson.*) still
 * applies.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
public class JsonMapperConfig {

  /**
   * AWE JSON contract customizer
   * @return Customizer applying {@link DataListUtil#applyAweJsonContract}
   */
  @Bean
  public JsonMapperBuilderCustomizer aweJsonContractCustomizer() {
    return DataListUtil::applyAweJsonContract;
  }
}
