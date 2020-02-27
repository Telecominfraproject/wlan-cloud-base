package com.whizcontrol.core.model.pair;

/**
 * @author dtop
 *
 */
public class PairIntLong extends GenericPair<Integer, Long>{
    private static final long serialVersionUID = 7865126635852022432L;

    public PairIntLong() {
    }
    
    public PairIntLong(int intVal, long longVal) {
        super(intVal, longVal);
    }

    @Override
    public PairIntLong clone() {
        return (PairIntLong) super.clone();
    }
    
    public Integer getIntVal() {
        return super.getValue1();
    }

    public void setIntVal(Integer intVal) {
        super.setValue1(intVal);
    }

    public Long getLongVal() {
        return super.getValue2();
    }

    public void setLongVal(Long longVal) {
        super.setValue2(longVal);
    }    
}