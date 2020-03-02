package com.telecominfraproject.wlan.core.model.pair;

/**
 * @author dtop
 *
 */
public class PairStringLong extends GenericPair<String, Long> {
    private static final long serialVersionUID = -4555131678496089001L;

    public PairStringLong() {
        //default constructor
    }
    
    public PairStringLong(String strValue, Long lValue) {
        this.setValue1(strValue);
        this.setValue2(lValue);
    }
    
    @Override
    public PairStringLong clone() {
        return (PairStringLong) super.clone();
    }    
}