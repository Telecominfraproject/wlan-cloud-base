package com.example.model_test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class TestVendorModels {

    static {
        System.setProperty("tip.wlan.vendorTopLevelPackages", "com.example.model_test");
        if(!BaseJsonModel.knowsClass("UnrealisticModelForTest")) {
            BaseJsonModel.refreshRegisteredSubtypes();
        }
    }
    
    @Test
    public void testVendorModelExtensions() {
        UnrealisticModelForTest m1 = new UnrealisticModelForTest("m1", 1);
        
        String m1Str = m1.toString();
        
        UnrealisticModelForTest m1d = (UnrealisticModelForTest) BaseJsonModel.fromString(m1Str, BaseJsonModel.class);
        UnrealisticModelForTest m1d1 = BaseJsonModel.fromString(m1Str, UnrealisticModelForTest.class);

        assertEquals(m1Str, m1d.toString());
        assertEquals(m1Str, m1d1.toString());

    }

}
