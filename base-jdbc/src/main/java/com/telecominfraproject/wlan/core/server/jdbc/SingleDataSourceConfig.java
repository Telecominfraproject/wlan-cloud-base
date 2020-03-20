package com.telecominfraproject.wlan.core.server.jdbc;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.server.jdbc.BaseDataSourceConfig;

/**
 * @author dtoptygin
 *
 */
@Component
@Profile("use_single_ds")
@PropertySource({ "${singleDataSource.props:classpath:singleDataSource.properties}" })
public class SingleDataSourceConfig extends BaseDataSourceConfig {

    @Profile("!use_embedded_db")
    @Bean
    @Primary
    public DataSource dataSource(){        
        return new BaseJDbcDataSource(super.getDataSource(), super.getKeyColumnConverter());
    }
    
    @Override
    public String getDataSourceName() {
        return "singleDataSource";
    }
}
