package com.almis.awe.component;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;

/**
 * Component used for load datasource target map in AweRoutingDataSource {@link AweRoutingDataSource}
 */
public class RoutingDatasourceInitializer
{
    private final DataSource dataSource;


    /**
     * RoutingDatasourceInitializer constructor
     * @param dataSource AbstractRouting dataSource
     */
    public RoutingDatasourceInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void loadDatasourceMap() {
        if (dataSource instanceof AweRoutingDataSource aweRoutingDataSource) {
            aweRoutingDataSource.loadDataSources();
        }
    }
}
