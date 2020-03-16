package com.telecominfraproject.wlan.cloudmetrics;

import java.util.Arrays;

import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.core.model.utils.SystemAndEnvPropertyResolver;

/**
 * @author dtop
 *
 */
public class CloudMetricsTags {

    private static final String UNDEFINED_STR = "undefined";
    private static boolean partitionInitialized;
    private static String currentPartition;
    

    
    public static final String instanceId = getPropertyAsString("container.instanceId", UNDEFINED_STR);
    public static final String role = getPropertyAsString("container.role", UNDEFINED_STR);
    public static final String stack = getPropertyAsString("container.stack", UNDEFINED_STR);
    public static final String deployment = getPropertyAsString("container.deployment", UNDEFINED_STR);
    public static final String localIpV4 = getPropertyAsString("container.localipv4", UNDEFINED_STR);
    

    public static final TagList commonTags = new BasicTagList(Arrays.asList(
            new BasicTag("InstanceId", instanceId),
            new BasicTag("local-ipv4", localIpV4),
            new BasicTag("cloudTag", buildCloudTag())
         ));
    
    private static String buildCloudTag(){
        return role + "-" + stack + "-" + deployment;
    }
    
    public static String getPropertyAsString(String propertyName, String defaultValue) {
        return SystemAndEnvPropertyResolver.getPropertyAsString(propertyName, defaultValue);
    }  
    
    public static String getCurrentPartition(){
        if(!partitionInitialized){
            partitionInitialized = true;
            
            if(UNDEFINED_STR.equals(stack)){
                currentPartition = null;
                return currentPartition;
            }
            
            currentPartition = stack + "-" + deployment;
        }
        
        return currentPartition;
    }
    
    /**
     * Used only in unit tests to be able to set different partitions (sequentially in time) inside single VM
     */
    public static void resetPartition(){
        partitionInitialized = false;
        currentPartition = null;
    }
}
