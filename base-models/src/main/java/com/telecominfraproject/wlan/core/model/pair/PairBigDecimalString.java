package com.telecominfraproject.wlan.core.model.pair;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author dtop
 *
 */
public class PairBigDecimalString extends GenericPair<BigDecimal, String>{
    private static final long serialVersionUID = 2927226074785057554L;

    public PairBigDecimalString() {
    }

    public PairBigDecimalString(BigDecimal bdVal, String stringVal) {
        super(bdVal, stringVal);
    }

    public BigDecimal getBigDecimalVal() {
        return super.getValue1();
    }

    public void setBigDecimalVal(BigDecimal bdVal) {
        super.setValue1(bdVal);
    }

    public String getStringVal() {
        return super.getValue2();
    }

    public void setStringVal(String stringVal) {
        super.setValue2(stringVal);
    }

    /**
     * Hiding this method from Json serialization in favor of more explicit method above - getBigDecimalVal
     */
    @Override
    @JsonIgnore
    public BigDecimal getValue1(){
        return super.getValue1();
    }
    
    /**
     * Hiding this method from Json serialization in favor of more explicit method above - setBigDecimalVal
     */
    @Override
    @JsonIgnore
    public void setValue1(BigDecimal val){
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
    public PairBigDecimalString clone() {
        return (PairBigDecimalString) super.clone();
    }
    
    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }
        return false;
    }
    
    public static void main(String[] args) {
        System.out.println(new PairBigDecimalString(new BigDecimal(42),"test42"));
        //{"_type":"PairIntString","bigDecimalVal":42,"stringVal":"test42"}
    }
}