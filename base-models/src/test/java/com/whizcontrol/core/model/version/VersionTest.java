package com.whizcontrol.core.model.version;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {

    @Test
    public void testVersionComparison() throws Exception {
        Version ver1 = new Version();
        ver1.setBranch("branch");
        ver1.setMajor(1);
        ver1.setMinor(1);
        ver1.setPatch(1);
        
        Version ver2 = new Version();
        ver2.setBranch("branch");
        ver2.setMajor(1);
        ver2.setMinor(1);
        ver2.setPatch(1);
        
        assertTrue(ver1.compareTo(ver2) == 0);
        
        ver2.setBranch("not-branch");
        assertTrue(ver1.compareTo(ver2) == 0);
        assertFalse(ver1.equals(ver2));
        
        ver2.setMajor(2);
        assertTrue(ver1.compareTo(ver2) == -1);
        assertTrue(ver2.compareTo(ver1) == 1);
        
        ver2.setMajor(1);
        ver2.setMinor(2);
        assertTrue(ver1.compareTo(ver2) == -1);
        assertTrue(ver2.compareTo(ver1) == 1);
        
        ver2.setMinor(1);
        ver2.setPatch(2);
        assertTrue(ver1.compareTo(ver2) == -1);
        assertTrue(ver2.compareTo(ver1) == 1);
    }
}
