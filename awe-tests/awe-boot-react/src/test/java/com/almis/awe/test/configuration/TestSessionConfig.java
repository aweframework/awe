package com.almis.awe.test.configuration;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.test.listener.TestSessionListener;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring security main configuration method
 * Created by dfuentes on 06/03/2017.
 */
@Configuration
public class TestSessionConfig extends ServiceConfig {

  @Bean
  public ServletListenerRegistrationBean<TestSessionListener> sessionListenerWithMetrics() {
    ServletListenerRegistrationBean<TestSessionListener> listenerRegBean = new ServletListenerRegistrationBean<>();

    listenerRegBean.setListener(new TestSessionListener());
    return listenerRegBean;
  }
}