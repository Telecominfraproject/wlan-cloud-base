package com.telecominfraproject.wlan.core.model.pair;


/**
 * @author dtop
 *
 */
public class PairLongLong extends GenericPair<Long, Long> 
{
    private static final long serialVersionUID = -6528772057665324888L;

    public PairLongLong()
    {
        super();
    }
    
    public PairLongLong(long left, long right)
    {
        super(left, right);
    }
        
    @Override
    public PairLongLong clone() {
        return (PairLongLong)super.clone();
    }
}