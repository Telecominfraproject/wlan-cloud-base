package com.telecominfraproject.wlan.core.model.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.utils.ReflectionUtil;

public class ReflectionUtilTests 
{
    private static class ClassA
    {
        private int lastTime;
        
        public ClassA()
        {
            
        }
        
    }

    private static class ClassB extends ClassA
    {
        private int otherTime;
        
        public ClassB()
        {
            super();
        }
        
    }
    
    @Test
    public void testBasic()
    {
        ClassA a = new ClassA();
        assertTrue(ReflectionUtil.containsEitherFields(a, "lastTime"));
        assertFalse(ReflectionUtil.containsEitherFields(a, "firstTime"));
    }
    
    @Test
    public void testInheritance()
    {
        ClassB b = new ClassB();
        assertTrue(ReflectionUtil.containsEitherFields(b, "lastTime"));
        assertTrue(ReflectionUtil.containsEitherFields(b, "otherTime"));
        assertTrue(ReflectionUtil.containsEitherFields(b, "lastTime", "otherTime"));
        assertTrue(ReflectionUtil.containsEitherFields(b, "random", "otherTime"));
        assertFalse(ReflectionUtil.containsEitherFields(b, "else", "spomethingelse"));
    }
    
}
