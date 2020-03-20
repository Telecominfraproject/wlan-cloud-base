package com.telecominfraproject.wlan.hazelcast.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.telecominfraproject.wlan.core.model.utils.SystemAndEnvPropertyResolver;

/**
 * @author dtop
 *
 */
@Configuration
public class HazelcastObjectsConfiguration {
    
    //
    // map settings for metrics, system events and equipment events
    //
    @Value("${tip.wlan.SystemEventDatastore.hazelcast.numBackups:1}")
    private int systemEventsNumBackups;
    @Value("${tip.wlan.SystemEventDatastore.hazelcast.ttlSeconds:600}")
    private int systemEventsTtlSeconds;
    @Value("${tip.wlan.SystemEventDatastore.hazelcast.mapPrefix:se-}")
    private String systemEventsMapPrefix;

    @Value("${tip.wlan.RawEquipmentEventDatastore.hazelcast.numBackups:1}")
    private int rawEquipmentEventsNumBackups;   
    @Value("${tip.wlan.RawEquipmentEventDatastore.hazelcast.ttlSeconds:600}")
    private int rawEquipmentEventsTtlSeconds;   
    @Value("${tip.wlan.RawEquipmentEventDatastore.hazelcast.mapPrefix:ree-}")
    private String rawEquipmentEventsMapPrefix;
    
    @Value("${tip.wlan.ServiceMetricsDatastore.hazelcast.numBackups:1}")
    private int serviceMetricsNumBackups;   
    @Value("${tip.wlan.ServiceMetricsDatastore.hazelcast.ttlSeconds:600}")
    private int serviceMetricsTtlSeconds;   
    @Value("${tip.wlan.ServiceMetricsDatastore.hazelcast.mapPrefix:sm-}")
    private String serviceMetricsMapPrefix;

    //queues for managing rule engine agent queues
    @Value("${tip.wlan.ruleAgent.hazelcast.queuePrefix:re-q-}")
    private String ruleAgentQueuePrefix;
    @Value("${tip.wlan.ruleAgent.hazelcast.queue.numBackups:1}")
    private int ruleAgentQueueNumBackups;
    @Value("${tip.wlan.ruleAgent.hazelcast.queueMaxSize:10000}")
    private int ruleAgentQueueMaxSize;

    @Value("${tip.wlan.ruleAgent.hazelcast.unassignedRuleAgentQueuesQ:unassigned-re-queues-q}")
    private String unassignedRuleAgentQueuesQName;
    @Value("${tip.wlan.ruleAgent.hazelcast.unassignedRuleAgentQueuesQ.numBackups:1}")
    private int unassignedRuleAgentQueuesQNumBackups;

    //maps for managing rule engine agent queues
    @Value("${tip.wlan.ruleAgent.hazelcast.ruleEngineProcessMap:re-process-map}")
    private String ruleAgentProcessMapName;
    @Value("${tip.wlan.ruleAgent.hazelcast.ruleEngineProcessMap.numBackups:1}")
    private int ruleAgentProcessMapNumBackups;    
    @Value("${tip.wlan.ruleAgent.hazelcast.ruleEngineProcessMap.ttlSeconds:120}")
    private int ruleAgentProcessMapTtlSeconds;

    @Value("${tip.wlan.ruleAgent.hazelcast.unassignedRuleAgentQueuesMap:unassigned-re-queues-map}")
    private String unassignedRuleAgentQueuesMapName;
    @Value("${tip.wlan.ruleAgent.hazelcast.unassignedRuleAgentQueuesMap.numBackups:1}")
    private int unassignedRuleAgentQueuesMapNumBackups;

    
    @Value("${tip.wlan.ruleAgent.hazelcast.ruleAgentQueueAssignmentsMap:rule-agent-q-assignments-map}")
    private String ruleAgentQueueAssignmentsMapName;
    @Value("${tip.wlan.ruleAgent.hazelcast.ruleAgentQueueAssignmentsMap.numBackups:1}")
    private int ruleAgentQueueAssignmentsMapNumBackups;

    @Value("${tip.wlan.ruleAgent.hazelcast.ruleEngineProcessConsumedCapacityMap:re-process-consumed-capacity-map}")
    private String ruleAgentProcessConsumedCapacityMapName;
    @Value("${tip.wlan.ruleAgent.hazelcast.ruleEngineProcessConsumedCapacityMap.numBackups:1}")
    private int ruleAgentProcessConsumedCapacityMapNumBackups;
    @Value("${tip.wlan.ruleAgent.hazelcast.ruleEngineProcessConsumedCapacityMap.ttlSeconds:120}")
    private int ruleAgentProcessConsumedCapacityMapTtlSeconds;

    @Value("${tip.wlan.ruleAgent.hazelcast.ruleEngineLevelAssignmentMap:re-level-assignment-map}")
    private String ruleEngineLevelAssignmentMapName;
    @Value("${tip.wlan.ruleAgent.hazelcast.ruleEngineLevelAssignmentMap.numBackups:1}")
    private int ruleEngineLevelAssignmentMapNumBackups;
    
    @Value("${tip.wlan.ruleAgent.hazelcast.equipmentFullPathMap:equipment-full-path-map}")
    private String equipmentFullPathMapName;
    @Value("${tip.wlan.ruleAgent.hazelcast.equipmentFullPathMap.numBackups:1}")
    private int equipmentFullPathMapNumBackups;

    @Value("${tip.wlan.ruleAgent.hazelcast.locationFullPathMap:location-full-path-map}")
    private String locationFullPathMapName;
    @Value("${tip.wlan.ruleAgent.hazelcast.locationFullPathMap.numBackups:1}")
    private int locationFullPathMapNumBackups;

    @Value("${tip.wlan.ruleAgent.hazelcast.workDistributionMonitorSemaphore:re-work-distribution-semaphore}")
    private String ruleEngineWorkDistributionSemaphoreName;
    @Value("${tip.wlan.ruleAgent.hazelcast.workDistributionMonitorSemaphore.numBackups:1}")
    private int ruleEngineWorkDistributionSemaphoreNumBackups;

    @Value("${tip.wlan.ruleAgent.hazelcast.woorkDistributionLastmodTimestamp:re-work-distribution-lastmod}")
    private String ruleEngineWorkDistributionLastmodTimestampName;
    
    @Value("${tip.wlan.ruleAgent.hazelcast.woorkDistributionLastmodTimestamp:re-unassigned-queue-monitor-lastmod}")
    private String ruleEngineUnasignedQueueLastmodTimestampName;
    
    @Value("${tip.wlan.ruleAgent.hazelcast.ruleAgentInitialReservedCapacityMap:agent-queue-initial-reserved-capacity-map}")
    private String ruleAgentInitialReservedCapacityMapName;
    

    
    /**
     * These maps hold creationTimestamp for files in HDS
     */
    @Value("${tip.wlan.hazelcast.hdsCreationTimestampFileMapPrefix:hdsCreationTs-}")
    private String hdsCreationTimestampFileMapPrefix;
    
    @Value("${tip.wlan.hazelcast.systemEventsHdsCreationTimestamps.numBackups:1}")
    private int systemEventsHdsCreationTimestampsNumBackups;    
    @Value("${tip.wlan.hazelcast.rawEquipmentEventsHdsCreationTimestamps.numBackups:1}")
    private int rawEquipmentEventsHdsCreationTimestampsNumBackups;    
    @Value("${tip.wlan.hazelcast.serviceMetricsHdsCreationTimestamps.numBackups:1}")
    private int serviceMetricsHdsCreationTimestampsNumBackups;
    
    @Value("${tip.wlan.hazelcast.systemEventsHdsCreationTimestamps.ttlSeconds:3600}")
    private int systemEventsHdsCreationTimestampsTtlSeconds;
    @Value("${tip.wlan.hazelcast.rawEquipmentEventsHdsCreationTimestamps.ttlSeconds:3600}")
    private int rawEquipmentEventsHdsCreationTimestampsTtlSeconds;
    @Value("${tip.wlan.hazelcast.serviceMetricsHdsCreationTimestamps.ttlSeconds:3600}")
    private int serviceMetricsHdsCreationTimestampsTtlSeconds;
    
    /**
     * This map holds directory listings for files in HDS
     */
    @Value("${tip.wlan.hazelcast.hdsDirectoryListingsMapName:hdsDirList-map}")
    private String hdsDirectoryListingsMapName;
    
    @Value("${tip.wlan.hazelcast.hdsDirectoryListings.numBackups:1}")
    private int hdsDirectoryListingsNumBackups;    
    
    @Value("${tip.wlan.hazelcast.hdsDirectoryListings.ttlSeconds:9000}")
    private int hdsDirectoryListingsTtlSeconds;
    
    /**
     * This map holds customer-equipmentId pairs taken from directory listings in HDS
     */
    @Value("${tip.wlan.hazelcast.hdsDirectoryCustomerEquipmentMapName:hdsDirCustEq-map}")
    private String hdsDirectoryCustomerEquipmentMapName;
    
    @Value("${tip.wlan.hazelcast.hdsDirectoryCustomerEquipment.numBackups:1}")
    private int hdsDirectoryCustomerEquipmentNumBackups;    
    
    @Value("${tip.wlan.hazelcast.hdsDirectoryCustomerEquipment.ttlSeconds:9000}")
    private int hdsDirectoryCustomerEquipmentTtlSeconds;

    /**
     * These queues holds command listings for CNAs
     */
    @Value("${tip.wlan.hazelcast.commands.queuePrefix:commands-q-}")
    private String commandListingsQueuePrefix;

    @Value("${tip.wlan.hazelcast.commands.numBackups:1}")
    private int commandListingsQueueNumBackups;

    @Value("${tip.wlan.hazelcast.commands.maxSize:5000}")
    private int commandListingsQueueMaxSize;

    /**
     * These maps hold record indexes with TTL = 2 hrs
     */
    @Value("${tip.wlan.hazelcast.recordIndexMapPrefix:recIdx-}")
    private String recordIndexMapPrefix;

    @Value("${tip.wlan.hazelcast.systemEventsRecordIndex.numBackups:1}")
    private int systemEventsRecordIndexNumBackups;
    @Value("${tip.wlan.hazelcast.rawEquipmentEventsRecordIndex.numBackups:1}")
    private int rawEquipmentEventsRecordIndexNumBackups;
    @Value("${tip.wlan.hazelcast.serviceMetricsRecordIndex.numBackups:1}")
    private int serviceMetricsRecordIndexNumBackups;
    
    @Value("${tip.wlan.hazelcast.systemEventsRecordIndex.ttlSeconds:7200}")
    private int systemEventsRecordIndexTtlSeconds;
    @Value("${tip.wlan.hazelcast.rawEquipmentEventsRecordIndex.ttlSeconds:7200}")
    private int rawEquipmentEventsRecordIndexTtlSeconds;
    @Value("${tip.wlan.hazelcast.serviceMetricsRecordIndex.ttlSeconds:7200}")
    private int serviceMetricsRecordIndexTtlSeconds;
    
    
    /**
     * This map holds names of hourly directories for which re-build of hourly index was requested
     * with TTL = 2 hrs
     */
    @Value("${tip.wlan.hazelcast.buildInProgressHourlyDirectoryNamesMapPrefix:bipHrDirs-}")
    private String buildInProgressHourlyDirectoryNamesMapPrefix;
    @Value("${tip.wlan.hazelcast.buildInProgressHourlyDirectoryNames.numBackups:1}")
    private int buildInProgressHourlyDirectoryNamesNumBackups;
    
    @Value("${tip.wlan.hazelcast.buildInProgressHourlyDirectoryNames.ttlSeconds:7200}")
    private int buildInProgressHourlyDirectoryNamesTtlSeconds;

    /**
     * This queue holds names of hourly directories for which re-build of hourly index was requested
     * K2HdsConnector reads from this queue and perform rebuilds of hourly indexes
     */
    @Value("${tip.wlan.hazelcast.rebuildIdxHourlyDirectoryNamesQueue:rebuildHrIdxQueue-}")
    private String rebuildIdxHourlyDirectoryNamesQueue;

    @Value("${tip.wlan.hazelcast.rebuildIdxHourlyDirectoryNamesQueue.numBackups:1}")
    private int rebuildIdxHourlyDirectoryNamesQueueNumBackups;   

    /**
     * Iterate through all instance variables, and for all that are String or int and that are null or 0 - 
     * find @Value annotation and use it with SystemAndEnvPropertyResolver.
     * 
     * @return HazelcastObjectsConfiguration constructed from System Properties or Environment Variables
     */
    public static HazelcastObjectsConfiguration createOutsideOfSpringApp(){
        HazelcastObjectsConfiguration ret = new HazelcastObjectsConfiguration();
        return SystemAndEnvPropertyResolver.initOutsideOfSpringApp(ret);
    }
    
    public static void main(String[] args) {
        createOutsideOfSpringApp();
    }
    
    public int getSystemEventsNumBackups() {
        return systemEventsNumBackups;
    }
    
    public int getSystemEventsTtlSeconds() {
        return systemEventsTtlSeconds;
    }

    /**
     * These maps hold last 10 minutes of system events - to be able to include in queries data that has not yet been written to HDS
     */
    public String getSystemEventsMapPrefix() {
        return systemEventsMapPrefix;
    }

    public int getRawEquipmentEventsNumBackups() {
        return rawEquipmentEventsNumBackups;
    }
    
    public int getRawEquipmentEventsTtlSeconds() {
        return rawEquipmentEventsTtlSeconds;
    }

    /**
     * These maps hold last 10 minutes of equipment events - to be able to include in queries data that has not yet been written to HDS
     */
    public String getRawEquipmentEventsMapPrefix() {
        return rawEquipmentEventsMapPrefix;
    }

    public int getServiceMetricsNumBackups() {
        return serviceMetricsNumBackups;
    }
    
    public int getServiceMetricsTtlSeconds(){
        return serviceMetricsTtlSeconds;
    }

    /**
     * These maps hold last 10 minutes of service metrics - to be able to include in queries data that has not yet been written to HDS
     */
    public String getServiceMetricsMapPrefix() {
        return serviceMetricsMapPrefix;
    }

    /**
     * These maps hold creationTimestamp for files in HDS
     */
    public String getHdsCreationTimestampFileMapPrefix() {
        return hdsCreationTimestampFileMapPrefix;
    }
    
    public int getSystemEventsHdsCreationTimestampsNumBackups(){
        return systemEventsHdsCreationTimestampsNumBackups;
    }
    
    public int getRawEquipmentEventsHdsCreationTimestampsNumBackups(){
        return rawEquipmentEventsHdsCreationTimestampsNumBackups;
    }

    public int getServiceMetricsHdsCreationTimestampsNumBackups(){
        return serviceMetricsHdsCreationTimestampsNumBackups;
    }
    
    public int getSystemEventsHdsCreationTimestampsTtlSeconds(){
        return systemEventsHdsCreationTimestampsTtlSeconds;
    }
    
    public int getRawEquipmentEventsHdsCreationTimestampsTtlSeconds(){
        return rawEquipmentEventsHdsCreationTimestampsTtlSeconds;
    }
    
    public int getServiceMetricsHdsCreationTimestampsTtlSeconds(){
        return serviceMetricsHdsCreationTimestampsTtlSeconds;
    }
    
    /**
     * These maps hold record indexes with TTL = 2 hrs
     */
    public String getRecordIndexMapPrefix() {
        return recordIndexMapPrefix;
    }
    
    public int getSystemEventsRecordIndexNumBackups(){
        return systemEventsRecordIndexNumBackups;
    }
    
    public int getRawEquipmentEventsRecordIndexNumBackups(){
        return rawEquipmentEventsRecordIndexNumBackups;
    }
    
    public int getServiceMetricsRecordIndexNumBackups(){
        return serviceMetricsRecordIndexNumBackups;
    }

    public int getSystemEventsRecordIndexTtlSeconds(){
        return systemEventsRecordIndexTtlSeconds;
    }
    
    public int getRawEquipmentEventsRecordIndexTtlSeconds(){
        return rawEquipmentEventsRecordIndexTtlSeconds;
    }
    
    public int getServiceMetricsRecordIndexTtlSeconds(){
        return serviceMetricsRecordIndexTtlSeconds;
    }
    
    /**
     * This map holds names of hourly directories for which re-build of hourly index was requested
     * with TTL = 2 hrs
     */
    public String getBuildInProgressHourlyDirectoryNamesMapPrefix() {
        return buildInProgressHourlyDirectoryNamesMapPrefix;
    }
    
    public int getBuildInProgressHourlyDirectoryNamesNumBackups(){
        return buildInProgressHourlyDirectoryNamesNumBackups;
    }
    
    public int getBuildInProgressHourlyDirectoryNamesTtlSeconds(){
        return buildInProgressHourlyDirectoryNamesTtlSeconds;
    }

    /**
     * This queue holds names of hourly directories for which re-build of hourly index was requested
     * K2HdsConnector reads from this queue and perform rebuilds of hourly indexes
     */
    public String getRebuildIdxHourlyDirectoryNamesQueue() {
        return rebuildIdxHourlyDirectoryNamesQueue;
    }
    
    public int getRebuildIdxHourlyDirectoryNamesQueueNumBackups(){
        return rebuildIdxHourlyDirectoryNamesQueueNumBackups;
    }

    /**
     * Prefix for all rule agent queues. Those queues are of limited capacity.
     * If a caller cannot place a new message on the queue, the queue has to be
     * cleared and system event created.
     */
    public String getRuleAgentQueuePrefix() {
        return ruleAgentQueuePrefix;
    }

    /**
     * Name of the queue that all rule engine processes will read to get requests for new rule agent queue assignments.
     * It works together with unassignedRuleAgentQueuesMapName to reduce number of duplicate requests. Entries never expire.
     */
    public String getUnassignedRuleAgentQueuesQName() {
        return unassignedRuleAgentQueuesQName;
    }
    
    public int getUnassignedRuleAgentQueuesQNumBackups(){
        return unassignedRuleAgentQueuesQNumBackups;
    }

    /**
     * Name of the map that keeps requests for new rule agent queue assignments.
     * It works together with unassignedRuleAgentQueuesQName to reduce number of duplicate requests.
     * Rule engine processes will remove an entry from this map when they successfully spawn a rule agent for a corresponding queue.
     */
    public String getUnassignedRuleAgentQueuesMapName() {
        return unassignedRuleAgentQueuesMapName;
    }

    public int getUnassignedRuleAgentQueuesMapNumBackups(){
        return unassignedRuleAgentQueuesMapNumBackups;
    }
    
    /**
     * Name of the map that rule engine processes register with. Entries in this
     * map auto-expire after 2 minutes - if not re-registered by rule engine
     * process.
     */
    public String getRuleAgentProcessMapName() {
        return ruleAgentProcessMapName;
    }
    
    public int getRuleAgentProcessMapNumBackups(){
        return ruleAgentProcessMapNumBackups;
    }
    
    public int getRuleAgentProcessMapTtlSeconds(){
        return ruleAgentProcessMapTtlSeconds;
    }

    /**
     * Name of the map that keeps track of what rule engine process is responsible for each rule agent queue. Entries never expire.
     */
    public String getRuleAgentQueueAssignmentsMapName() {
        return ruleAgentQueueAssignmentsMapName;
    }

    public int getRuleAgentQueueAssignmentsMapNumBackups(){
        return ruleAgentQueueAssignmentsMapNumBackups;
    }
    
    /**
     * Name of the map where rule engine processes publish their consumed capacity in events per second. Entries never expire.
     */
    public String getRuleAgentProcessConsumedCapacityMapName() {
        return ruleAgentProcessConsumedCapacityMapName;
    }
    
    public int getRuleAgentProcessConsumedCapacityMapNumBackups(){
        return ruleAgentProcessConsumedCapacityMapNumBackups;
    }
    
    public int getRuleAgentProcessConsumedCapacityMapTtlSeconds(){
        return ruleAgentProcessConsumedCapacityMapTtlSeconds;
    }

    /**
     * Name of the map that stores for every customerId what level the rule
     * agents are created: customer-city-building-floor. Entries are persisted
     * in RDBMS.
     */
    public String getRuleEngineLevelAssignmentMapName() {
        return ruleEngineLevelAssignmentMapName;
    }
    
    public int getRuleEngineLevelAssignmentMapNumBackups(){
        return ruleEngineLevelAssignmentMapNumBackups;
    }

    /**
     * Name of the map that stores for every equipmentId its full path in
     * location hierarchy: i.e. eq_15 -> Cu_10_C_12_B_13_F_14. Entries are
     * persisted in RDBMS.
     */
    public String getEquipmentFullPathMapName() {
        return equipmentFullPathMapName;
    }
    
    public int getEquipmentFullPathMapNumBackups(){
        return equipmentFullPathMapNumBackups;
    }

    /**
     * Name of the map that stores for every locationId its full path in
     * location hierarchy: i.e. F_14 -> Cu_10_C_12_B_13. Entries are
     * persisted in RDBMS.
     */
    public String getLocationFullPathMapName() {
        return locationFullPathMapName;
    }
    
    public int getLocationFullPathMapNumBackups(){
        return locationFullPathMapNumBackups;
    }

    /**
     * Name of the semaphore that coordinates work between RE work distribution monitors
     */
    public String getRuleEngineWorkDistributionSemaphoreName() {
        return ruleEngineWorkDistributionSemaphoreName;
    }
    
    public int getRuleEngineWorkDistributionSemaphoreNumBackups(){
        return ruleEngineWorkDistributionSemaphoreNumBackups;
    }

    /**
     * Name of the atomic long that stores timestamp of the last scan completed by the RE work distribution monitor
     */
    public String getRuleEngineWorkDistributionLastmodTimestampName() {
        return ruleEngineWorkDistributionLastmodTimestampName;
    }
    

    public String getRuleEngineUnasignedQueueLastmodTimestampName() {
        return ruleEngineUnasignedQueueLastmodTimestampName;
    }

    /**
     * Max number of events a rule agent queue can hold until it starts blocking
     */
    public int getRuleAgentQueueMaxSize() {
        return ruleAgentQueueMaxSize;
    }

    public int getRuleAgentQueueNumBackups() {
        return ruleAgentQueueNumBackups;
    }
    
    public String getRuleAgentInitialReservedCapacityMapName() {
        return ruleAgentInitialReservedCapacityMapName;
    }
    
    /**
     * This map holds directory listings for files in HDS
     */
    public String getHdsDirectoryListingsMapName() {
        return hdsDirectoryListingsMapName;
    }

    public int getHdsDirectoryListingsNumBackups() {
        return hdsDirectoryListingsNumBackups;
    }

    public int getHdsDirectoryListingsTtlSeconds() {
        return hdsDirectoryListingsTtlSeconds;
    }

    /**
     * This map holds customer-equipment mappings taken from directory listings in HDS
     */
    public String getHdsDirectoryCustomerEquipmentMapName() {
        return hdsDirectoryCustomerEquipmentMapName;
    }

    public int getHdsDirectoryCustomerEquipmentNumBackups() {
        return hdsDirectoryCustomerEquipmentNumBackups;
    }

    public int getHdsDirectoryCustomerEquipmentTtlSeconds() {
        return hdsDirectoryCustomerEquipmentTtlSeconds;
    }

    /**
     * This is the prefix for queues that holds commands to be executed by a CNA
     */
    public String getCommandListingsQueuePrefix() {
        return commandListingsQueuePrefix;
    }

    public int getCommandListingsQueueMaxSize() { return commandListingsQueueMaxSize; }

    public int getCommandListingsQueueNumBackups() { return commandListingsQueueNumBackups; }
}
