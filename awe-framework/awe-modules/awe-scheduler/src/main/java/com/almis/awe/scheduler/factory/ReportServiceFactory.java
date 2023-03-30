package com.almis.awe.scheduler.factory;

import com.almis.awe.scheduler.enums.ReportType;
import com.almis.awe.scheduler.service.report.ISchedulerReportService;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReportServiceFactory {
  private static final Map<ReportType, ISchedulerReportService> serviceCache = new ConcurrentHashMap<>();
  private final List<ISchedulerReportService> services;

  public ReportServiceFactory(List<ISchedulerReportService> services) {
    this.services = services;
  }

  public static ISchedulerReportService getInstance(ReportType type) {
    return serviceCache.get(type);
  }

  @PostConstruct
  public void initCache() {
    services.forEach(service -> serviceCache.put(service.getType(), service));
  }
}
