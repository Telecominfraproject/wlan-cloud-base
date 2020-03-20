/**
 * 
 */
package com.telecominfraproject.wlan.core.server.jdbc;

/**
 * Default converter, does nothing
 * 
 * @author yongli
 *
 */
public class KeyColumnConverter implements BaseKeyColumnConverter {
    /**
     * Return list of column names
     * 
     * @param columnNames
     * @return converted name
     */
    /**
     * Return list of column names
     * 
     * @param columnNames
     * @return converted name
     */
    @Override
    public String[] getKeyColumnName(final String[] columnNames) {
        return columnNames;
    }
}
