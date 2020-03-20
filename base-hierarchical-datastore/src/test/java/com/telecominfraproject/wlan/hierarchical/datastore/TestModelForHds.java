package com.telecominfraproject.wlan.hierarchical.datastore;

import java.util.Set;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * This model is used in generic Hierarchical DataStore unit tests.
 * @author dtop
 *  
 */
class TestModelForHds extends BaseJsonModel{
    private static final long serialVersionUID = -4915028451879087322L;
    
    private String recordType;
    private String client;
    private String value;
    private Set<String> manyClients;
    
    public TestModelForHds() {
    }

    public TestModelForHds(String recordType, String client, String value) {
        this.recordType = recordType;
        this.client = client;
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getRecordType() {
        return recordType;
    }
    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((recordType == null) ? 0 : recordType.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TestModelForHds)) {
            return false;
        }
        TestModelForHds other = (TestModelForHds) obj;
        if (recordType == null) {
            if (other.recordType != null) {
                return false;
            }
        } else if (!recordType.equals(other.recordType)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public Set<String> getManyClients() {
        return manyClients;
    }

    public void setManyClients(Set<String> manyClients) {
        this.manyClients = manyClients;
    }
    
    
}