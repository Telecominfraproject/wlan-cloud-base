package com.telecominfraproject.wlan.core.model.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.testclasses.Vehicle;
import com.telecominfraproject.wlan.core.model.utils.JsonPatchUtil;

public class OriginatingSwapTests {
    private static final Logger LOG = LoggerFactory.getLogger(OriginatingSwapTests.class);

    // This instance changed both the "weird" argument and its colour
    private static final String OLD_DEFAULT_WITH_OBSOLETE_PARAMETER = "{\"Super weird argument\" : \"This\", \"colour\":\"black\",\"numKm\":100,\"peopleOnTheLease\":[\"John\",\"Mary\"]}";
    private static final String OLD_DEFAULT_WITH_OBSOLETE_PARAMETER_AND_NAME = "{\"Super weird argument\" : \"This\",\"name\":\"stinger\", \"colour\":\"black\",\"numKm\":100,\"peopleOnTheLease\":[\"John\",\"Mary\"]}";
    private static final String NEW_DEFAULT_WITHOUT_OBSOLETE_PARAMETER = "{\"colour\":\"black\",\"name\":\"stinger\",\"numKm\":100,\"peopleOnTheLease\":[\"John\",\"Mary\"]}";

    private static final String OLD_INSTANCE_WITH_OBSOLETE_PARAMETER = "{\"Super weird argument\" : \"That\", \"colour\":\"blue\", \"numKm\":100,\"peopleOnTheLease\":[\"John\",\"Mary\"]}";
    private static final String OLD_INSTANCE_WITH_OBSOLETE_PARAMETER_AND_NAME = "{\"Super weird argument\" : \"That\",\"name\":\"stinger\", \"colour\":\"blue\", \"numKm\":100,\"peopleOnTheLease\":[\"John\",\"Mary\"]}";
    private static final String OLD_INSTANCE_WITHOUT_OBSOLETE_PARAMETER = "{\"colour\":\"blue\", \"numKm\":100,\"peopleOnTheLease\":[\"John\",\"Mary\"]}";

    @Test
    /*
     * We'll generate a patch from an old default that has had a parameter
     * removed. We'll apply that patch to a class which doesn't have that
     * argument.
     * 
     * ... in the old diff parser, it yielded a heart attack.
     * 
     * The new instance should also end up with a "blue" color
     */
    public void testWithRemovingADefaultAndUpdatingADefault() throws Exception {
        /* This patch only affect the obsolete argument */
        String oldPatch = JsonPatchUtil.generatePatch(OLD_DEFAULT_WITH_OBSOLETE_PARAMETER,
                OLD_INSTANCE_WITH_OBSOLETE_PARAMETER);

        Vehicle defaultVehicle = Vehicle.fromString(NEW_DEFAULT_WITHOUT_OBSOLETE_PARAMETER, Vehicle.class);

        /* We apply the patch */
        Vehicle patchedVehicle = JsonPatchUtil.apply(defaultVehicle, oldPatch, Vehicle.class);

        assertEquals("blue", patchedVehicle.getColour());
        assertEquals("stinger", patchedVehicle.getName());
    }

    @Test
    public void testWithPatchThatRemovedObsoleteParameters() throws Exception {
        /* This patch only affect the obsolete argument */
        String oldPatch = JsonPatchUtil.generatePatch(OLD_DEFAULT_WITH_OBSOLETE_PARAMETER,
                OLD_INSTANCE_WITHOUT_OBSOLETE_PARAMETER);

        Vehicle defaultVehicle = Vehicle.fromString(NEW_DEFAULT_WITHOUT_OBSOLETE_PARAMETER, Vehicle.class);

        /* We apply the patch */
        Vehicle patchedVehicle = JsonPatchUtil.apply(defaultVehicle, oldPatch, Vehicle.class);

        assertEquals("blue", patchedVehicle.getColour());
        assertEquals("stinger", patchedVehicle.getName());
    }

    @Test
    public void testWithPatchThatAddsAnObsoleteParameters() throws Exception {
        /* This patch only affect the obsolete argument */
        String oldPatch = JsonPatchUtil.generatePatch(NEW_DEFAULT_WITHOUT_OBSOLETE_PARAMETER,
                OLD_INSTANCE_WITH_OBSOLETE_PARAMETER_AND_NAME);

        Vehicle defaultVehicle = Vehicle.fromString(OLD_DEFAULT_WITH_OBSOLETE_PARAMETER_AND_NAME, Vehicle.class);

        /* We apply the patch */
        Vehicle patchedVehicle = JsonPatchUtil.apply(defaultVehicle, oldPatch, Vehicle.class);

        assertEquals("blue", patchedVehicle.getColour());
        assertEquals("stinger", patchedVehicle.getName());
    }

    @Test
    /* We remove a default value */
    public void testAlteringAMapByRemovingObject() throws Exception {
        final String oldDefault = "{\"map\" : { \"a\" : \"A\" , \"b\" : \"B\" } }";
        final String oldInstance = "{\"map\" : { \"a\" : \"A\" , \"b\" : \"BB\" } }";
        final String newDefault = "{\"map\" : { \"b\" : \"B\" } }";

        String oldPath = JsonPatchUtil.generatePatch(oldDefault, oldInstance);
        String newResolvedInstance = JsonPatchUtil.apply(newDefault, oldPath);
        assertEquals("{\"map\":{\"b\":\"BB\"}}", newResolvedInstance);
    }

    @Test
    /* We add a default value */
    public void testAlteringAMapByAddingObjectAndRemoving() throws Exception {
        final String oldDefault = "{\"map\" : { \"a\" : \"A\" , \"b\" : \"B\" } }";
        final String oldInstance = "{\"map\" : { \"a\" : \"A\" , \"b\" : \"BB\" } }";
        final String newDefault = "{\"map\" : { \"b\" : \"B\", \"c\" : \"C\" } }";

        String oldPath = JsonPatchUtil.generatePatch(oldDefault, oldInstance);
        String newResolvedInstance = JsonPatchUtil.apply(newDefault, oldPath);

        LOG.debug("Resolved Instance: {}", newResolvedInstance);

        assertEquals("{\"map\":{\"b\":\"BB\",\"c\":\"C\"}}", newResolvedInstance);
    }

    @Test
    /* We add a default value */
    public void testAlteringAMapByAddingObject() throws Exception {
        final String oldDefault = "{\"map\" : { \"a\" : \"A\" , \"b\" : \"B\" } }";
        final String oldInstance = "{\"map\" : { \"a\" : \"A\" , \"b\" : \"BB\" } }";
        final String newDefault = "{\"map\" : { \"a\" : \"A\" , \"b\" : \"B\", \"c\" : \"C\" } }";

        String oldPath = JsonPatchUtil.generatePatch(oldDefault, oldInstance);
        String newResolvedInstance = JsonPatchUtil.apply(newDefault, oldPath);

        LOG.debug("Resolved Instance: {}", newResolvedInstance);

        assertEquals("{\"map\":{\"a\":\"A\",\"b\":\"BB\",\"c\":\"C\"}}", newResolvedInstance);
    }

    @Test
    /*
     * Since the user has deleted the second element, we don't take any changes
     * to the second element.
     */
    public void testAlteringAListByRemovingObject() throws Exception {
        final String oldDefault = "{\"list\" : [ \"A\" , \"B\" ] }";
        final String oldInstance = "{\"list\" : [ \"A\" , \"C\" ] }";
        final String userInstance = "{\"list\" : [ \"B\" ] }";

        String oldPatch = JsonPatchUtil.generatePatch(oldDefault, oldInstance);

        String patched = JsonPatchUtil.apply(userInstance, oldPatch);
        assertEquals("{\"list\":[\"B\"]}", patched);
    }

    @Test
    public void testReplaceOnNonExistantArgument() throws Exception {
        final String oldWithObsoleteParms = "{\"root\" : { \"sub\" : \"value\" } }";
        final String newerWithObsoleteParms = "{\"root\" : { \"sub\" : \"anotherValue\" } }";
        final String whereWeWantToApplyThePatch = "{ }";

        String patch = JsonPatchUtil.generatePatch(oldWithObsoleteParms, newerWithObsoleteParms);

        JsonPatchUtil.apply(whereWeWantToApplyThePatch, patch);

    }

}
