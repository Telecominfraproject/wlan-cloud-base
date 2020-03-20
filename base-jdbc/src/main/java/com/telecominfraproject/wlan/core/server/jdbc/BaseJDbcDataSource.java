/**
 * 
 */
package com.telecominfraproject.wlan.core.server.jdbc;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * A delegating {@link DataSource} with other extensions
 * 
 * @author yongli
 *
 */
public class BaseJDbcDataSource extends DelegatingDataSource {

    private final BaseKeyColumnConverter keyConvertor;

    public BaseJDbcDataSource(DataSource targetDataSource, BaseKeyColumnConverter targetConverter) {
        super(targetDataSource);
        this.keyConvertor = targetConverter;
    }

    public BaseKeyColumnConverter getKeyColumnConverter() {
        return keyConvertor;
    }
}
