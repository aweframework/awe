package com.almis.awe.autoconfigure;

import com.almis.awe.config.RestConfigProperties;
import com.almis.awe.exception.AWERuntimeException;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;
import java.util.concurrent.TimeUnit;

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
    // Configure ConnectionConfig for connection timeouts
    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(properties.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .build();

    // Configurar RequestConfig para otros timeouts
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(properties.getConnectionRequestTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .setResponseTimeout(properties.getResponseTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .build();

    // Configurar SSL context (trust all)
    SSLContext sslContext;
    try {
      sslContext = SSLContextBuilder.create()
          .loadTrustMaterial(TrustAllStrategy.INSTANCE)
          .build();
    } catch (Exception ex) {
      throw new AWERuntimeException("Error configuring SSL context", ex);
    }

    // Configure cookies store
    final BasicCookieStore defaultCookieStore = new BasicCookieStore();
    
    // Configurar connection manager con ConnectionConfig
    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext)
                .build())
            .setMaxConnTotal(100)
            .setMaxConnPerRoute(20)
            .build();

    // Crear CloseableHttpClient
    CloseableHttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .evictExpiredConnections()
        .evictIdleConnections(TimeValue.ofSeconds(30))
        .setDefaultCookieStore(defaultCookieStore)
        .build();

    // Crear HttpComponentsClientHttpRequestFactory
    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
    requestFactory.setHttpClient(httpClient);

    return requestFactory;
  }
}
