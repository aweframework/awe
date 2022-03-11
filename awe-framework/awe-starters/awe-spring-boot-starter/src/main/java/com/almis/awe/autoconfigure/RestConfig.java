package com.almis.awe.autoconfigure;

import com.almis.awe.config.RestConfigProperties;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Class used to start AWE as a microservice
 */
@Configuration
@EnableConfigurationProperties(value = RestConfigProperties.class)
public class RestConfig {

  // Autowired services
  private final RestConfigProperties properties;

  /**
   * Rest config constructor
   *
   * @param properties Rest configuration properties
   */
  public RestConfig(RestConfigProperties properties) {
    this.properties = properties;
  }

  /**
   * Define client http request factory bean
   * @return Client http request factory
   */
  @Bean
  public ClientHttpRequestFactory httpComponentsClientHttpRequestFactory() {
    CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
    requestFactory.setHttpClient(httpClient);
    requestFactory.setConnectTimeout((int) properties.getConnectionTimeout().toMillis());
    requestFactory.setConnectionRequestTimeout((int) properties.getConnectionRequestTimeout().toMillis());
    return requestFactory;
  }
}
