/**
 * 
 */
package com.telecominfraproject.wlan.core.server.jdbc;

/**
 * Delegate KeyColumnConverter serves as a wrapper
 * 
 * @author yongli
 *
 */
public class DelegateKeyColumnConverter implements BaseKeyColumnConverter {

    private final BaseKeyColumnConverter delegate;
    
    protected DelegateKeyColumnConverter(BaseKeyColumnConverter delegate) {
        this.delegate = delegate;
    }
    
    /* (non-Javadoc)
     * @see com.telecominfraproject.wlan.core.server.jdbc.BaseKeyColumnConvertor#getKeyColumnName(java.lang.String[])
     */
    @Override
    public String[] getKeyColumnName(String[] columnNames) {
        return delegate.getKeyColumnName(columnNames);
    }

}
