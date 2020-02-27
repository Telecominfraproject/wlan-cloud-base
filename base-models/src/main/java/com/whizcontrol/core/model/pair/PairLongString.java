package com.whizcontrol.core.model.pair;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author dtop
 *
 */
public class PairLongString extends GenericPair<Long, String>{
    private static final long serialVersionUID = 2927226074785057554L;

    public PairLongString() {
    }

    public PairLongString(long longVal, String stringVal) {
        super(longVal, stringVal);
    }

    public Long getLongVal() {
        return super.getValue1();
    }

    public void setLongVal(Long longVal) {
        super.setValue1(longVal);
    }

    public String getStringVal() {
        return super.getValue2();
    }

    public void setStringVal(String stringVal) {
        super.setValue2(stringVal);
    }

    /**
     * Hiding this method from Json serialization in favor of more explicit method above - getIntVal
     */
    @Override
    @JsonIgnore
    public Long getValue1(){
        return super.getValue1();
    }
    
    /**
     * Hiding this method from Json serialization in favor of more explicit method above - setIntVal
     */
    @Override
    @JsonIgnore
    public void setValue1(Long val){
        super.setValue1(val);
    }

    /**
     * Hiding this method from Json serialization in favor of more explicit method above - getStringVal
     */
    @Override
    @JsonIgnore
    public String getValue2(){
        return super.getValue2();
    }
    
    /**
     * Hiding this method from Json serialization in favor of more explicit method above - setStringVal
     */
    @Override
    @JsonIgnore
    public void setValue2(String val){
        super.setValue2(val);
    }

    @Override
    public PairLongString clone() {
        return (PairLongString) super.clone();
    }
    
    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }
        return false;
    }
    
    public static void main(String[] args) {
        System.out.println(new PairLongString(42,"test42"));
        //{"_type":"PairLongString","longVal":42,"stringVal":"test42"}
    }
}