package com.telecominfraproject.wlan.hazelcast.client;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.collection.impl.queue.QueueService;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.DistributedObjectEvent;
import com.hazelcast.core.DistributedObjectListener;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.query.Predicate;
import com.telecominfraproject.wlan.hazelcast.common.SamplePredicate;

public class TestClient {

    public static void main_1(String[] args) throws IOException {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName("wc-dev").setPassword("wc-dev-pass");
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5900");
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5901");
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5902");
        
        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false); 

        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);

        Map<Integer, String> metricsPerDevice = client.getMap( "metricsPerDevice" );
        
        System.out.println("metricsMap: " + metricsPerDevice);
        for(Map.Entry<Integer, String> entry: metricsPerDevice.entrySet()){
            System.out.println("metricsMap["+ entry.getKey() +"]: " +entry.getValue());
        }

        IdGenerator testIdGenerator = client.getIdGenerator("id_generator_created_from_client");
        
        Map<Long, String> mapCreatedFromClient = client.getMap( "map_created_from_client" );
        System.out.println("mapCreatedFromClient: " + mapCreatedFromClient);
        mapCreatedFromClient.put(testIdGenerator.newId(), Long.toString(System.currentTimeMillis()));
        for(Map.Entry<Long, String> entry: mapCreatedFromClient.entrySet()){
            System.out.println("mapCreatedFromClient["+ entry.getKey() +"]: " +entry.getValue());
        }

        Map<Long, String> mapDynamicName = client.getMap( "metricsPerDevice-with-expiration-"+8 );
        long lv = testIdGenerator.newId();
        mapDynamicName.put(lv, "value-"+lv);
        System.out.println("mapDynamicName: " + mapDynamicName);
        for(Map.Entry<Long, String> entry: mapDynamicName.entrySet()){
            System.out.println("mapDynamicName["+ entry.getKey() +"]: " +entry.getValue());
        }
        
        
        System.out.println("Press Enter to terminate the client");
        System.in.read();
        System.exit(0);

    }
    
    
    private static final String clusterName = System.getProperty("wc.hazelcast.clusterName", "wc-dev");
    private static final String password = System.getProperty("wc.hazelcast.clusterPassword", "wc-dev-pass");
    private static final String addr = System.getProperty("wc.hazelcast.clusterAddr", "127.0.0.1:5900");
    
    private static class TestMapListener implements EntryAddedListener<String, String>, 
            EntryRemovedListener<String, String>, 
            EntryEvictedListener<String, String>, Serializable {
        
        private static final long serialVersionUID = -1669018710360608086L;
        
        private static final Logger LOG = LoggerFactory.getLogger(TestMapListener.class);
                
        @Override
        public void entryEvicted(EntryEvent<String, String> event) {
            LOG.info( "{} Entry Evicted: {}", event.getSource(), event.getKey() );
        }
        
        @Override
        public void entryRemoved(EntryEvent<String, String> event) {
            LOG.info( "{} Entry Removed: {}", event.getSource(), event.getKey() );
        }
        
        @Override
        public void entryAdded(EntryEvent<String, String> event) {
            LOG.info( "{} Entry Added: {}", event.getSource(), event.getKey() );        
        }
        
        
        //This class is a singleton, all instances are meant to be equal
        @Override
        public int hashCode() {
            return 1;
        }
        
        @Override
        public boolean equals(Object obj) {
            return obj instanceof TestMapListener;
        }
    }
    
    public static void main_2(String[] args) throws InterruptedException {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName(clusterName).setPassword(password);
        clientConfig.getNetworkConfig().addAddress(addr);
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5901");
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5902");

        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false); 

        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);

        //String[] mapNames = {"s3CreationTs-sm-", "s3CreationTs-sm_x5m-", "s3CreationTs-sm_x15m-"};
        String[] mapNames = {"testMap"};
        
        for(String mName: mapNames){
            IMap<String, String> map = client.getMap( mName );
            
            map.addEntryListener(new TestMapListener(), true);
            
            map.put("t1", "v1", 15, TimeUnit.SECONDS);
            map.put("t2", "v2", 20, TimeUnit.SECONDS);
            map.put("t3", "v3");
            map.put("t4", "v4");
            System.out.println("Map: " + mName+ " size = "+ map.size());

            for(Map.Entry<String,String> entry: map.entrySet()){
                System.out.format("%s -> %s %n", entry.getKey(), entry.getValue());
            }
        }
        
        System.out.println("Waiting 3...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(3L));
        for(String mName: mapNames){
            IMap<String,String> map = client.getMap( mName );
            map.put("t3", "v3");
            System.out.println("Map: " + mName+ " size = "+ map.size());
            for(Map.Entry<String,String> entry: map.entrySet()){
                System.out.format("%s -> %s %n", entry.getKey(), entry.getValue());
            }
        }

        System.out.println("Waiting 7...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(7L));

        for(String mName: mapNames){
            IMap<String,String> map = client.getMap( mName );
            map.put("t1", "v1", 15, TimeUnit.SECONDS);
            System.out.println("Map: " + mName+ " size = "+ map.size());
            for(Map.Entry<String,String> entry: map.entrySet()){
                System.out.format("%s -> %s %n", entry.getKey(), entry.getValue());
            }
        }

        System.out.println("Waiting 10...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(10L));

        for(String mName: mapNames){
            Map<String,String> map = client.getMap( mName );
            System.out.println("Map: " + mName+ " size = "+ map.size());
            for(Map.Entry<String,String> entry: map.entrySet()){
                System.out.format("%s -> %s %n", entry.getKey(), entry.getValue());
            }
        }

        System.out.println("Waiting 10...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(10L));

        for(String mName: mapNames){
            Map<String,String> map = client.getMap( mName );
            System.out.println("Map: " + mName+ " size = "+ map.size());
            for(Map.Entry<String,String> entry: map.entrySet()){
                System.out.format("%s -> %s %n", entry.getKey(), entry.getValue());
            }
        }

        //Important!!!
        //Eviction may happen before specified time - when Hazelcast nodes are stopped/restarted/removed/added 
        //  (if the entry lives in the affected Hazelcast node)
        //Also, eviction events may be duplicated during cluster re-partitioning
        //
        //Cannot rely on eviction events for maintaining up-to-date data structures
        //
        
        //testing lease acquisition and extension
        System.out.println("Testing Lease acquisition and renewal");
        for(int i = 0; i<10; i++){
            System.out.println("Waiting 10...");
            Thread.sleep(TimeUnit.SECONDS.toMillis(10L));
    
            for(String mName: mapNames){
                IMap<String,String> map = client.getMap( mName );
                map.put("t10_lease", "v_"+System.currentTimeMillis(), 15, TimeUnit.SECONDS);
                System.out.println("Map: " + mName+ " size = "+ map.size());
                for(Map.Entry<String,String> entry: map.entrySet()){
                    System.out.format("%s -> %s %n", entry.getKey(), entry.getValue());
                }
            }
        }
        
        System.out.println("Stopped lease renewal");

        System.out.println("Waiting 20...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(20L));

        for(String mName: mapNames){
            Map<String,String> map = client.getMap( mName );
            System.out.println("Map: " + mName+ " size = "+ map.size());
            for(Map.Entry<String,String> entry: map.entrySet()){
                System.out.format("%s -> %s %n", entry.getKey(), entry.getValue());
            }
        }

        System.exit(0);

    }


    public static void main_3(String[] args) throws InterruptedException, ExecutionException {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName(clusterName).setPassword(password);
        clientConfig.getNetworkConfig().addAddress(addr);
//        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5901");
//        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5902");

        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false);
        clientConfig.getNetworkConfig().setConnectionAttemptLimit(0);

        //HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
        HazelcastInstance client = new ReConnectingHazelcastClient(clientConfig, 20);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, Integer.getInteger("maxTestThreads",10), 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
        
        long start = System.currentTimeMillis();
        long end;
        
        if(Boolean.getBoolean("populateMapBeforeTest")){
            @SuppressWarnings("rawtypes")
			List<Future> futures = new ArrayList<>(1000);

            //populate map with entries        
            for(int i=0; i<1000000; i++){
                final int fI = i;
//                for(int retries=0;retries<100000; retries++){
//                    try{
//                        Future f = executor.submit(new Runnable() {
//                            @Override
//                            public void run() {
//                                map.put("t_"+fI, ""+fI);
//                            }
//                        });
//                        futures.add(f);
//                        break;
//                    }catch(RejectedExecutionException e){
//                        Thread.sleep(100);
//                        continue;
//                    }
//                }

                futures.add(client.getMap("testMap").putAsync("t_" + Integer.toString(fI), Integer.toString(fI)));

                //wait for a batch of futures to complete
                if(futures.size()>=990){
                    for(Future<?> f: futures){
                        f.get();
                    }
                    
                    futures.clear();
                }
                
            }

            for(Future<?> f: futures){
                f.get();
            }

            end = System.currentTimeMillis();
            
            System.out.println("Map: size = "+ client.getMap( "testMap" ).size() + " took "+(end - start)+" ms to populate");
        }
        
        //measure time it takes to iterate through the whole dataset
        start = System.currentTimeMillis();
        
        //iterate through all map entries
        int evenCount = 0;
        for(IMap.Entry<?,?> entry: client.getMap( "testMap" ).entrySet()){
            //map.tryLock(key, time, timeunit, leaseTime, leaseTimeunit)
            if(Integer.parseInt((String)entry.getValue())%2==0){
                evenCount++;
            }
        }
        
        end = System.currentTimeMillis();

        System.out.println("Map: size = "+ client.getMap( "testMap" ).size() + " took "+(end - start)+" ms to iterate through all entries. Found "+ evenCount + " even values");

        if(Boolean.getBoolean("iterateWithPredicateTest")){
            //iterate through all map entries with predicate - in all members in parallel
            evenCount = 0;
            Predicate<String, String> predicate = new SamplePredicate();
            
            for(Map.Entry<?, ?> entry: client.getMap( "testMap" ).entrySet(predicate )){
                evenCount++;
            }
            
            end = System.currentTimeMillis();
    
            System.out.println("Map: size = "+ client.getMap( "testMap" ).size() + " took "+(end - start)+" ms to iterate through all entries with predicate (in parallel). Found "+ evenCount + " even values");
        }
        
        if(Boolean.getBoolean("deleteMapAfterTest")){
            client.getMap( "testMap" ).destroy();
        }
        
        System.exit(0);

    }

    public static void main_4(String[] args) {
        
        if(args.length!=2){
            System.out.println("Usage: program serviceName objectName");
            System.out.println("Where serviceName is one of: ");
            String[] services = {"hz:impl:queueService", "hz:impl:mapService", "hz:impl:atomicLongService", "or any SERVICE_NAME of descentants of com.hazelcast.spi.ManagedService" };
            for(String s: services){
                System.out.println(s);
            }
            
            System.exit(1);
        }

        
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName(clusterName).setPassword(password);
        clientConfig.getNetworkConfig().addAddress(addr);

        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false);
        clientConfig.getNetworkConfig().setConnectionAttemptLimit(0);

        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
                
        String serviceName = args[0];
        String objectName = args[1];
        
        System.out.format("Removing object %s:%s %n", serviceName, objectName);
        DistributedObject obj = client.getDistributedObject(serviceName, objectName);
        obj.destroy();
        
        if("hz:impl:queueService".equals(serviceName) && objectName.startsWith("re-q-Cu_")){
            client.getMap("rule-agent-q-assignments-map").remove(objectName);
            client.getMap("unassigned-re-queues-map").remove(objectName);
            client.getMap("agent-queue-initial-reserved-capacity-map").remove(objectName);
        }
        
        System.out.println("done.");
        
        System.exit(0);
    }
    
    public static void main_5(String[] args) {
                
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName(clusterName).setPassword(password);
        clientConfig.getNetworkConfig().addAddress(addr);

        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false);
        clientConfig.getNetworkConfig().setConnectionAttemptLimit(0);

        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
                
        String testQName = "re-q-Cu_1";       
        IMap<String,String> qAssignmentsMap = client.getMap("rule-agent-q-assignments-map");
        //qAssignmentsMap.put(testQName, ""+System.currentTimeMillis());
        
        if(qAssignmentsMap.tryLock(testQName)){
            if(qAssignmentsMap.get(testQName)==null){
                System.out.println("Entry does not exist");
            } else {
                System.out.println("Entry exists : "+ qAssignmentsMap.get(testQName));
            }
            qAssignmentsMap.unlock(testQName);
        }
        
        qAssignmentsMap.delete(testQName);
        
        IQueue<byte[]> queue = client.getQueue(testQName);
        System.out.println("Entry exists : '"+ qAssignmentsMap.get(testQName)+"'");
        queue.clear();
        System.out.println("Entry exists : '"+ qAssignmentsMap.get(testQName)+"'");
        System.out.println("Queue cleared. Size = "+ queue.size());
        queue.destroy();
        System.out.println("Entry exists : "+ qAssignmentsMap.get(testQName));
        System.out.println("Queue destroyed. Size = "+ queue.size());
        
        System.exit(0);
    }
    
    public static void main_6(String[] args) {
        
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName(clusterName).setPassword(password);
        clientConfig.getNetworkConfig().addAddress(addr);

        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false);
        clientConfig.getNetworkConfig().setConnectionAttemptLimit(0);

        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
                
        String testQName = "re-q-Cu_1";       
        IQueue<byte[]> agentQueue = client.getQueue(testQName);
        
        while(true){
            byte[] eventBytes = ("evt-"+System.currentTimeMillis()).getBytes();
            if(!agentQueue.offer(eventBytes)){
                //agentQueue is full and cannot take any more events. 
                //we will drain it of old events and insert our new event.
                agentQueue.clear();
                System.err.println("Cleared queue "+ testQName);
                
                if(!agentQueue.offer(eventBytes)){
                    System.err.println("Cannot enqueue event " + testQName);
                }
            }
            
            if(agentQueue.size()%1000 == 0){
                System.out.println("Enqueued 1000 events, queue size is "+ agentQueue.size());
            }
        }
        
        //System.exit(0);
    }

    
    public static void main_7(String[] args) {
        
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName(clusterName).setPassword(password);
        clientConfig.getNetworkConfig().addAddress(addr);

        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false);
        clientConfig.getNetworkConfig().setConnectionAttemptLimit(0);

        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
                
        String testQName = "re-q-Cu_1";       
        IQueue<byte[]> agentQueue = client.getQueue(testQName);
        
        while(true){

            List<byte[]> batchBytes = new ArrayList<>();
            for(int i=0; i<20; i++){
                batchBytes.add(("evt-"+System.currentTimeMillis()).getBytes());
            }
            
            boolean addedSuccessfully = false;
            try{
                addedSuccessfully = agentQueue.addAll(batchBytes);
            }catch(IllegalStateException e){
                //do nothing
            }
            
            if(!addedSuccessfully){
                //agentQueue is full and cannot take any more events. 
                //we will drain it of old events and insert our new events.
                agentQueue.clear();
                System.err.println("Cleared queue "+ testQName);
                
                //try again the same operation
                try{
                    addedSuccessfully = agentQueue.addAll(batchBytes);
                }catch(IllegalStateException e1){
                    //do nothing
                }
                
                if(!addedSuccessfully) {
                    System.err.println("Cannot enqueue event " + testQName);
                }
                
            }

            if(agentQueue.size()%1000 == 0){
                System.out.println("Enqueued 1000 events, queue size is "+ agentQueue.size());
            }
            

        }
        
        //System.exit(0);
    }

    public static void main(String[] args) {
        
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName(clusterName).setPassword(password);
        clientConfig.getNetworkConfig().addAddress(addr);

        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false);
        clientConfig.getNetworkConfig().setConnectionAttemptLimit(0);

        HazelcastInstance hazelcastClient = HazelcastClient.newHazelcastClient(clientConfig);

        DistributedObjectListener distributedObjectListener = new DistributedObjectListener() {
            
            @Override
            public void distributedObjectDestroyed(DistributedObjectEvent event) {
                System.out.println("Object destroyed " + event.getServiceName() +" : " + event.getObjectName());
                String serviceName = event.getServiceName();
                String name = (String) event.getObjectName();
                for(DistributedObject distObj: hazelcastClient.getDistributedObjects()){
                    if(distObj.getServiceName().equals(serviceName) && distObj.getName().equals(name)){
                        distObj.destroy();
                    }
                }
            }
            
            @Override
            public void distributedObjectCreated(DistributedObjectEvent event) {
                System.out.println("Object created " + event.getServiceName() +" : " + event.getObjectName());
            }
        };
        hazelcastClient.addDistributedObjectListener(distributedObjectListener );

        
        System.err.println("*** initial state ***");

        for(DistributedObject distributedObject: hazelcastClient.getDistributedObjects()){            
            if(distributedObject.getServiceName().equals(QueueService.SERVICE_NAME)
                    && distributedObject.getName().startsWith("re-q-")
                    ){
                System.out.println(distributedObject.getName());
            }
        }
        
        String queueName = "re-q-Cu_1";        
        IQueue<byte[]> agentQueue = hazelcastClient.getQueue(queueName);
        agentQueue.size();

        System.err.println("*** after queue.size() ***");

        for(DistributedObject distributedObject: hazelcastClient.getDistributedObjects()){            
            if(distributedObject.getServiceName().equals(QueueService.SERVICE_NAME)
                    && distributedObject.getName().startsWith("re-q-")
                    ){
                System.out.println(distributedObject.getName());
            }
        }

        agentQueue.destroy();
        System.err.println("*** after queue.destroy() ***");

        for(DistributedObject distributedObject: hazelcastClient.getDistributedObjects()){            
            if(distributedObject.getServiceName().equals(QueueService.SERVICE_NAME)
                    && distributedObject.getName().startsWith("re-q-")
                    ){
                System.out.println(distributedObject.getName());
            }
        }

        System.err.println("*** done ***");
        
//        System.exit(0);
    }
    
    public static void main_9(String[] args) throws InterruptedException {
        
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName(clusterName).setPassword(password);
        clientConfig.getNetworkConfig().addAddress(addr);

        //see http://docs.hazelcast.org/docs/3.6/manual/html-single/index.html#java-client-operation-modes
        // here we're using "dumb" client that connects only to a single node of the cluster
        clientConfig.getNetworkConfig().setSmartRouting(false);
        clientConfig.getNetworkConfig().setConnectionAttemptLimit(0);

        HazelcastInstance hazelcastClient = HazelcastClient.newHazelcastClient(clientConfig);
        DistributedObjectListener distributedObjectListener = new DistributedObjectListener() {
            
            @Override
            public void distributedObjectDestroyed(DistributedObjectEvent event) {
                System.out.println("Object destroyed " + event.getServiceName() +" : " + event.getObjectName());
                String serviceName = event.getServiceName();
                String name = (String) event.getObjectName();
                for(DistributedObject distObj: hazelcastClient.getDistributedObjects()){
                    if(distObj.getServiceName().equals(serviceName) && distObj.getName().equals(name)){
                        distObj.destroy();
                    }
                }
            }
            
            @Override
            public void distributedObjectCreated(DistributedObjectEvent event) {
                System.out.println("Object created " + event.getServiceName() +" : " + event.getObjectName());
            }
        };
        hazelcastClient.addDistributedObjectListener(distributedObjectListener );

        while(true){
            Thread.sleep(5000);
            System.err.println("*** -------------------------------- ***");
    
            for(DistributedObject distributedObject: hazelcastClient.getDistributedObjects()){            
                if(distributedObject.getServiceName().equals(QueueService.SERVICE_NAME)
                        && distributedObject.getName().startsWith("re-q-")
                        ){
                    System.err.println(distributedObject.getName());
                }
            }
            System.err.println("*** =============================== ***");

        }
                
        //System.exit(0);
    }
}
