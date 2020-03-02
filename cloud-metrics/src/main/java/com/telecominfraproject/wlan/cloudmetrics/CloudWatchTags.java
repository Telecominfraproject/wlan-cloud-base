package com.telecominfraproject.wlan.cloudmetrics;

import java.util.Arrays;

import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.tag.aws.AwsInjectableTag;
import com.telecominfraproject.wlan.core.model.utils.SystemAndEnvPropertyResolver;

/**
 * @author dtop
 *
 */
public class CloudWatchTags {

    private static final String UNDEFINED_STR = "undefined";
    private static boolean partitionInitialized;
    private static String currentPartition;
    
    public static final String role = getPropertyAsString("role", UNDEFINED_STR);
    public static final String stack = getPropertyAsString("stack", UNDEFINED_STR);
    public static final String deployment = getPropertyAsString("deployment", UNDEFINED_STR);
    

    public static final TagList commonTags = new BasicTagList(Arrays.asList(
            AwsInjectableTag.INSTANCE_ID,
            AwsInjectableTag.AVAILABILITY_ZONE,
            AwsInjectableTag.LOCAL_IPV4,
            new BasicTag("a2wTag", buildA2WTag())
         ));
    
    private static String buildA2WTag(){
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
