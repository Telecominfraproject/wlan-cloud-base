package com.telecominfraproject.wlan.core.model.pair;

/**
 * @author dtop
 *
 */
public class PairIntInt extends GenericPair<Integer, Integer>{
    private static final long serialVersionUID = 603121536875075675L;

    public PairIntInt() {
    }
    
    public PairIntInt(int v1, int v2) {
        this.setValue1(v1);
        this.setValue2(v2);
    }
    
    @Override
    public PairIntInt clone() {
        return (PairIntInt) super.clone();
    }
}