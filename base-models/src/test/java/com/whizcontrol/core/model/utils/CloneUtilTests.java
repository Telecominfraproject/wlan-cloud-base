package com.whizcontrol.core.model.utils;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.whizcontrol.core.model.json.BaseJsonModel;

public class CloneUtilTests {

    @Test
    public void testBasicTest() {
        SuperClass superClass = new SuperClass();
        superClass.setTest("MyValue!");

        ChildClass childClass = CloneUtil.createBlankChildFromParent(superClass, ChildClass.class);

        assertNotNull(childClass);
        assertEquals("MyValue!", childClass.getTest());
        assertNull(childClass.getChildValue()); // shouldn't have a value
    }

    @Test
    public void testListOfParentType() {
        ChildClass superClass = new ChildClass();
        superClass.setTest("MyValue!");

        List<SuperClass> childClass = CloneUtil.createListOfParentType(Collections.singletonList(superClass));

        assertNotNull(childClass);
        assertEquals(1, childClass.size()); // shouldn't have a value

    }

    @SuppressWarnings("serial")
    private static class SuperClass extends BaseJsonModel {
        private final static long THIS_IS_A_PRIVATE_STATIC = 12L;

        private String test;

        public SuperClass() {
            // nothing
        }

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

    }

    @SuppressWarnings("serial")
    private static class ChildClass extends SuperClass {
        private String childValue;

        public ChildClass() {
            super();
        }

        public String getChildValue() {
            return childValue;
        }

        public void setChildValue(String childValue) {
            this.childValue = childValue;
        }
    }

}
