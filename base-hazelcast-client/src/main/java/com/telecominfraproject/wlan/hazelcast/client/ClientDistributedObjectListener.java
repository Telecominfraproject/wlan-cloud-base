package com.telecominfraproject.wlan.hazelcast.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.DistributedObjectEvent;
import com.hazelcast.core.DistributedObjectListener;
import com.hazelcast.core.HazelcastInstance;

/**
 * @author dtop
 * This class cleans up locally-known distributed objects when their remote counterparts get destroyed
 */
public class ClientDistributedObjectListener implements DistributedObjectListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClientDistributedObjectListener.class);

    private final HazelcastInstance hazelcastInstance;
    
    private final static boolean propagateHazelcastDestroyEvents = Boolean.getBoolean("tip.wlan.hazelcast.propagateDestroyEvents");
    
    /**
     * This listener cleans up locally-known distributed objects when their remote counterparts get destroyed
     * @param hazelcastInstance
     */
    public ClientDistributedObjectListener(HazelcastInstance hazelcastInstance){
        this.hazelcastInstance = hazelcastInstance;
    }
    
    @Override
    public void distributedObjectDestroyed(DistributedObjectEvent event) {
        LOG.info("Object destroyed {} : {}", event.getServiceName(), event.getObjectName());
        
        if(propagateHazelcastDestroyEvents){
            //IMPORTANT: this was causing issue with infinite loop of destroy/create events when one of the 
            //  clients re-created the queue before all of the clients were done with destroying it.
            // This needs to be better understood and rewritten before enabling in production.
            String serviceName = event.getServiceName();
            String name = (String) event.getObjectName();
            //Need to iterate through the whole list,  
            //  cannot just call hazelcastInstance.getDistributedObject(service, name) 
            //  because that would implicitly create a new distributed object
            for(DistributedObject distObj: hazelcastInstance.getDistributedObjects()){
                if(distObj.getServiceName().equals(serviceName) && distObj.getName().equals(name)){
                    distObj.destroy();
                }
            }
        }
    }
    
    @Override
    public void distributedObjectCreated(DistributedObjectEvent event) {
        LOG.info("Object created {} : {}", event.getServiceName(), event.getObjectName());
    }

}
