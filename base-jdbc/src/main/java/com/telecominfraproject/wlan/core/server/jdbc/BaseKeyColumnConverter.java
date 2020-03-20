/**
 * 
 */
package com.telecominfraproject.wlan.core.server.jdbc;

/**
 * When column name is used for collecting auto generated key. different data
 * source has different behavior. For example, embedded HSQL expected it in
 * upper case. PostgreSQL expected it in lower case.
 * 
 * @author yongli
 *
 */
public interface BaseKeyColumnConverter {

    /**
     * Return list of column names
     * 
     * @param columnNames
     * @return converted name
     */
    public String[] getKeyColumnName(final String[] columnNames);
}
