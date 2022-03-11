package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.NumericConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.settings.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Initialize properties
 *
 * @author pgarcia
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@PropertySource("classpath:config/base.properties")
public class PropertyConfig {

  /**
   * Web number options
   *
   * @param numericConfigProperties Numeric config properties
   * @return Web number options bean
   */
  @Bean
  @ConditionalOnMissingBean
  public WebNumberOptions webNumberOptions(NumericConfigProperties numericConfigProperties) {
    return new WebNumberOptions()
            .setASep(numericConfigProperties.getSeparatorThousand())
            .setDGroup(numericConfigProperties.getGroupDecimal())
            .setADec(numericConfigProperties.getSeparatorDecimal())
            .setASign(numericConfigProperties.getCurrencySign())
            .setPSign(numericConfigProperties.getCurrencyPlace())
            .setVMin(numericConfigProperties.getMinValue())
            .setVMax(numericConfigProperties.getMaxValue())
            .setMDec(numericConfigProperties.getDecimalNumbers())
            .setMRound(numericConfigProperties.getRoundType().getCode())
            .setAPad(numericConfigProperties.isPaddingWithZeros())
            .setWEmpty(numericConfigProperties.getEmptyValue());
  }

  /**
   * Web chart options
   *
   * @return Web chart options bean
   */
  @Bean
  @ConditionalOnMissingBean
  public WebChartOptions webChartOptions(BaseConfigProperties baseConfigProperties) {
    return new WebChartOptions()
            .setLimitPointsSerie(baseConfigProperties.getComponent().getChartLimitPointSerie());
  }


  /**
   * Web pivot options
   *
   * @return Web pivot options bean
   */
  @Bean
  @ConditionalOnMissingBean
  public WebPivotOptions webPivotOptions(BaseConfigProperties baseConfigProperties) {
    return new WebPivotOptions()
            .setNumGroup(baseConfigProperties.getComponent().getPivotNumGroup());
  }

  /**
   * Web tooltip options
   *
   * @return Web tooltip options bean
   */
  @Bean
  @ConditionalOnMissingBean
  public WebTooltip webTooltip() {
    return new WebTooltip();
  }

  /**
   * Web settings
   *
   * @param baseConfigProperties     Base configuration properties
   * @param securityConfigProperties Security configuration properties
   * @param numberOptions            Numeric options
   * @param chartOptions             Chart options
   * @param tooltipOptions           Web tooltip options
   * @param pivotOptions             Pivot table options
   * @return WebSettings bean
   */
  @Bean
  @ConditionalOnMissingBean
  public WebSettings webSettings(BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties, WebNumberOptions numberOptions, WebChartOptions chartOptions, WebTooltip tooltipOptions, WebPivotOptions pivotOptions) {
    return WebSettings.builder()
            // Base settings
            .applicationName(baseConfigProperties.getName())
            .language(baseConfigProperties.getLanguageDefault())
            .theme(baseConfigProperties.getTheme())
            .charset(baseConfigProperties.getEncoding())
            .pathServer(baseConfigProperties.getPaths().getServer())
            .initialURL(baseConfigProperties.getPaths().getServer())
            .addressIdentifier(baseConfigProperties.getAddress())
            .tokenKey(baseConfigProperties.getParameter().getToken())
            .homeScreen(baseConfigProperties.getPaths().getScreen() + baseConfigProperties.getScreen().getHome())
            .dataSuffix(baseConfigProperties.getComponent().getDataSuffix())
            .recordsPerPage(baseConfigProperties.getComponent().getGridRowsPerPage())
            .recordsPerPageOnCriteria(baseConfigProperties.getComponent().getCriteriaRowsPerPage())
            .pixelsPerCharacter(baseConfigProperties.getComponent().getGridPixelsPerCharacter())
            .suggestTimeout(baseConfigProperties.getComponent().getSuggestTimeout().toMillis())
            .defaultComponentSize(baseConfigProperties.getComponent().getSize().toString().toLowerCase())
            .reloadCurrentScreen(baseConfigProperties.isReloadCurrentScreen())
            .loadingTimeout(baseConfigProperties.getLoadingTimeout().toMillis())
            .uploadIdentifier(baseConfigProperties.getComponent().getUploadFileId())
            .uploadMaxSize(baseConfigProperties.getComponent().getUploadMaxFileSize().toMegabytes())
            .downloadIdentifier(baseConfigProperties.getComponent().getDownloadFileId())
            .helpTimeout(baseConfigProperties.getComponent().getHelpTimeout().toMillis())
            // Security settings
            .encodeTransmission(securityConfigProperties.isJsonEncryptEnable())
            .encodeKey(securityConfigProperties.getJsonParameter())
            .passwordPattern(securityConfigProperties.getPasswordPattern())
            .minlengthPassword(securityConfigProperties.getPasswordMinLength())
            // Other options
            .numericOptions(numberOptions)
            .chartOptions(chartOptions)
            .pivotOptions(pivotOptions)
            .messageTimeout(tooltipOptions)
            .build();
  }
}
