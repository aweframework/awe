package com.almis.awe.autoconfigure;

import com.almis.awe.config.RestConfigProperties;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
  public ClientHttpRequestFactory httpComponentsClientHttpRequestFactory() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    // Customize timeouts
    final RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(properties.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .setResponseTimeout(properties.getConnectionRequestTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .build();
    // Configure cookies store
    final BasicCookieStore defaultCookieStore = new BasicCookieStore();
    // Configure SSL context (trust all)
    final SSLContext sslcontext = SSLContexts.custom()
            .loadTrustMaterial(null, new TrustAllStrategy()).build();
    final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
            .setSslContext(sslcontext).build();
    // Configure connection manager
    final HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(sslSocketFactory).build();

    // Http client
    CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultCookieStore(defaultCookieStore)
            .setDefaultRequestConfig(defaultRequestConfig)
            .setConnectionManager(connectionManager)
            .evictExpiredConnections()
            .build();

    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
    requestFactory.setHttpClient(httpClient);
    return requestFactory;
  }
}
