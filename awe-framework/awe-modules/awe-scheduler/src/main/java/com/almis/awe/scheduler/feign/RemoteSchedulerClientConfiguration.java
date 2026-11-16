package com.almis.awe.scheduler.feign;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Feign configuration of {@link RemoteScheduler} only.
 *
 * <p>It is deliberately not annotated as a component, so it is not picked up by the other Feign clients.
 */
public class RemoteSchedulerClientConfiguration {

  /**
   * Bearer authentication against the remote scheduler instance
   *
   * @param schedulerUrl Remote scheduler API url
   * @param user         User of the remote scheduler instance
   * @param password     Password of the remote scheduler instance
   * @return Request interceptor
   */
  @Bean
  public RequestInterceptor remoteSchedulerAuthInterceptor(
    @Value("${awe.scheduler.remote-scheduler-url:http://localhost:8000/scheduler/api/v1}") String schedulerUrl,
    @Value("${awe.scheduler.remote-scheduler-user:}") String user,
    @Value("${awe.scheduler.remote-scheduler-password:}") String password) {
    return new RemoteSchedulerAuthInterceptor(schedulerUrl, user, password, Clock.systemUTC());
  }
}
