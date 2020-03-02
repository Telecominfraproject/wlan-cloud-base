/**
 * 
 */
package com.telecominfraproject.wlan.core.model.service.subscription;

import java.util.concurrent.TimeUnit;

/**
 * @author yongli
 *
 */
public class SubscriptionConstants {

    public static final long DefaultNotifyBeforeSubscriptionConversionTrialToFullMs = TimeUnit.DAYS.toMillis(5); // 5
                                                                                                                 // days
    public static final long DefaultNotifyBeforeSubscriptionRenewalMs = TimeUnit.DAYS.toMillis(30); // 30
                                                                                                    // days
    public static final long DefaultLengthOfTrialSubscriptionMs = TimeUnit.DAYS.toMillis(30); // 30
                                                                                              // days
    public static final long DefaultLengthOfFullSubscriptionMs = TimeUnit.DAYS.toMillis(2L * 356); // 2
                                                                                                  // years

}
