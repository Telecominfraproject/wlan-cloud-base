/**
 * 
 */
package com.telecominfraproject.wlan.core.server.jdbc;

/**
 * Convert all column name to lower case.
 * 
 * @author yongli
 *
 */
public class KeyColumnLowerCaseConverter implements BaseKeyColumnConverter {
    /**
     * Return list of column names in lower case.
     * 
     * @param columnNames
     * @return converted name
     */
    @Override
    public String[] getKeyColumnName(final String[] columnNames) {
        // empty 
        if (null == columnNames || (0 == columnNames.length)) {
            return null;
        }
        String[] result = new String[columnNames.length];
        for (int i = 0; i < columnNames.length; ++i) {
            result[i] = columnNames[i].toLowerCase();
        }
        return result;
    }
}
