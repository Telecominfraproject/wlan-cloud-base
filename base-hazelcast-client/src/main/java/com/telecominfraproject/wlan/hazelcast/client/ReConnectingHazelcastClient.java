package com.telecominfraproject.wlan.hazelcast.client;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.cardinality.CardinalityEstimator;
import com.hazelcast.client.ClientService;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Endpoint;
import com.hazelcast.collection.IList;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.config.Config;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.DistributedObjectListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICacheManager;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.crdt.pncounter.PNCounter;
import com.hazelcast.durableexecutor.DurableExecutorService;
import com.hazelcast.flakeidgen.FlakeIdGenerator;
import com.hazelcast.logging.LoggingService;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.partition.PartitionService;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.hazelcast.scheduledexecutor.IScheduledExecutorService;
import com.hazelcast.splitbrainprotection.SplitBrainProtectionService;
import com.hazelcast.sql.SqlService;
import com.hazelcast.topic.ITopic;
import com.hazelcast.transaction.HazelcastXAResource;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionException;
import com.hazelcast.transaction.TransactionOptions;
import com.hazelcast.transaction.TransactionalTask;

/**
 * Wrapper for the HazelcastClient that reconnects to the HC cluster at regular
 * intervals to spread the load across HC nodes
 * 
 * @author dtop
 *
 */
public class ReConnectingHazelcastClient implements HazelcastInstance {

    private static final Logger LOG = LoggerFactory.getLogger(ReConnectingHazelcastClient.class);

    private final ClientConfig clientConfig;
    
    private HazelcastInstance client;
    private boolean isTimeToReConnect;
    
    public ReConnectingHazelcastClient(ClientConfig clientConfig, int reconnectTimeSec){
        this.clientConfig = clientConfig;
        this.client = newHazelcastClient();
        
        if(reconnectTimeSec>0){
            Thread thr = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        try{
                            Thread.sleep(1000L * reconnectTimeSec);
                        }catch(InterruptedException e){
                            Thread.currentThread().interrupt();
                            break;
                        }
                        
                        synchronized(ReConnectingHazelcastClient.this){
                            isTimeToReConnect = true;
                        }
                    }
                }
            },"HC-Reconnect-kicker-thread");
            thr.setDaemon(true);
            thr.start();
        }
    }
    
    private HazelcastInstance newHazelcastClient(){
        HazelcastInstance c = HazelcastClient.newHazelcastClient(clientConfig);
        c.addDistributedObjectListener(new ClientDistributedObjectListener(c));
        return c;
    }
    
    private synchronized HazelcastInstance getClient(){
        if(isTimeToReConnect){
            final HazelcastInstance oldClient = this.client;
            LOG.info("Re-connecting hazelcast client for load balancing across nodes");
            this.client = newHazelcastClient();
            Thread thr = new Thread(new Runnable() {
                @Override
                public void run() {
                    //wait a bit to ensure that current operations had a chance to complete before shutting down old client
                    try{
                        Thread.sleep(300000);
                    }catch(InterruptedException e){
                        //do nothing
                        Thread.currentThread().interrupt();
                    }
                    oldClient.shutdown();

                }
            }, "HazelcastClient-shutdown-old-client-thread");
            
            thr.setDaemon(true);
            thr.start();
            
            isTimeToReConnect = false;
        }
        
        if(this.client==null || !this.client.getLifecycleService().isRunning()){
            //whole cluster went down, client has been terminated, we'll attempt to re-create the client to connect again            
            HazelcastInstance oldClient = this.client;
            if(oldClient!=null){
                oldClient.getLifecycleService().terminate();
            }

            for(int i=0; i<Integer.getInteger("com.telecominfraproject.wlan.hazelcast.client.maxReconnectAttempts", 3000); i++){
                try {
                    Thread.sleep(Integer.getInteger("com.telecominfraproject.wlan.hazelcast.client.sleepBeforeReconnectMs", 1000));
                } catch (InterruptedException e) {
                    //do nothing
                    Thread.currentThread().interrupt();
                }
    
                LOG.warn("Re-connecting hazelcast client because the old one got disconnected");
                try{
                    this.client = newHazelcastClient();
                    break;
                }catch(Exception e){
                    LOG.error("Client could not connect");
                }
            }
            
        }
        return this.client;
    }

    public String getName() {
        return getClient().getName();
    }

    public <E> IQueue<E> getQueue(String name) {
        return getClient().getQueue(name);
    }

    public <E> ITopic<E> getTopic(String name) {
        return getClient().getTopic(name);
    }

    public <E> ISet<E> getSet(String name) {
        return getClient().getSet(name);
    }

    public <E> IList<E> getList(String name) {
        return getClient().getList(name);
    }

    public <K, V> IMap<K, V> getMap(String name) {
        return getClient().getMap(name);
    }

    public <K, V> ReplicatedMap<K, V> getReplicatedMap(String name) {
        return getClient().getReplicatedMap(name);
    }

    public <K, V> MultiMap<K, V> getMultiMap(String name) {
        return getClient().getMultiMap(name);
    }

    public <E> Ringbuffer<E> getRingbuffer(String name) {
        return getClient().getRingbuffer(name);
    }

    public <E> ITopic<E> getReliableTopic(String name) {
        return getClient().getReliableTopic(name);
    }

    public Cluster getCluster() {
        return getClient().getCluster();
    }

    public Endpoint getLocalEndpoint() {
        return getClient().getLocalEndpoint();
    }

    public IExecutorService getExecutorService(String name) {
        return getClient().getExecutorService(name);
    }

    public DurableExecutorService getDurableExecutorService(String name) {
        return getClient().getDurableExecutorService(name);
    }

    public <T> T executeTransaction(TransactionalTask<T> task) throws TransactionException {
        return getClient().executeTransaction(task);
    }

    public <T> T executeTransaction(TransactionOptions options, TransactionalTask<T> task) throws TransactionException {
        return getClient().executeTransaction(options, task);
    }

    public TransactionContext newTransactionContext() {
        return getClient().newTransactionContext();
    }

    public TransactionContext newTransactionContext(TransactionOptions options) {
        return getClient().newTransactionContext(options);
    }

    public FlakeIdGenerator getFlakeIdGenerator(String name) {
        return getClient().getFlakeIdGenerator(name);
    }

    public Collection<DistributedObject> getDistributedObjects() {
        return getClient().getDistributedObjects();
    }

    public UUID addDistributedObjectListener(DistributedObjectListener distributedObjectListener) {
        return getClient().addDistributedObjectListener(distributedObjectListener);
    }

    public boolean removeDistributedObjectListener(UUID registrationId) {
        return getClient().removeDistributedObjectListener(registrationId);
    }

    public Config getConfig() {
        return getClient().getConfig();
    }

    public PartitionService getPartitionService() {
        return getClient().getPartitionService();
    }

    public SplitBrainProtectionService getSplitBrainProtectionService() {
        return getClient().getSplitBrainProtectionService();
    }

    public ClientService getClientService() {
        return getClient().getClientService();
    }

    public LoggingService getLoggingService() {
        return getClient().getLoggingService();
    }

    public LifecycleService getLifecycleService() {
        return getClient().getLifecycleService();
    }

    public <T extends DistributedObject> T getDistributedObject(String serviceName, String name) {
        return getClient().getDistributedObject(serviceName, name);
    }

    public ConcurrentMap<String, Object> getUserContext() {
        return getClient().getUserContext();
    }

    public HazelcastXAResource getXAResource() {
        return getClient().getXAResource();
    }

    public ICacheManager getCacheManager() {
        return getClient().getCacheManager();
    }

    public CardinalityEstimator getCardinalityEstimator(String name) {
        return getClient().getCardinalityEstimator(name);
    }

    public PNCounter getPNCounter(String name) {
        return getClient().getPNCounter(name);
    }

    public IScheduledExecutorService getScheduledExecutorService(String name) {
        return getClient().getScheduledExecutorService(name);
    }

    public CPSubsystem getCPSubsystem() {
        return getClient().getCPSubsystem();
    }

    public SqlService getSql() {
        return getClient().getSql();
    }

    public void shutdown() {
        getClient().shutdown();
    }
    
}
