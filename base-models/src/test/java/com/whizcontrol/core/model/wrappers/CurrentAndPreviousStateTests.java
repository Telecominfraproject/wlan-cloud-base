package com.whizcontrol.core.model.wrappers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CurrentAndPreviousStateTests 
{
    public static enum CrapState
    {
        FIRST_STATE,
        SECOND_STATE
    }

    @Test
    public void testBasicSwitching()
    {
        CurrentAndPreviousState<CrapState> myState = new CurrentAndPreviousState<CurrentAndPreviousStateTests.CrapState>(CrapState.FIRST_STATE);

        assertEquals(CrapState.SECOND_STATE, myState.setState(CrapState.SECOND_STATE));
        assertEquals(CrapState.SECOND_STATE, myState.setState(CrapState.SECOND_STATE));
        assertEquals(CrapState.FIRST_STATE, myState.setPreviousState());
    }
    
    
}
