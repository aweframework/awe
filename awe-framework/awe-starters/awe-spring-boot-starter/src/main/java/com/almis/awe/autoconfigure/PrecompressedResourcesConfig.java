package com.almis.awe.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Serves the brotli copy of the precompressed client bundles to the browsers that accept it (issue #965).
 *
 * <p>Active while the resource chain serves precompressed resources ({@code spring.web.resources.chain.compressed},
 * {@code true} by default in AWE).</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty("spring.web.resources.chain.compressed")
public class PrecompressedResourcesConfig {

  /**
   * Filter that lists brotli first in the accepted encodings of read requests.
   *
   * @return Filter
   */
  @Bean
  @ConditionalOnMissingBean
  public BrotliFirstAcceptEncodingFilter brotliFirstAcceptEncodingFilter() {
    return new BrotliFirstAcceptEncodingFilter();
  }
}
