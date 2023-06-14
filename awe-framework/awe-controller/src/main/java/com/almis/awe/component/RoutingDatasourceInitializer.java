package com.almis.awe.component;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * Component used for load datasource target map in AweRoutingDataSource {@link AweRoutingDataSource}
 */
@Component
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
        if (dataSource instanceof AweRoutingDataSource) {
            ((AweRoutingDataSource) dataSource).loadDataSources();
        }
    }
}
