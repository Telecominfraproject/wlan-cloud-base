package com.telecominfraproject.wlan.core.model.pair;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 * @param <K>
 * @param <V>
 */
@JsonPropertyOrder(alphabetic=true)
public class GenericPair<K,V> extends BaseJsonModel implements Cloneable {
    private static final long serialVersionUID = -7932612991844647854L;
    private K v1;
    private V v2;
    
    public GenericPair() {
    }

    public GenericPair(K v1, V v2){
        this.v1 = v1;
        this.v2 = v2;
    }
    
    public K getValue1(){
        return this.v1;
    }
    
    public void setValue1(K val){
        this.v1 = val;
    }
    
    public V getValue2(){
        return this.v2;
    }
    
    public void setValue2(V val){
        this.v2 = val;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public GenericPair<K,V> clone() {
        return (GenericPair<K,V>) super.clone();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.v1 == null) ? 0 : this.v1.hashCode());
        result = prime * result + ((this.v2 == null) ? 0 : this.v2.hashCode());
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
        if (!(obj.getClass().getName().equals(this.getClass().getName()))) {
            return false;
        }

        @SuppressWarnings("unchecked")
        GenericPair<K, V> other = (GenericPair<K, V>) obj;
        
        if (v1 == null) {
            if (other.v1 != null) {
                return false;
            }
        } else if (!v1.equals(other.v1)) {
            return false;
        }
        if (v2 == null) {
            if (other.v2 != null) {
                return false;
            }
        } else if (!v2.equals(other.v2)) {
            return false;
        }
        return true;
    }
        
    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }
        return false;
    }

}
