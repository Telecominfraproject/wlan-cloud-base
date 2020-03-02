package com.telecominfraproject.wlan.core.model.pair;

/**
 * @author dtop
 *
 */
public class PairStringString extends GenericPair<String, String> {
    private static final long serialVersionUID = -4555131678496089001L;

    public PairStringString() {
        //default constructor
    }
    
    public PairStringString(String strValue1, String strValue2) {
        this.setValue1(strValue1);
        this.setValue2(strValue2);
    }
    
    @Override
    public PairStringString clone() {
        return (PairStringString) super.clone();
    }    
}