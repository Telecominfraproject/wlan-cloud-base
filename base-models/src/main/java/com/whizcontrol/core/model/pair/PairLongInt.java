package com.whizcontrol.core.model.pair;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author dtop
 *
 */
public class PairLongInt extends GenericPair<Long, Integer> {
    private static final long serialVersionUID = -7917159590232816399L;


    public PairLongInt() {
    }

    public PairLongInt(long longVal, int intVal) {
        super(longVal, intVal);
    }

    public Integer getIntVal() {
        return super.getValue2();
    }

    public void setIntVal(Integer intVal) {
        super.setValue2(intVal);
    }

    public Long getLongVal() {
        return super.getValue1();
    }

    public void setLongVal(Long longVal) {
        super.setValue1(longVal);
    }
    
    /**
     * Hiding this method from Json serialization in favor of more explicit method above - getLongVal
     */
    @Override
    @JsonIgnore
    public Long getValue1(){
        return super.getValue1();
    }
    
    /**
     * Hiding this method from Json serialization in favor of more explicit method above - setLongVal
     */
    @Override
    @JsonIgnore
    public void setValue1(Long val){
        super.setValue1(val);
    }
    
    
    /**
     * Hiding this method from Json serialization in favor of more explicit method above - getIntVal
     */
    @Override
    @JsonIgnore
    public Integer getValue2(){
        return super.getValue2();
    }
    
    /**
     * Hiding this method from Json serialization in favor of more explicit method above - setIntVal
     */
    @Override
    @JsonIgnore
    public void setValue2(Integer val){
        super.setValue2(val);
    }

    
    @Override
    public PairLongInt clone() {
        return (PairLongInt)super.clone();
    }
}