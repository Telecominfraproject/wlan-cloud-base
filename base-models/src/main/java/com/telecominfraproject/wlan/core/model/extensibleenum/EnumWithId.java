package com.telecominfraproject.wlan.core.model.extensibleenum;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Interface used by all Enumeration-like classes that are meant to be extended by the 3rd-party vendors.
 * <br>These classes behave like java enums, but can be extended by other classes to add more elements.
 * <br>
 * <br>For example of implementation of such an extensible enum:
 * <br> @see com.telecominfraproject.wlan.core.model.extensibleenum.example.OriginalEnumExample
 * <br> @see com.telecominfraproject.wlan.core.model.extensibleenum.example.ExtendedEnumExample
 * <br> @see com.telecominfraproject.wlan.core.model.extensibleenum.example.VendorExtendedEnumExampleModel
 * <br> @author dtop
 *
 */
@JsonSerialize(using=EnumWithIdSerializer.class)
public interface EnumWithId {
    int getId();
    String getName();
}
