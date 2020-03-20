package com.telecominfraproject.wlan.hazelcast.client;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.cardinality.CardinalityEstimator;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.ClientService;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.DistributedObjectListener;
import com.hazelcast.core.Endpoint;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.ICacheManager;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISemaphore;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.core.MultiMap;
import com.hazelcast.core.PartitionService;
import com.hazelcast.core.ReplicatedMap;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.crdt.pncounter.PNCounter;
import com.hazelcast.durableexecutor.DurableExecutorService;
import com.hazelcast.flakeidgen.FlakeIdGenerator;
import com.hazelcast.logging.LoggingService;
import com.hazelcast.mapreduce.JobTracker;
import com.hazelcast.quorum.QuorumService;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.hazelcast.scheduledexecutor.IScheduledExecutorService;
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
    
    @Override
    public String getName() {
        return getClient().getName();
    }

    @Override
    public <E> IQueue<E> getQueue(String name) {
        return getClient().getQueue(name);
    }

    @Override
    public <E> ITopic<E> getTopic(String name) {
        return getClient().getTopic(name);
    }

    @Override
    public <E> ISet<E> getSet(String name) {
        return getClient().getSet(name);
    }

    @Override
    public <E> IList<E> getList(String name) {
        return getClient().getList(name);
    }

    @Override
    public <K, V> IMap<K, V> getMap(String name) {
        return getClient().getMap(name);
    }

    @Override
    public <K, V> ReplicatedMap<K, V> getReplicatedMap(String name) {
        return getClient().getReplicatedMap(name);
    }

    @Override
    public JobTracker getJobTracker(String name) {
        return getClient().getJobTracker(name);
    }

    @Override
    public <K, V> MultiMap<K, V> getMultiMap(String name) {
        return getClient().getMultiMap(name);
    }

    @Override
    public ILock getLock(String key) {
        return getClient().getLock(key);
    }

    @Override
    public <E> Ringbuffer<E> getRingbuffer(String name) {
        return getClient().getRingbuffer(name);
    }

    @Override
    public <E> ITopic<E> getReliableTopic(String name) {
        return getClient().getReliableTopic(name);
    }

    @Override
    public Cluster getCluster() {
        return getClient().getCluster();
    }

    @Override
    public Endpoint getLocalEndpoint() {
        return getClient().getLocalEndpoint();
    }

    @Override
    public IExecutorService getExecutorService(String name) {
        return getClient().getExecutorService(name);
    }

    @Override
    public <T> T executeTransaction(TransactionalTask<T> task) throws TransactionException {
        return getClient().executeTransaction(task);
    }

    @Override
    public <T> T executeTransaction(TransactionOptions options, TransactionalTask<T> task) throws TransactionException {
        return getClient().executeTransaction(options, task);
    }

    @Override
    public TransactionContext newTransactionContext() {
        return getClient().newTransactionContext();
    }

    @Override
    public TransactionContext newTransactionContext(TransactionOptions options) {
        return getClient().newTransactionContext(options);
    }

    @Override
    public IdGenerator getIdGenerator(String name) {
        return getClient().getIdGenerator(name);
    }

    @Override
    public IAtomicLong getAtomicLong(String name) {
        return getClient().getAtomicLong(name);
    }

    @Override
    public <E> IAtomicReference<E> getAtomicReference(String name) {
        return getClient().getAtomicReference(name);
    }

    @Override
    public ICountDownLatch getCountDownLatch(String name) {
        return getClient().getCountDownLatch(name);
    }

    @Override
    public ISemaphore getSemaphore(String name) {
        return getClient().getSemaphore(name);
    }

    @Override
    public Collection<DistributedObject> getDistributedObjects() {
        return getClient().getDistributedObjects();
    }

    @Override
    public String addDistributedObjectListener(DistributedObjectListener distributedObjectListener) {
        return getClient().addDistributedObjectListener(distributedObjectListener);
    }

    @Override
    public boolean removeDistributedObjectListener(String registrationId) {
        return getClient().removeDistributedObjectListener(registrationId);
    }

    @Override
    public Config getConfig() {
        return getClient().getConfig();
    }

    @Override
    public PartitionService getPartitionService() {
        return getClient().getPartitionService();
    }

    @Override
    public QuorumService getQuorumService() {
        return getClient().getQuorumService();
    }

    @Override
    public ClientService getClientService() {
        return getClient().getClientService();
    }

    @Override
    public LoggingService getLoggingService() {
        return getClient().getLoggingService();
    }

    @Override
    public LifecycleService getLifecycleService() {
        return getClient().getLifecycleService();
    }

    @Override
    public <T extends DistributedObject> T getDistributedObject(String serviceName, String name) {
        return getClient().getDistributedObject(serviceName, name);
    }

    @Override
    public ConcurrentMap<String, Object> getUserContext() {
        return getClient().getUserContext();
    }

    @Override
    public HazelcastXAResource getXAResource() {
        return getClient().getXAResource();
    }

    @Override
    public void shutdown() {
        getClient().shutdown();
    }

    @Override
    public DurableExecutorService getDurableExecutorService(String name) {
        return getClient().getDurableExecutorService(name);
    }

    @Override
    public ICacheManager getCacheManager() {
        return getClient().getCacheManager();
    }

    @Override
    public CardinalityEstimator getCardinalityEstimator(String name) {
        return getClient().getCardinalityEstimator(name);
    }

    @Override
    public IScheduledExecutorService getScheduledExecutorService(String name) {
        return getClient().getScheduledExecutorService(name);
    }

    @Override
    public PNCounter getPNCounter(String name) {    	
    	return getClient().getPNCounter(name);
    }
    
    @Override
    public CPSubsystem getCPSubsystem() {
    	return getClient().getCPSubsystem();
    }
    
    @Override
    public FlakeIdGenerator getFlakeIdGenerator(String name) {
    	return getClient().getFlakeIdGenerator(name);
    }
}
