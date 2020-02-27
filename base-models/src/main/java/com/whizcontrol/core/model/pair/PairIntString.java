package com.whizcontrol.core.model.pair;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author dtop
 *
 */
public class PairIntString extends GenericPair<Integer, String>{
    private static final long serialVersionUID = 2927226074785057554L;

    public PairIntString() {
    }

    public PairIntString(int intVal, String stringVal) {
        super(intVal, stringVal);
    }

    public Integer getIntVal() {
        return super.getValue1();
    }

    public void setIntVal(Integer intVal) {
        super.setValue1(intVal);
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
    public Integer getValue1(){
        return super.getValue1();
    }
    
    /**
     * Hiding this method from Json serialization in favor of more explicit method above - setIntVal
     */
    @Override
    @JsonIgnore
    public void setValue1(Integer val){
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
    public PairIntString clone() {
        return (PairIntString) super.clone();
    }
    
    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }
        return false;
    }
    
    public static void main(String[] args) {
        System.out.println(new PairIntString(42,"test42"));
        //{"_type":"PairIntString","intVal":42,"stringVal":"test42"}
    }
}