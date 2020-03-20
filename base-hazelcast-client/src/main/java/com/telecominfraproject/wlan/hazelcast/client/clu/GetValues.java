package com.telecominfraproject.wlan.hazelcast.client.clu;

import java.util.Map.Entry;
import java.util.Set;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.properties.GroupProperty;
import com.telecominfraproject.wlan.hazelcast.common.HazelcastObjectsConfiguration;

public class GetValues 
{
    
    private static final String clusterName = System.getProperty("wc.hazelcast.clusterName", "wc-dev");
    private static final String password = System.getProperty("wc.hazelcast.clusterPassword", "wc-dev-pass");
    private static final String addr = System.getProperty("wc.hazelcast.clusterAddr", "127.0.0.1:5701");
    
    public static void main(String[] args) throws Exception {
        
//        if(args.length==1){
//            System.out.println("Usage: program hashKey");
//            System.exit(1);
//        }

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setProperty(GroupProperty.LOGGING_TYPE.getName(), "slf4j");
        clientConfig.setProperty(GroupProperty.PHONE_HOME_ENABLED.getName(), "false");
        
        clientConfig.getGroupConfig().setName(clusterName).setPassword(password);
        clientConfig.getNetworkConfig().addAddress(addr);

        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
        HazelcastObjectsConfiguration hazelcastObjectsConfiguration = HazelcastObjectsConfiguration.createOutsideOfSpringApp();
                
        IMap<Integer, Set<Long>> hashValues = client.getMap(hazelcastObjectsConfiguration.getHdsDirectoryCustomerEquipmentMapName());
        
        if(hashValues != null)
        {
            for(Entry<Integer, Set<Long>> entry : hashValues.entrySet())
            {
                System.out.println("CustomerId: " + entry.getKey());
                System.out.println("EquipmentIds: " + entry.getValue());
            }
        }
        else
        {
            System.out.println("No values found.");
        }
        
        System.out.println("done.");
        
        System.exit(0);
    }

}
