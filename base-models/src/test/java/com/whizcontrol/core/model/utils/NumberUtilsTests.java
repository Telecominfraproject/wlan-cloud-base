package com.whizcontrol.core.model.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NumberUtilsTests 
{
    @Test
    public void testNumberUtils()
    {
        assertEquals(5, NumberUtils.getValueWithinRange(1, 5, 10));
        assertEquals(1, NumberUtils.getValueWithinRange(1, 0, 10));
        assertEquals(10, NumberUtils.getValueWithinRange(1, 11, 10));
    }

    
    @Test
    public void testNumberUtilsWithNegatives()
    {
        assertEquals(-90, NumberUtils.getValueWithinRange(-90, -91, -65));
        assertEquals(-65, NumberUtils.getValueWithinRange(-90, -64, -65));
        assertEquals(-70, NumberUtils.getValueWithinRange(-90, -70, -65));
    }

    
}
