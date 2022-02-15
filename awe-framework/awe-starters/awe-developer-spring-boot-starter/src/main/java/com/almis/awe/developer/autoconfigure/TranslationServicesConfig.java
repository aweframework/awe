package com.almis.awe.developer.autoconfigure;

import com.almis.awe.developer.translators.clients.MyMemoryClient;
import com.almis.awe.developer.translators.clients.MyMemoryFeignClient;
import com.almis.awe.developer.translators.clients.RapidAPIClient;
import com.almis.awe.developer.translators.clients.RapidAPIFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {MyMemoryFeignClient.class, RapidAPIFeignClient.class})
@EnableConfigurationProperties({
  MyMemoryConfigProperties.class,
  RapidAPIConfigProperties.class})
public class TranslationServicesConfig {

  private final MyMemoryConfigProperties myMemoryConfigProperties;
  private final RapidAPIConfigProperties rapidAPIConfigProperties;

  @Autowired
  public TranslationServicesConfig(MyMemoryConfigProperties myMemoryConfigProperties,
                                   RapidAPIConfigProperties rapidAPIConfigProperties) {
    this.myMemoryConfigProperties = myMemoryConfigProperties;
    this.rapidAPIConfigProperties = rapidAPIConfigProperties;
  }

  /**
   * My memory translator client
   * @return My memory client
   */
  @Bean
  public MyMemoryClient myMemoryClient(MyMemoryFeignClient myMemoryFeignClient) {
    return new MyMemoryClient(myMemoryFeignClient,
      myMemoryConfigProperties.getKey(),
      myMemoryConfigProperties.getHost(),
      myMemoryConfigProperties.getEmail()
    );
  }

  /**
   * Rapid API translator client
   * @return My memory client
   */
  @Bean
  public RapidAPIClient rapidAPIClient(RapidAPIFeignClient rapidAPIFeignClient) {
    return new RapidAPIClient(rapidAPIFeignClient,
      rapidAPIConfigProperties.getKey(),
      rapidAPIConfigProperties.getHost(),
      rapidAPIConfigProperties.getEmail(),
      myMemoryConfigProperties.getKey()
    );
  }
}
