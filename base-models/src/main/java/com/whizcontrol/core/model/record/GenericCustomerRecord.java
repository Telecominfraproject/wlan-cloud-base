package com.whizcontrol.core.model.record;

import com.whizcontrol.core.model.json.BaseJsonModel;

public class GenericCustomerRecord<T extends BaseJsonModel> extends BaseJsonModel {
    private static final long serialVersionUID = -5744903915279732103L;
    private long id;
    private T details;

    private long createdTimestamp;
    private long lastModifiedTimestamp;

    public GenericCustomerRecord() {
        super();
    }

    public GenericCustomerRecord(GenericCustomerRecord<T> other) {
        this.id = other.id;
        this.details = other.details;
        this.createdTimestamp = other.createdTimestamp;
        this.lastModifiedTimestamp = other.lastModifiedTimestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public T getDetails() {
        return details;
    }

    public void setDetails(T details) {
        this.details = details;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    /**
     * Use by rule engine. only use id field.
     */
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    /**
     * Used by rule engine, only use id field and class
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!getClass().isInstance(obj))
            return false;
        GenericCustomerRecord<T> other = getClass().cast(obj);
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public GenericCustomerRecord<T> clone() {
        GenericCustomerRecord<T> ret = (GenericCustomerRecord<T>) super.clone();
        if (details != null) {
            ret.details = (T) details.clone();
        }
        return ret;
    }

}
