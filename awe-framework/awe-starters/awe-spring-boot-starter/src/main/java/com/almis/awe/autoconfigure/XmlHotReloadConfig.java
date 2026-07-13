package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.hotreload.XmlHotReloadService;
import com.almis.awe.service.hotreload.XmlHotReloadWatcher;
import com.almis.awe.service.hotreload.XmlReloadHandler;
import com.almis.awe.service.hotreload.XmlSchemaValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * XML hot reload autoconfiguration. Only active when {@code awe.application.xml-hot-reload}
 * is enabled (development only), so it has zero production footprint by default.
 */
@Configuration
@ConditionalOnProperty(prefix = "awe.application", name = "xml-hot-reload", havingValue = "true")
@EnableConfigurationProperties(BaseConfigProperties.class)
public class XmlHotReloadConfig {

  /**
   * XML schema validator (reuses the compile-time catalog to guard hot reloads)
   *
   * @return XML schema validator bean
   */
  @Bean
  @ConditionalOnMissingBean
  public XmlSchemaValidator xmlSchemaValidator() {
    return new XmlSchemaValidator();
  }

  /**
   * XML hot reload service
   *
   * @param aweElements          Awe elements
   * @param baseConfigProperties Base configuration properties
   * @param cacheManager         Cache manager provider (cache may not be configured)
   * @param schemaValidator      XML schema validator
   * @param broadcastService     Broadcast service provider (websocket may not be configured)
   * @param reloadHandlers       Registered handlers for XML types unrecognized by the built-in
   *                             classification (empty when no downstream module contributes one: pure no-op)
   * @return XML hot reload service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public XmlHotReloadService xmlHotReloadService(AweElements aweElements, BaseConfigProperties baseConfigProperties,
                                                 ObjectProvider<CacheManager> cacheManager, XmlSchemaValidator schemaValidator,
                                                 ObjectProvider<BroadcastService> broadcastService,
                                                 List<XmlReloadHandler> reloadHandlers) {
    return new XmlHotReloadService(aweElements, baseConfigProperties, cacheManager.getIfAvailable(), schemaValidator,
      broadcastService.getIfAvailable(), reloadHandlers);
  }

  /**
   * XML hot reload watcher
   *
   * @param xmlHotReloadService  XML hot reload service
   * @param baseConfigProperties Base configuration properties
   * @return XML hot reload watcher bean
   */
  @Bean
  @ConditionalOnMissingBean
  public XmlHotReloadWatcher xmlHotReloadWatcher(XmlHotReloadService xmlHotReloadService, BaseConfigProperties baseConfigProperties) {
    return new XmlHotReloadWatcher(xmlHotReloadService, baseConfigProperties);
  }
}
