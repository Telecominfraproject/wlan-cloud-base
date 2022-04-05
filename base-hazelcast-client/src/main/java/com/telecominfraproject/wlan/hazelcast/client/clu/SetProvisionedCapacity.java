package com.telecominfraproject.wlan.hazelcast.client.clu;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.security.UsernamePasswordIdentityConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

public class SetProvisionedCapacity {  
    
    private static final String clusterName = System.getProperty("wc.hazelcast.clusterName", "wc-dev");
    private static final String password = System.getProperty("wc.hazelcast.clusterPassword", "wc-dev-pass");
    private static final String addr = System.getProperty("wc.hazelcast.clusterAddr", "127.0.0.1:5900");
    
    public static void main(String[] args) throws Exception {
        
        if(args.length!=2){
            System.out.println("Usage: program re_queue_name provisioned_capacity");
            System.exit(1);
        }

        
        ClientConfig clientConfig = new ClientConfig();
        
        clientConfig.getSecurityConfig().setUsernamePasswordIdentityConfig(new UsernamePasswordIdentityConfig(clusterName, password));
        clientConfig.getNetworkConfig().addAddress(addr);

        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false);
        //clientConfig.getNetworkConfig().setConnectionAttemptLimit(0);

        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
                
        String qName = args[0];
        float newProvisionedCapacity = Float.parseFloat(args[1]);
        
        System.out.format("Setting provisioned capacity for '%s' to %s %n", args[0], args[1]);
        IMap<String,Float> initialReservedCapacityMap = client.getMap("agent-queue-initial-reserved-capacity-map");
        Float oldValue = initialReservedCapacityMap.put(qName, newProvisionedCapacity);
        if(oldValue!=null){
            System.out.println("Replaced old value: " + oldValue);
        }
        
        System.out.println("done.");
        
        System.exit(0);
    }
    
}
