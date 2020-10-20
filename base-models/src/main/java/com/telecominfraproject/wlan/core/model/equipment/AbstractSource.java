package com.telecominfraproject.wlan.core.model.equipment;

import java.util.Objects;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public abstract class AbstractSource<T> extends BaseJsonModel {
    private static final long serialVersionUID = 2761981826629575941L;
    protected SourceType source;
    protected T value;

    public AbstractSource(SourceType source, T manualValue) {
        this.source = source;
        this.value = manualValue;
    }

    protected AbstractSource() {
        // json construct
    }

    public SourceType getSource() {
        return source;
    }

    public void setSource(SourceType source) {
        this.source = source;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractSource)) {
            return false;
        }
        AbstractSource<T> other = (AbstractSource<T>) obj;
        return this.source == other.source && Objects.equals(value, other.value);
    }

}
