package com.telecominfraproject.wlan.hierarchical.datastore;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Timer;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsUtils;
import com.telecominfraproject.wlan.core.model.converters.BaseModelConverter;
import com.telecominfraproject.wlan.core.model.filter.EntryFilter;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.hazelcast.common.AppendStringToSetEntryProcessor;
import com.telecominfraproject.wlan.hazelcast.common.HazelcastObjectsConfiguration;
import com.telecominfraproject.wlan.hierarchical.datastore.index.DirectoryIndex;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndex;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexCounts;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexPositions;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexValueExtractor;
import com.telecominfraproject.wlan.hierarchical.datastore.index.aggregator.HourlyIndexAggregatorHazelcastScalable;
import com.telecominfraproject.wlan.hierarchical.datastore.index.registry.RecordIndexRegistry;
import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

/**
 * @author dtop
 *
 */
public class HierarchicalDatastore{

    private static final Logger LOG = LoggerFactory.getLogger(HierarchicalDatastore.class);

    private static final int hdsRequestExecutorThreads = Integer.getInteger("tip.wlan.hdsExecutorThreads", 300);
    private static final int hdsRequestExecutorCoreThreadsFactor = Integer.getInteger("tip.wlan.hdsExecutorCoreThreadsFactor", 10);
    private static final int hdsRequestExecutorQueueSize = Integer.getInteger("tip.wlan.hdsExecutorQueueSize", 5000);

    private final int maxRetries = 100;
    private final long sleepBetweenRetriesMs = 100;
    private final long idleTimeoutBeforeFlushingMs;

    private final int numberOfMinutesPerFile;

    public static final int hdsDirMapLeaseTimeForLockMs = 5000;
    public static final int hdsDirMapTimeToWaitForLockMs = 5 * hdsDirMapLeaseTimeForLockMs;


    private HazelcastInstance hazelcastClient;
    private final String dsRootDirName;
    private final String dsPrefix;
    private final String fileNamePrefix;
    private final String servoMetricPrefix;
    private final String hazelcastMapPrefix;
    private final String hdsCreationTimestampFileMapPrefix;
    private final AtomicInteger thrCounter = new AtomicInteger();

    private final HazelcastObjectsConfiguration hazelcastObjectsConfiguration;

    private final RecordIndexRegistry recordIndexRegistry;
    private final Map<String, RecordIndexValueExtractor> recordIndexes;

    private final ThreadPoolExecutor hdsGetFileNamesExecutor = new ThreadPoolExecutor(
            Math.round((float)hdsRequestExecutorThreads/hdsRequestExecutorCoreThreadsFactor), 
            hdsRequestExecutorThreads,
            10000L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(hdsRequestExecutorQueueSize),
            new ThreadFactory(){
                @Override
                public Thread newThread(Runnable r) {
                    Thread thr = new Thread(r, dsRootDirName + "-" + dsPrefix +"-" + fileNamePrefix + "-hdsGetFileNamesExecutor-"+System.currentTimeMillis());
                    thr.setDaemon(true);
                    return thr;
                }
            });

    private final ThreadPoolExecutor hdsProcessFilesExecutor = new ThreadPoolExecutor(
            Math.round((float)hdsRequestExecutorThreads/hdsRequestExecutorCoreThreadsFactor), 
            hdsRequestExecutorThreads,
            10000L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(hdsRequestExecutorQueueSize),
            new ThreadFactory(){
                @Override
                public Thread newThread(Runnable r) {
                    Thread thr = new Thread(r, dsRootDirName + "-" + dsPrefix +"-" + fileNamePrefix + "-hdsProcessFilesExecutor-"+System.currentTimeMillis()+"-"+thrCounter.incrementAndGet());
                    thr.setDaemon(true);
                    return thr;
                }
            });

    private final ThreadPoolExecutor hdsGetEntriesExecutor = new ThreadPoolExecutor(
            Math.round((float)hdsRequestExecutorThreads/hdsRequestExecutorCoreThreadsFactor), 
            hdsRequestExecutorThreads,
            10000L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(hdsRequestExecutorQueueSize),
            new ThreadFactory(){
                @Override
                public Thread newThread(Runnable r) {
                    Thread thr = new Thread(r, dsRootDirName + "-" + dsPrefix +"-" + fileNamePrefix + "-hdsGetEntriesExecutor-"+System.currentTimeMillis());
                    thr.setDaemon(true);
                    return thr;
                }
            });

    private final ThreadPoolExecutor hdsGetContentExecutor = new ThreadPoolExecutor(
            Math.round((float)hdsRequestExecutorThreads/hdsRequestExecutorCoreThreadsFactor), 
            hdsRequestExecutorThreads,
            10000L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(hdsRequestExecutorQueueSize),
            new ThreadFactory(){
                @Override
                public Thread newThread(Runnable r) {
                    Thread thr = new Thread(r, dsRootDirName + "-" + dsPrefix +"-" + fileNamePrefix + "-hdsGetContentExecutor-"+System.currentTimeMillis()+"-"+thrCounter.incrementAndGet());
                    thr.setDaemon(true);
                    return thr;
                }
            });

    public HierarchicalDatastore(String dsRootDirName, String dsPrefix, String fileNamePrefix, int numberOfMinutesPerFile, long idleTimeoutBeforeFlushingMs,
            HazelcastInstance hazelcastClient, String hazelcastMapPrefix, HazelcastObjectsConfiguration hazelcastObjectsConfiguration, RecordIndexRegistry recordIndexRegistry){
        this.dsRootDirName = dsRootDirName;
        this.dsPrefix = dsPrefix;
        this.fileNamePrefix = fileNamePrefix;
        this.numberOfMinutesPerFile = numberOfMinutesPerFile;
        this.idleTimeoutBeforeFlushingMs = idleTimeoutBeforeFlushingMs;
        this.servoMetricPrefix = "hds-"+dsRootDirName+"-"+dsPrefix+"-"+fileNamePrefix+"-";
        this.hazelcastClient = hazelcastClient;
        this.hazelcastMapPrefix = hazelcastMapPrefix;
        this.hazelcastObjectsConfiguration = hazelcastObjectsConfiguration;
        this.hdsCreationTimestampFileMapPrefix = hazelcastObjectsConfiguration==null?null:hazelcastObjectsConfiguration.getHdsCreationTimestampFileMapPrefix();
        this.recordIndexRegistry = recordIndexRegistry;
        this.recordIndexes = recordIndexRegistry.getIndexMap(fileNamePrefix);

        this.hdsGetFileNamesExecutor.prestartAllCoreThreads();
        this.hdsGetContentExecutor.prestartAllCoreThreads();
        this.hdsGetEntriesExecutor.prestartAllCoreThreads();
        this.hdsProcessFilesExecutor.prestartAllCoreThreads();
        
        File rootDir = new File(dsRootDirName + File.separator + dsPrefix);
        if(!rootDir.exists()) {
        	LOG.info("Creating datastore root dir {}", rootDir.getAbsoluteFile());
        	rootDir.mkdirs();
        }

        CloudMetricsUtils.registerGauge(servoMetricPrefix+"getFileNames-tasksInTheQueue", 
                new Callable<Long>(){
            @Override
            public Long call() throws Exception {
                return (long) hdsGetFileNamesExecutor.getQueue().size();
            }
        });
        CloudMetricsUtils.registerGauge(servoMetricPrefix+"getContent-tasksInTheQueue", 
                new Callable<Long>(){
            @Override
            public Long call() throws Exception {
                return (long) hdsGetContentExecutor.getQueue().size();
            }
        });
        CloudMetricsUtils.registerGauge(servoMetricPrefix+"getEntries-tasksInTheQueue", 
                new Callable<Long>(){
            @Override
            public Long call() throws Exception {
                return (long) hdsGetEntriesExecutor.getQueue().size();
            }
        });
        CloudMetricsUtils.registerGauge(servoMetricPrefix+"processFiles-tasksInTheQueue", 
                new Callable<Long>(){
            @Override
            public Long call() throws Exception {
                return (long) hdsProcessFilesExecutor.getQueue().size();
            }
        });


        LOG.info("Initialized HierarchicalDatastore {} with executorThreads = {} QueueSize = {} idleBeforeFlushMs = {}",
                dsRootDirName + "-" + dsPrefix +"-" + fileNamePrefix,
                hdsRequestExecutorThreads, hdsRequestExecutorQueueSize, idleTimeoutBeforeFlushingMs
                );

    }


    /**
     * Used only for unit tests
     * @param indexName
     * @param valueExtractor
     */
    void registerRecordIndex(String indexName, RecordIndexValueExtractor valueExtractor){
        recordIndexes.putIfAbsent(indexName, valueExtractor);
    }

    public Map<String, RecordIndexValueExtractor> getRecordIndexes(){
        return recordIndexes;
    }


    public int getNumberOfMinutesPerFile() {
        return numberOfMinutesPerFile;
    }

    
    public List<String> getFileNames(int customerId, long equipmentId, long fromTime, long toTime){
        return getFileNames_ListRequestPerHour(customerId, equipmentId, fromTime, toTime);
        //return getFileNames_ListRequestPerFileName(customerId, equipmentId, fromTime, toTime);
    }
        
    public List<String> getFileNames_ListRequestPerHour(int customerId, long equipmentId, long fromTime, long toTime){
        
        List<String> ret = new ArrayList<>();

        LOG.trace("begin getFileNames_ListRequestPerHour for customer {} equipment {} from {} to {}", customerId, equipmentId, fromTime, toTime);

        if(fromTime>toTime){
            //invalid date range, return empty list
            LOG.debug("getFileNames_ListRequestPerHour for customer {} equipment {} from {} to {} built 0 files. invalid time range.", customerId, equipmentId, fromTime, toTime);
            return ret;
        }

        //if toTime is in the future - set it back to now
        long currentTime = System.currentTimeMillis();
        long startTimeMs = currentTime;
        if(toTime>currentTime){
            toTime = currentTime;
        }

        if((toTime - fromTime)/(3600000L) > 32*24){
            //limit pre-generated file name list to ~45000 ( 32 days with 1 minute intervals)
            fromTime = toTime - 32L*24*TimeUnit.HOURS.toMillis(1);
            LOG.warn("limiting requested time range to {} ms", toTime - fromTime);

        }

        //adjust fromTime so it a multiple of numberOfMinutesPerFile
        fromTime = fromTime - fromTime%(60000L*this.numberOfMinutesPerFile); 
        final long adjustedFromTime = fromTime;
        final long adjustedToTime = toTime;
        
        Calendar fromCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        fromCalendar.setTime(new Date(fromTime));

        Calendar toCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        toCalendar.setTime(new Date(toTime));

        List<Future<List<String>>> futures = new ArrayList<>(100);

        //generate list of files based on supplied criteria
        while(fromCalendar.before(toCalendar) || fromCalendar.equals(toCalendar)){

            //Get a list of files to retrieve data from
            int numRetries = 0;
            //get the list of files for each interval
            while(true){
                try{
                    LOG.trace("submitting getFileCreatedTimestampsForInterval task - attempt {}", numRetries);

                    final int year = fromCalendar.get(Calendar.YEAR);
                    final int month = fromCalendar.get(Calendar.MONTH) + 1;
                    final int day = fromCalendar.get(Calendar.DAY_OF_MONTH);
                    final int hour = fromCalendar.get(Calendar.HOUR_OF_DAY); 
                    final long fromCalendarMillis = fromCalendar.getTimeInMillis();

                    futures.add(hdsGetFileNamesExecutor.submit(new Callable<List<String>>(){
                        @Override
                        public List<String> call() throws Exception {
                            
                            IMap<String, Set<String>> dirListMap = null;
                            String dirKey = null;
                            Set<String> allFileNames = new HashSet<>();
                            
                            if(hazelcastClient!=null){
                                //try to find directory listing for this hour in hazelcast map dir-list<dirName,Set<fileName>>
                                dirListMap = hazelcastClient.getMap(hazelcastObjectsConfiguration.getHdsDirectoryListingsMapName());

                                //calculate directory name for the current hour based on passed in parameters
                                StringBuilder sb = new StringBuilder(1024);
                                Formatter formatter = new Formatter(sb, null);
                                formatter.format("%s/%d/%d/%4d/%02d/%02d/%02d/",
                                        dsPrefix, customerId, equipmentId, year, month, day, hour);                                
                                formatter.close();

                                dirKey = sb.toString();
                                
                                Set<String> fNames = dirListMap.get(dirKey);
                                if(fNames!=null){
                                    //we need full file names in the result, so prepend directory name to each file name
                                    for(String fName: fNames){
                                        allFileNames.add(dirKey+fName);
                                    }
                                }
                                
                            }
                            
                            if(allFileNames.isEmpty()){
                                
                                boolean lockAcquired = false;
                                try{
                                    //lock entry in dir-list map in Hazelcast, so only one list request is processed per directory at a time
                                    if((hazelcastClient!=null && dirListMap.tryLock(dirKey, 
                                            hdsDirMapTimeToWaitForLockMs, TimeUnit.MILLISECONDS, 
                                            hdsDirMapLeaseTimeForLockMs, TimeUnit.MILLISECONDS))
                                            || (hazelcastClient==null)
                                            ){
                                        lockAcquired = hazelcastClient!=null;

                                        //after acquiring lock - make sure that entry in map exists, and only if it does not exist call getFileNamesForOneHour
                                        if(hazelcastClient!=null){
                                            Set<String> fNames = dirListMap.get(dirKey);
                                            if(fNames!=null){
                                                //we need full file names in the result, so prepend directory name to each file name
                                                for(String fName: fNames){
                                                    allFileNames.add(dirKey+fName);
                                                }
                                            }
                                        }

                                        
                                        if(allFileNames.isEmpty()){
                                            //hazelcast listing not found, get full listing of the files for that hour
                                            allFileNames = getFileNamesForOneHour(customerId, equipmentId, fromCalendarMillis,"");
        
                                            if(hazelcastClient!=null){
                                                //store directory listing for this hour in hazelcast for future requests
                                                Set<String> fNames = new HashSet<>();
                                                if(allFileNames!=null){
                                                    for(String fName: allFileNames){
                                                        //we store only the file name in the set, directory is stored as a key in the map
                                                        fNames.add(fName.substring(fName.lastIndexOf('/')+1));
                                                    }
                                                    
                                                    dirListMap.put(dirKey, fNames);
                                                }
                                            }
                                        }
                                    } else {
                                        LOG.warn("Could not acquire lock for {}:{} ", hazelcastObjectsConfiguration.getHdsDirectoryListingsMapName(), dirKey);
                                        throw new GenericErrorException("Could not aqcuire lock for " + hazelcastObjectsConfiguration.getHdsDirectoryListingsMapName() + ":"+ dirKey);
                                    }
                                }catch(InterruptedException e){
                                    LOG.warn("Interrupted while waiting to lock {} map", hazelcastObjectsConfiguration.getHdsDirectoryListingsMapName());
                                    Thread.currentThread().interrupt();
                                }finally {
                                    if(lockAcquired){                                        
                                        try{
                                            dirListMap.unlock(dirKey);
                                        }catch(IllegalMonitorStateException e){
                                            LOG.warn("Distributed Lock management for {}:{} ", hazelcastObjectsConfiguration.getHdsDirectoryListingsMapName(), dirKey, e);
                                        }
                                        
                                    }
                                }

                            }
                            
                            //for this particular request include only those files that match filePrefix and fall within requested from-to range
                            List<String> fNames;
                            
                            if (allFileNames == null) {
                                fNames = Collections.emptyList();
                            } else {
                                fNames = new ArrayList<>(allFileNames.size());
                                for (String fName : allFileNames) {
                                    if (fName.substring(fName.lastIndexOf('/') + 1).startsWith(fileNamePrefix)) {
                                        long fileTime = DirectoryIndex.extractTimeFromTheDataFileName(fName);
                                        if (fileTime >= adjustedFromTime && fileTime <= adjustedToTime) {
                                            fNames.add(fName);
                                        }
                                    }
                                }
                            }

                            LOG.trace("Found {} files in directory {}", fNames.size(), dirKey);
                            
                            return fNames;
                        }

                    }));
                    break;
                }catch(RejectedExecutionException e){
                    LOG.trace("getFileNames_ListRequestPerHour for customer {} equipment {} from {} to {} - rejected execution, will retry", customerId, equipmentId, fromTime, toTime);

                    //could not submit task, will retry
                    try {
                        Thread.sleep(sleepBetweenRetriesMs);
                    } catch (InterruptedException e1) {
                        //do nothing
                        Thread.currentThread().interrupt();
                    }

                    numRetries++;

                    if(numRetries> maxRetries){
                        LOG.error("getFileNames_ListRequestPerHour for customer {} equipment {} from {} to {} could not complete after {} retries", 
                                customerId, equipmentId, fromTime, toTime, maxRetries);
                        throw new GenericErrorException("Could not get list of files after "+maxRetries+" retries");
                    }
                }
            }

            //advance time to get file names for the next hour
            fromCalendar.add(Calendar.HOUR_OF_DAY, 1);
        }

        LOG.trace("waiting for {} futures", futures.size());

        //combine file names from all the intervals
        for(Future<List<String>> future: futures){
            try {
                ret.addAll(future.get());
                LOG.trace("collected future result");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GenericErrorException(e);
            } catch (ExecutionException e) {
                LOG.error("getFileNames_ListRequestPerHour for customer {} equipment {} from {} to {} could not retrieve data", 
                        customerId, equipmentId, fromTime, toTime, e);
                throw new GenericErrorException("Failed to get Result", e);
            }
        }


        LOG.trace("getFileNames_ListRequestPerHour for customer {} equipment {} from {} to {} built {} files, took {} ms", customerId, equipmentId, fromTime, toTime, ret.size(), (System.currentTimeMillis() - startTimeMs));

        return ret;        
    }



    public void appendFileNameToDirectoryListing(int customerId, long equipmentId, int year, int month, int day, int hour, String fullFileName){

        LOG.trace("begin appendFileNameToDirectoryListing for customer {} equipment {} {}/{}/{} {}:{} {}",
                customerId, equipmentId, year, month, day, hour,
                fullFileName);

        if(hazelcastClient==null){
            LOG.warn("Hazelcast client is not configured for datastore {}-{}-{}, working in limited capacity.", this.dsRootDirName, this.dsPrefix, this.fileNamePrefix);
            return;
        }

        IMap<String, Set<String>> dirListMap = hazelcastClient.getMap(hazelcastObjectsConfiguration.getHdsDirectoryListingsMapName());

        String dirKey = fullFileName.substring(0, fullFileName.lastIndexOf('/')+1);
        String shortFileName = fullFileName.substring(fullFileName.lastIndexOf('/')+1);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long fromTime = calendar.getTimeInMillis();
        
        //force populate initial value in the map
        getFileNames(customerId, equipmentId, fromTime, fromTime + 1);

        try {
            LOG.trace("submitting append request to hazelcast");
            //submit operation and wait for its completion
            dirListMap.submitToKey(dirKey, new AppendStringToSetEntryProcessor(shortFileName) ).get();
            LOG.trace("append request is processed in hazelcast");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenericErrorException("Failed to submit append request", e);
        } catch (ExecutionException e) {
            throw new GenericErrorException("Failed to submit append request", e);
        }
    }

    /**
     * @param customerId
     * @param equipmentId
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @param createdTs
     * @return fully qualified file name, without the bucket part
     */
    public String getFileName(int customerId, long equipmentId, int year, int month, int day, int hour, int minute, long createdTs){
        StringBuilder sb = new StringBuilder(1024);
        Formatter formatter = new Formatter(sb, null);
        formatter.format("%s/%d/%d/%4d/%02d/%02d/%02d/%s_%d_%d_%4d_%02d_%02d_%02d_%02d_%d.zip",
                dsPrefix, customerId, equipmentId, year, month, day, hour,
                fileNamePrefix, customerId, equipmentId, year, month, day, hour, minute, createdTs
                );
        formatter.close();

        return sb.toString();
    }

    public String getFileName(int customerId, String relativeTargetName){
        StringBuilder sb = new StringBuilder(1024);
        Formatter formatter = new Formatter(sb, null);
        formatter.format("%s/%d/%s_%d_%s.json",
                dsPrefix, customerId,
                fileNamePrefix, customerId, relativeTargetName
                );
        formatter.close();

        return sb.toString();
    }

    public String getFileName(int customerId, long equipmentId, String relativeTargetName){
        StringBuilder sb = new StringBuilder(1024);
        Formatter formatter = new Formatter(sb, null);
        formatter.format("%s/%d/%d/%s_%d_%d_%s.json",
                dsPrefix, customerId, equipmentId,
                fileNamePrefix, customerId, equipmentId, relativeTargetName
                );
        formatter.close();

        return sb.toString();
    }


    /**
     * @param customerId
     * @param equipmentId
     * @param timestampMs - it will be normalized to numberOfMinutesPerFile, and (year, month, day, hour, minute) will be calculated from it
     * @return fully qualified file name, without the bucket part, i.e.
     *      "dsPrefix/customerId/equipmentId/year/month/day/hour/fileNamePrefix_customerId_equipmentId_year_month_day_hour_minute_createdTs.zip",
     *      where (year, month, day, hour, minute) are calculated from supplied timestampMs (normalized to numberOfMinutesPerFile), and createdTs=System.currentTimeMillis() 
     */
    public String getFileNameForNewFile(int customerId, long equipmentId, long timestampMs){
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        //adjust timestampMs so it a multiple of numberOfMinutesPerFile
        timestampMs = timestampMs - timestampMs%(1L*this.getNumberOfMinutesPerFile()*60000); 

        c.setTime(new Date(timestampMs));
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        return getFileName(customerId, equipmentId, year, month, day, hour, minute, System.currentTimeMillis());
    }

    /**
     * @param fileName - key of the object (full fileName)
     * @param entryFilter - piece of logic that converts a line of text into an object and applies filter to it.
     * @param indexName - index to use, can be null - will result in full scan of the data files
     * @param indexedValues - if index is used then indexedValues specifies what values should be matched by the index. Can be null or empty, in which case will result in full scan of the data files
     * @param recordIndex - record index to get matching positions from, can be null, in which case an attempt will be made to get record index from hazelcast, and if that fails - full scan of data files will be used 
     * @param dataClass - dataClass
     * @return List of objects that match entryFilter. 
     * Objects are parsed from lines inside zipped (JSON) file stored under specified key.
     * One object per line.
     */
    private <T> List<T> getContent(String fileName, EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues, RecordIndex recordIndex, Class<T> dataClass){

        LOG.trace("begin getContent for {}", fileName);

        String counterMetricId = servoMetricPrefix + "getContentSingleKey-count";
        String timerMetricId = servoMetricPrefix + "getContentSingleKey-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        // use record index in here to get the set of matching lines in the json datafile - positions corresponding to indexed values
        Set<Integer> matchingLines = getMatchingPositionsFromIndex(recordIndex, fileName, indexName, indexedValues);

        if(matchingLines!=null && matchingLines.isEmpty()){
            //no need to retrieve the data file - it does not have any of the indexed values
            return new ArrayList<>();
        }

        try(FileInputStream fis = new FileInputStream(dsRootDirName+ File.separator + fileName)){
            LOG.trace("found object");
            return getContent(fis, entryFilter, matchingLines, dataClass);
        } catch (FileNotFoundException e){
            LOG.trace("object {} does not exist", fileName);
            return new ArrayList<>(1);
        } catch (IOException e) {
            throw new GenericErrorException("Failed to get object content", e);
        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }
    }


    /**
     * Build name of the index file: idx_[indexName]_[data_file_name_without.zip_extention]
     * @param dataFileName
     * @param indexName
     * @return short file name (no path) of the index file based on the supplied full file name of the data file
     */
    public static String getIndexFileName(String dataFileName, String indexName){

        int namePos = dataFileName.lastIndexOf('/')+1;
        String idxfileName = "idx_" + indexName + "_" + dataFileName.substring(namePos, dataFileName.length() - 4);

        return idxfileName;
    }

    /**
     * @param recordIndex - record index to get matching positions from, can be null, in which case an attempt will be made to get record index from hazelcast 
     * @param fileName - full file name of the data file
     * @param indexName - index to use, can be null - will result in full scan of the data files
     * @param indexedValues - if index is used then indexedValues specifies what values should be matched by the index. Can be null or empty, in which case will result in full scan of the data files
     * @return set of record positions (line numbers, starting with 0) in the data file that contains objects that match provided indexedValues, or null if any error occurs. 
     *  Null value would mean that full scan of the data file is required. 
     *  Empty set would mean that no data of interest is contained in the data file.
     */
    private Set<Integer> getMatchingPositionsFromIndex(RecordIndex recordIndex, String fileName, String indexName, Set<String> indexedValues) {
        if(indexName==null || indexedValues==null || indexedValues.isEmpty()){
            // no index was provided, will deserialize and process all records
            return null;
        }

        LOG.trace("begin getMatchingPositionsFromIndex for {}", fileName);

        String counterMetricId = servoMetricPrefix + "getMatchingPositionsFromIndex-count";
        String timerMetricId = servoMetricPrefix + "getMatchingPositionsFromIndex-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        try{
            RecordIndex countsAndPositions = recordIndex!=null?recordIndex:findRecordIndex(indexName, fileName);

            if(countsAndPositions!=null){
                LOG.trace("found record index file for {}", fileName);

                // use record index to find the Set of matching lines in the json datafile - positions corresponding to indexed values
                Set<Integer> matchingLines = null;

                RecordIndexCounts recordIndexCounts = countsAndPositions.getCounts();
                RecordIndexPositions recordIndexPositions = countsAndPositions.getPositions();

                if(recordIndexCounts==null){
                    LOG.debug("Could not find RecordIndexCounts for {}", fileName);
                    return null;
                }

                //check if any of the indexedValues is having counts > 0
                //  if not, then there is no need to look further, return empty set
                boolean foundAtLeastOneValueInIndex = false;
                for(String idxVal: indexedValues){
                    if(recordIndexCounts.getCountForValue(idxVal)>0){
                        foundAtLeastOneValueInIndex = true;
                        break;
                    }
                }

                if(!foundAtLeastOneValueInIndex){
                    LOG.trace("No matching values found in index {} for {}", indexName, fileName);
                    return Collections.emptySet();
                }

                if(recordIndexPositions==null){
                    LOG.debug("Could not find RecordIndexPositions for {}", fileName);
                    return null;
                }

                matchingLines = new HashSet<>();
                //include into matchingLines record positions of those indexed values that have counts > 0
                for(String idxVal: indexedValues){
                    if(recordIndexCounts.getCountForValue(idxVal)>0){
                        matchingLines.addAll(recordIndexPositions.getPositionsForValue(idxVal));
                    }
                }

                return matchingLines;
            } else {
                LOG.trace("index file for {} does not exist", fileName);
                return null;
            }

        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }

    }

    /**
     * @param fileName - key of the object (full fileName)
     * @return content of the object as a subclass of BaseJsonModel or null if the specified key does not exist
     *  
     */
    public BaseJsonModel getContentAsModelOrNull(String fileName){

        LOG.trace("begin getContentAsModelOrNull for {}", fileName);

        String counterMetricId = servoMetricPrefix + "getContentAsModel-count";
        String timerMetricId = servoMetricPrefix + "getContentAsModel-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        try(FileInputStream fis = new FileInputStream(dsRootDirName+ File.separator + fileName)){
            LOG.trace("found object");
            return BaseJsonModel.fromString(StreamUtils.copyToString(fis, StandardCharsets.UTF_8), BaseJsonModel.class);
        } catch (FileNotFoundException e){
            LOG.trace("object {} does not exist", fileName);
            return null;
        } catch (IOException e) {
            throw new GenericErrorException("Failed to get object", e);
        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * @param fileName - key of the object (full fileName)
     * @param entryFilter - piece of logic that converts a line of text into an object and applies filter to it.
     * @param indexName - index to use, can be null - will result in full scan of the data files
     * @param indexedValues - if index is used then indexedValues specifies what values should be matched by the index. Can be null or empty, in which case will result in full scan of the data files
     * @param recordIndex - record index to get matching positions from, can be null, in which case an attempt will be made to get record index from hazelcast, and if that fails - full scan of data files will be used 
     * @param dataClass - data class
     * @return Count of objects that match entryFilter. 
     * Objects are parsed from lines inside zipped (JSON) file stored under specified key.
     * One object per line.
     */
    public <T> int countEntries(String fileName, EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues, RecordIndex recordIndex, Class<T> dataClass){

        LOG.trace("begin countEntries for {}", fileName);
        String counterMetricId = servoMetricPrefix + "countEntriesSingleKey-count";
        String timerMetricId = servoMetricPrefix + "countEntriesSingleKey-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        // use record index in here to get the set of matching lines in the json datafile - positions corresponding to indexed values
        Set<Integer> matchingLines = getMatchingPositionsFromIndex(recordIndex, fileName, indexName, indexedValues);

        if(matchingLines!=null && matchingLines.isEmpty()){
            //no need to retrieve the data file - it does not have any of the indexed values
            return 0;
        }

        try(FileInputStream fis = new FileInputStream(dsRootDirName+ File.separator + fileName)){
            LOG.trace("found object");
            return countEntries(fis, entryFilter, matchingLines, dataClass);
        } catch (FileNotFoundException e){
            LOG.trace("object {} does not exist", fileName);
            return 0;
        } catch (IOException e) {
            throw new GenericErrorException("Failed to get object", e);
        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * @param inputStream - inputStream that contains zipped (JSON) file
     * @param entryFilter - piece of logic that converts a line of text into an object and applies filter to it.
     * @param lineNumbers - line numbers in the json file from which to deserialize data before passing it to the entryFilter, all other lines are ignored. If lineNumbers is null, then all the lines are deserialized
     * @param dataClass - data class
     * @return Count of objects that match entryFilter. 
     * Objects are parsed from lines inside zipped (JSON) file supplied as an inputStream.
     * One object per line.
     */
    public <T> int countEntries(InputStream inputStream, EntryFilter<T> entryFilter, Set<Integer> lineNumbers, Class<T> dataClass){

        ZipEntry ze;
        String zipEntryName;

        int ret = 0;

        try(
                ZipInputStream zis = new ZipInputStream(inputStream);
                ){

            while ((ze=zis.getNextEntry())!=null){
                zipEntryName = ze.getName();
                LOG.trace("Processing zip entry {}", zipEntryName);
                InputStreamReader isr = new InputStreamReader(zis, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr); 

                //
                // use provided line numbers here - extracted from the index - to only look at the records of interest 
                //
                int lineNum = -1;

                for(String line; (line = br.readLine()) != null; ) {
                    lineNum++; //first line number is 0

                    if(lineNumbers!=null){
                        //line numbers were provided, will look for data only on those lines
                        if(!lineNumbers.contains(lineNum)){
                            //not interested in this line
                            continue;
                        }
                    } 

                    T entity = entryFilter.getFilteredEntry(line, dataClass);
                    if(entity!=null){
                        ret++; 
                    }
                }
            }

        } catch (IOException e) {
            throw new GenericErrorException("Failed to process zip entry", e);
        }

        LOG.trace("counted {} entries", ret);

        return ret;
    }

    /**
     * @param inputStream - inputStream that contains zipped (JSON) file
     * @param entryFilter - piece of logic that converts a line of text into an object and applies filter to it.
     * @param lineNumbers - line numbers in the json file from which to deserialize data before passing it to the entryFilter, all other lines are ignored. If lineNumbers is null, then all the lines are deserialized
     * @param dataClass - data class
     * @return List of objects that match entryFilter. 
     * Objects are parsed from lines inside zipped (JSON) file supplied as an inputStream.
     * One object per line.
     */
    public static <T> List<T> getContent(InputStream inputStream, EntryFilter<T> entryFilter, Set<Integer> lineNumbers, Class<T> dataClass){

        ZipEntry ze;
        String zipEntryName;

        List<T> ret = new ArrayList<>();
        Integer maxLineNum = lineNumbers==null?null:lineNumbers.stream().max(Comparator.naturalOrder()).orElse(null);

        try(ZipInputStream zis = new SelfDrainingZipInputStream(inputStream)){

            while ((ze=zis.getNextEntry())!=null){
                zipEntryName = ze.getName();
                LOG.trace("Processing zip entry {}", zipEntryName);
                InputStreamReader isr = new InputStreamReader(zis, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr); 

                //
                // use provided line numbers here - extracted from the index - to only look at the records of interest 
                //
                int lineNum = -1;
                for(String line; (line = br.readLine()) != null; ) {

                    lineNum++; //first line number is 0

                    if(lineNumbers!=null){
                        //line numbers were provided, will look for data only on those lines
                        if(!lineNumbers.contains(lineNum)){
                            //not interested in this line
                            continue;
                        }
                    } 
                    
                    T entity = entryFilter.getFilteredEntry(line, dataClass);
                    if(entity!=null){
                        ret.add(entity); 
                    }
                    
                    if(maxLineNum != null && maxLineNum <= lineNum) {
                        // No need to read anymore line, we've read all the ones we need.
                        break;
                    }
                }
            }
            
        } catch (IOException e) {
            throw new GenericErrorException("Failed to get zipped content", e);
        }

        LOG.trace("Read {} entries", ret.size());

        return ret;
    }

    public <T> List<T> getEntries(int customerId, long equipmentId, long fromTime, long toTime,
            final EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues, Class<T> dataClass) {
        final List<T> ret = new ArrayList<>(4000);

        DataFileOperation<T> operation = new DataFileOperation<T>() {
            @Override
            public void processFiles(EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues,
                    List<String> dataFileNames, DirectoryIndex hourlyIdx) {
                List<T> content = getContent(entryFilter, indexName, indexedValues, dataFileNames, hourlyIdx,
                        dataClass);
                synchronized (this) {
                    ret.addAll(content);
                }
            }
        };

        processDataFiles(customerId, equipmentId, fromTime, toTime, entryFilter, indexName, indexedValues, operation);

        LOG.debug("getEntries for customer {} equipment {} from {} to {} Found {} results.", customerId, equipmentId,
                fromTime, toTime, ret.size());
        return ret;
    }

    /**
     * Apply a DataFileOperation to the list of data files for customer equipment between fromTime and toTime.
     * It is up to the operation to interpret entry filter, indexName and indexedValues parameters.
     * @param customerId
     * @param equipmentId
     * @param fromTime
     * @param toTime
     * @param entryFilter - piece of logic that converts a line of text into an object and applies filter to it.
     * @param indexName - index to use, can be null - will result in full scan of the data files
     * @param indexedValues - if index is used then indexedValues specifies what values should be matched by the index. Can be null or empty, in which case will result in full scan of the data files
     * @param operation - DataFileOperation to apply as this method traverses the names of the data files - IMPORTANT the operation must be thread-safe.
     */
    public <T> void processDataFiles(int customerId, long equipmentId, long fromTime, long toTime, final EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues, DataFileOperation<T> operation){

        LOG.trace("begin processDataFiles for customer {} equipment {} from {} to {}", customerId, equipmentId, fromTime, toTime);

        String counterMetricId = servoMetricPrefix + "processDataFiles-count";
        String timerMetricId = servoMetricPrefix + "processDataFiles-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();

        long startTimeMs = System.currentTimeMillis();
        long endTimeMs;

        //if toTime is in the future - set it back to now
        if(toTime>startTimeMs){
            LOG.debug("toTime for the query is in the future {}. Limiting it to current time {}", toTime , startTimeMs);
            toTime = startTimeMs;
        }                        

        if((toTime - fromTime)/(60000L*this.numberOfMinutesPerFile) > 45000){
            //limit pre-generated file name list to 45000 ( ~ 32 days with 1 minute intervals)
            fromTime = toTime - 45000L*60000*this.numberOfMinutesPerFile;
            LOG.warn("Query asks for too much data. Limiting requested time range {} - {} to {} ms", fromTime, toTime, toTime - fromTime);
        }

        try{

            //Make use of hourly index (if present in the same directory as the data file)
            // For hour intervals that do not have corresponding hourly index
            // we will request hourly index to be built, and meanwhile
            // do the full data file scan.
            //Retrieve hourly index only once per query - it can be used for many data files within that hour

            HourlyIndexFileNames hourlyIndexFileNames = new HourlyIndexFileNames(customerId, equipmentId, fromTime, toTime, indexName, dsPrefix, fileNamePrefix, numberOfMinutesPerFile);

            LOG.debug("processDataFiles: will process {} hours of data files", hourlyIndexFileNames.getSize());

            //during each iteration through hourlyIndexFileNames maintain hourly interval for that hourly index file name
            //this is needed so we can retrieve data files for the same interval in case hourly index file is missing 
            long fromTimeHr = fromTime; 
            long toTimeHr = Math.min(toTime, 
                    fromTime 
                    + TimeUnit.HOURS.toMillis(hourlyIndexFileNames.getHourIncrement()) //add hour increment
                    - fromTime%TimeUnit.HOURS.toMillis(hourlyIndexFileNames.getHourIncrement()) //align on the beginning of the hour increment
                    - 1 //ensure that the next hour increment will be processed on the next iteration and there's no overlap between two time intervals
                    );

            List<Future<Void>> futures = new ArrayList<>(100);

            for(String hrIdxFileName: hourlyIndexFileNames){
                int numRetries = 0;
                final long curFromTimeHr = fromTimeHr;
                final long curToTimeHr = toTimeHr;
                //for each hour collect index information                  
                while(true){
                    try{
                        LOG.trace("submitting processDataFiles task - attempt {}", numRetries);
                        
                        futures.add(hdsProcessFilesExecutor.submit(new Callable<Void>(){
                            @Override
                            public Void call() throws Exception {
                                DirectoryIndex hrIdx;

                                if(indexName!=null && indexedValues!=null && ! indexedValues.isEmpty() 
                                        && curFromTimeHr < System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(70)) {
                                    hrIdx = HierarchicalDatastore.getZippedModelFromFile(dsRootDirName, hrIdxFileName, DirectoryIndex.class);
                                } 
                                else {
                                    //if fromTime is less than 70 minutes earlier than now, then skip hourly 
                                    //  index, as it has not been built yet - rely on record indexes in hazelcast instead
                                    hrIdx = null;
                                }

                                List<String> dataFileNames; 

                                LOG.trace("processDataFiles: processing interval from {} to {}", curFromTimeHr, curToTimeHr);

                                if(hrIdx!=null){
                                    //hourly index exists, use it to get the right data files (for this particular hour) and to get to the right lines in the data files
                                    LOG.trace("found hourly index {}", hrIdxFileName);
                                    dataFileNames = new ArrayList<>(hrIdx.getDataFileNames(curFromTimeHr, curToTimeHr, numberOfMinutesPerFile));
                                } else {
                                    //hourly index for this particular hour does not exist, request to build it - but only if fromTimeHr is older than 70 minutes from now
                                    if(indexName!=null && indexedValues!=null && ! indexedValues.isEmpty()
                                            && curFromTimeHr < System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(70))
                                    {
                                        scheduleRebuildOfHourlyIndex(indexName, curFromTimeHr, hrIdxFileName);
                                    }

                                    //meanwhile use full scan on the data files for this particular hour without hourly index
                                    LOG.trace("did not find hourly index {}, will use full scan for that hour", hrIdxFileName);
                                    dataFileNames = getFileNames(customerId, equipmentId, curFromTimeHr, curToTimeHr);
                                }

                                operation.processFiles(entryFilter, indexName, indexedValues, dataFileNames, hrIdx);   
                               return null;
                            }

                        }));
                        break;
                    }catch(RejectedExecutionException e){
                        LOG.trace("processDataFiles for {} - rejected execution, will retry", hrIdxFileName);

                        //could not submit task, will retry
                        try {
                            Thread.sleep(sleepBetweenRetriesMs);
                        } catch (InterruptedException e1) {
                            //do nothing
                            Thread.currentThread().interrupt();
                        }

                        numRetries++;

                        if(numRetries> maxRetries){
                            LOG.error("processDataFiles could not retrieve hourly index from {} after {} retries", hrIdxFileName, maxRetries);
                            throw new GenericErrorException("Could not retrieve hourly index from "+hrIdxFileName+" after "+maxRetries+" retries");
                        }
                    }
                }



                //adjust hour boundaries for the next iteration
                fromTimeHr = toTimeHr + 1; 
                toTimeHr = Math.min(toTime, 
                        fromTimeHr 
                        + TimeUnit.HOURS.toMillis(hourlyIndexFileNames.getHourIncrement())  //add hour increment
                        - 1 //ensure that the next hour increment will be processed on the next iteration and there's no overlap between two time intervals
                        );

            }

            //combine entries from all the files
            for(Future<Void> future: futures){
                try {
                    future.get();
                    LOG.trace("processDataFiles - collected future result");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GenericErrorException("Failed to collect result", e);
                } catch (ExecutionException e) {
                    LOG.error("processDataFiles could not retrieve data", e);
                    throw new GenericErrorException("Failed to collect result", e);
                }
            }

            endTimeMs = System.currentTimeMillis();
            LOG.debug("processDataFiles for customer {} equipment {} from {} to {} completed in {} ms.", customerId, equipmentId, fromTime, toTime, (endTimeMs - startTimeMs));

        }finally{
            endTimeMs = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTimeMs - startTimeMs), TimeUnit.MILLISECONDS);
        }

    }


    /**
     * Schedule a request to build an hourly index for specific customer, equipment and hour.
     * Only one caller (first among many threads and processes) will place request on the queue.
     * Actual rebuild of the index will happen when K2HDSConnector reads request 
     * from rebuildHrIdxQueue in hazelcast. 
     * 
     * Coordination happens via hazelcast queue rebuildHrIdxQueue and map bipHrDirs, configured by 
     * hazelcastObjectsConfiguration.getBuildInProgressHourlyDirectoryNamesMapPrefix() and
     * hazelcastObjectsConfiguration.getRebuildIdxHourlyDirectoryNamesQueue()
     * 
     * @param customerId
     * @param equipmentId
     * @param indexName - may be null, which will result in no action
     * @param timeHrMs - hour for which to build an index will be extracted from this timestamp.
     * @param hourlyIndexFileName - full name of the hourly index file
     */
    private void scheduleRebuildOfHourlyIndex(String indexName, long timeHrMs, String hourlyIndexFileName) {
        if(indexName == null || timeHrMs > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(70)){
            //does not make sense to build hourly index when index is not specified, or when hourly directory still has partial data.
            LOG.debug("scheduleRebuildOfHourlyIndex: skipping index creation because index is null or data is too recent");
            return;
        }

        if(hazelcastClient==null){
            LOG.debug("Hazelcast client is not configured for this datastore. Working in limited capacity. {}", fileNamePrefix);
            return;
        }

        //coordinate requests with potential other callers via hazelcast queue and map

        //check if another request to build indexes in this directory has already been scheduled
        String hourlyDirectoryName = hourlyIndexFileName.substring(0, hourlyIndexFileName.lastIndexOf('/'));
        IMap<String, Long> recordIdxMap = hazelcastClient.getMap(hazelcastObjectsConfiguration.getBuildInProgressHourlyDirectoryNamesMapPrefix());
        Long existingTs = recordIdxMap.putIfAbsent(hourlyDirectoryName, timeHrMs);

        if(existingTs!=null){
            //another request to build this index has already been scheduled, do nothing
            LOG.debug("Another request to re-build indexes for directory {} has been previoulsy scheduled. Ignoring this one.", hourlyDirectoryName);
            return;
        }

        //place re-build request on a queue, it will be picked up by HourlyIndexBuildRequestProcessor inside K2HDSConnector process
        IQueue<String> buildIndexRequestQueue = hazelcastClient.getQueue(hazelcastObjectsConfiguration.getRebuildIdxHourlyDirectoryNamesQueue()); 
        try {
            buildIndexRequestQueue.put(hourlyDirectoryName);
        } catch (InterruptedException e) {
            LOG.error("Cannot place request to rebuild indexes for {} into the queue: {}", hourlyDirectoryName, e);
            Thread.currentThread().interrupt();
        }

    }

    /**
     * This method is called by HourlyIndexBuildRequestProcessor
     *  when a request to build hourly index is read from the hazelcast queue.
     * The name of the queue comes from hazelcastObjectsConfiguration.getRebuildIdxHourlyDirectoryNamesQueue().
     * 
     * This method is part of HDS class so it can be used in HDS unit tests.
     */
    public static void rebuildHourlyIndex(String hourlyDirectoryName, 
            String dsRootDirName, String dsPrefix, RecordIndexRegistry recordIndexRegistry, 
            HazelcastInstance hazelcastClient, 
            HazelcastObjectsConfiguration hazelcastObjectsConfiguration
            ){

        HourlyIndexAggregatorHazelcastScalable hrIndexBuilder = new HourlyIndexAggregatorHazelcastScalable(
                dsRootDirName, dsPrefix, recordIndexRegistry, 
                hazelcastClient, hazelcastObjectsConfiguration
                );

        //Extract customerId, equipmentId, year, month, day, and hour from the directory name
        //@see com.telecominfraproject.wlan.hierarchical.datastore.HierarchicalDatastore.getFileName(int, long, int, int, int, int, int, long)
        //  for details about the structure of the file name
        try{
            String[] parts = hourlyDirectoryName.split("/");
            int customerId = Integer.parseInt(parts[1]);
            long equipmentId = Long.parseLong(parts[2]);
            int year = Integer.parseInt(parts[3]);
            int month = Integer.parseInt(parts[4]);
            int day = Integer.parseInt(parts[5]);
            int hour = Integer.parseInt(parts[6]);

            hrIndexBuilder.buildHourlyIndexForSingleHour(customerId, equipmentId, year, month, day, hour);

            //remove entry from the hazelcast map
            IMap<String, Long> recordIdxMap = hazelcastClient.getMap(hazelcastObjectsConfiguration.getBuildInProgressHourlyDirectoryNamesMapPrefix());
            recordIdxMap.remove(hourlyDirectoryName);

        }catch(Exception e){
            LOG.error("Could not build hourly index for {} : {}",  hourlyDirectoryName, e);
        }
    }

    private <T> List<T> getContent(
            final EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues, List<String> dataFileNames, DirectoryIndex hourlyIdx, Class<T> dataClass) {

        List<T> ret = new ArrayList<>(200);
        List<Future<List<T>>> futures = new ArrayList<>(100);

        //Go through supplied list of files to retrieve data from
        for(final String fileName: dataFileNames){
            int numRetries = 0;
            //for each file, get entries stored in it                    
            while(true){
                try{
                    LOG.trace("submitting getContent task - attempt {}", numRetries);

                    futures.add(hdsGetContentExecutor.submit(new Callable<List<T>>(){
                        @Override
                        public List<T> call() throws Exception {
                            return getContent(fileName, entryFilter, indexName, indexedValues, 
                                    hourlyIdx==null?null:hourlyIdx.getDataFileNameToRecordIndexMap().get(fileName), dataClass);
                        }

                    }));
                    break;
                }catch(RejectedExecutionException e){
                    LOG.trace("getContent for {} - rejected execution, will retry", fileName);

                    //could not submit task, will retry
                    try {
                        Thread.sleep(sleepBetweenRetriesMs);
                    } catch (InterruptedException e1) {
                        //do nothing
                        Thread.currentThread().interrupt();
                    }

                    numRetries++;

                    if(numRetries> maxRetries){
                        LOG.error("getContent could not retrieve data from {} after {} retries", fileName, maxRetries);
                        throw new GenericErrorException("Could not retrieve data from "+fileName+" after "+maxRetries+" retries");
                    }
                }
            }
        }

        LOG.trace("waiting for {} futures", futures.size());

        //combine entries from all the files
        for(Future<List<T>> future: futures){
            try {
                ret.addAll(future.get());
                LOG.trace("getContent - collected future result");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GenericErrorException("Failed to collect result", e);
            } catch (ExecutionException e) {
                LOG.error("getContent could not retrieve data", e);
                throw new GenericErrorException("Failed to collect result", e);
            }
        }

        return ret;
    }

    public <T> List<T> getEntries(int customerId, long fromTime, long toTime, final EntryFilter<T> entryFilter,
            String indexName, Set<String> indexedValues, Class<T> dataClass) {
        LOG.trace("begin getEntries for customer {} from {} to {}", customerId, fromTime, toTime);
        List<T> ret = new ArrayList<>(4000);

        Set<Long> equipmentIds = getEquipmentIds(customerId);
        LOG.trace("found {} equipment ids for customer {}", equipmentIds.size(), customerId);

        List<Future<List<T>>> futures = new ArrayList<>(equipmentIds.size());
        for(final long eqId: equipmentIds){
            int numRetries = 0;
            LOG.trace("Collecting entries for equipment {}", eqId);
            while(true){
                try{
                    futures.add(hdsGetEntriesExecutor.submit(new Callable<List<T>>(){
                        @Override
                        public List<T> call() throws Exception {
                            return getEntries(customerId, eqId, fromTime, toTime, entryFilter, indexName, indexedValues, dataClass);
                        } 
                        }));
                    break;
                }
                catch(RejectedExecutionException e){
                    LOG.trace("getEntries for {} - rejected execution, will retry", eqId);

                    //could not submit task, will retry
                    try {
                        Thread.sleep(sleepBetweenRetriesMs);
                    } catch (InterruptedException e1) {
                        //do nothing
                        Thread.currentThread().interrupt();
                    }

                    numRetries++;

                    if(numRetries> maxRetries){
                        LOG.error("getEntries could not getEntries for {} after {} retries", eqId, maxRetries);
                        throw new GenericErrorException("Could get entries for "+eqId+" after "+maxRetries+" retries");
                    }
                }
            }
        }
        LOG.trace("waiting for {} futures", futures.size());

        //combine entries from all the files
        for(Future<List<T>> future: futures){
            try {
                ret.addAll(future.get());
                LOG.trace("getEntries {} - collected future result",customerId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GenericErrorException("Failed to collect result", e);
            } catch (ExecutionException e) {
                LOG.error("getEntries could not retrieve data", e);
                throw new GenericErrorException("Failed to collect result", e);
            }
        }


        LOG.trace("getEntries for customer {} from {} to {} - found {} entries", customerId, fromTime, toTime, ret.size());

        return ret;
    }


    public <T> int countEntries(int customerId, long equipmentId, long fromTime, long toTime, final EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues, Class<T> dataClass){

        final AtomicInteger ret = new AtomicInteger();

        DataFileOperation<T> operation = new DataFileOperation<T>() {
            @Override
            public void processFiles(EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues,
                    List<String> dataFileNames, DirectoryIndex hourlyIdx) {
                ret.addAndGet(countEntries(entryFilter, indexName, indexedValues, dataFileNames, hourlyIdx, dataClass));
            }
        };

        processDataFiles(customerId, equipmentId, fromTime, toTime, entryFilter, indexName, indexedValues, operation);

        LOG.debug("countEntries for customer {} equipment {} from {} to {} Found {} results.", customerId, equipmentId, fromTime, toTime, ret.get());
        return ret.get();
    }

    public <T> int countEntries(final EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues, List<String> dataFileNames, DirectoryIndex hourlyIdx, Class<T> dataClass){
        int ret = 0;

        List<Future<Integer>> futures = new ArrayList<>(100);
        //Go through a list of data files
        for(final String fileName: dataFileNames){
            int numRetries = 0;
            //for each file, count entries stored in it
            while(true){
                try{
                    LOG.trace("submitting countEntries task - attempt {}", numRetries);

                    futures.add(hdsGetEntriesExecutor.submit(new Callable<Integer>(){
                        @Override
                        public Integer call() throws Exception {
                            return countEntries(fileName, entryFilter, indexName, indexedValues, 
                                    hourlyIdx==null?null:hourlyIdx.getDataFileNameToRecordIndexMap().get(fileName), dataClass);
                        }

                    }));
                    break;
                }catch(RejectedExecutionException e){

                    LOG.trace("countEntries for {} - rejected execution, will retry", fileName);

                    //could not submit task, will retry
                    try {
                        Thread.sleep(sleepBetweenRetriesMs);
                    } catch (InterruptedException e1) {
                        //do nothing
                        Thread.currentThread().interrupt();
                    }

                    numRetries++;

                    if(numRetries> maxRetries){
                        LOG.error("countEntries for could not retrieve data from {} after {} retries", 
                                fileName, maxRetries);
                        throw new GenericErrorException("Could not retrieve data from "+fileName+" after "+maxRetries+" retries");
                    }
                }
            }
        }

        LOG.trace("waiting for {} futures", futures.size());

        //combine entries from all the files
        for(Future<Integer> future: futures){
            try {
                ret+=future.get();
                LOG.trace("collected future result");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GenericErrorException("Failed to collect result", e);
            } catch (ExecutionException e) {
                LOG.error("countEntries could not retrieve data", e);
                throw new GenericErrorException("Failed to collect result", e);
            }
        }

        return ret;
    }

    public <T> int countEntries(int customerId, long fromTime, long toTime, final EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues, Class<T> dataClass){
        int ret = 0;
        LOG.trace("begin countEntries for customer {} from {} to {}", customerId, fromTime, toTime);

        Set<Long> equipmentIds = getEquipmentIds(customerId);
        LOG.trace("found {} equipment ids for customer {}", equipmentIds.size(), customerId);

        for(long eqId: equipmentIds){
            LOG.trace("Collecting entries for equipment {}", eqId);
            ret+=countEntries(customerId, eqId, fromTime, toTime, entryFilter, indexName, indexedValues, dataClass);
        }

        LOG.trace("getEntries for customer {} from {} to {} - found {} entries", customerId, fromTime, toTime, ret);

        return ret;
    }

    public int deleteFiles(int customerId, long equipmentId, long fromTime, long toTime){
        LOG.trace("begin deleteFiles for customer {} equipment {} from {} to {}", customerId, equipmentId, fromTime, toTime);

        String counterMetricId = servoMetricPrefix + "deleteFiles-count";
        String timerMetricId = servoMetricPrefix + "deleteFiles-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        try{
            int i = 0;
            //Get a list of files to delete
            for(String fileName: getFileNames(customerId, equipmentId, fromTime, toTime)){
                //for each file, delete it
                LOG.trace("deleting file {} ", fileName);
                File f = new File(dsRootDirName, fileName);
                boolean result = f.delete();
                if(result) {
                	LOG.trace("deleted file {}", fileName);
                    i++;
                } else {
                	LOG.warn("Could not delete file {}", fileName);
                }
                
            }

            LOG.trace("deleted {} files", i);

            return i;
        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }

    }


    public int deleteFiles(int customerId, long fromTime, long toTime){
        LOG.trace("begin deleteFiles for customer {} from {} to {}", customerId, fromTime, toTime);

        int i = 0;
        Set<Long> equipmentIds = getEquipmentIds(customerId);
        LOG.trace("found {} equipment ids for customer {}", equipmentIds.size(), customerId);

        for(long eqId: equipmentIds){
            LOG.trace("deleting files for equipment {}", eqId);
            i+=deleteFiles(customerId, eqId, fromTime, toTime);
        }

        return i;
    }

    public void deleteFile(int customerId, String relativeTargetObjectName){

        LOG.trace("begin deleteFile for customer {} {}", customerId, relativeTargetObjectName);

        String counterMetricId = servoMetricPrefix + "deleteFiles-count";
        String timerMetricId = servoMetricPrefix + "deleteFiles-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        try{
            String targetObjectName = getFileName(customerId, relativeTargetObjectName);
            
            File f = new File(dsRootDirName, targetObjectName);
            boolean result = f.delete();
            if(result) {
            	LOG.trace("deleted file {}", targetObjectName);
            } else {
            	LOG.warn("Could not delete file {}", targetObjectName);
            }

        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }

    }


    public void deleteFile(int customerId, long equipmentId, String relativeTargetObjectName){

        LOG.trace("begin deleteFile for customer {} equipment {}  {}", customerId, equipmentId, relativeTargetObjectName);

        String counterMetricId = servoMetricPrefix + "deleteFiles-count";
        String timerMetricId = servoMetricPrefix + "deleteFiles-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        try{
            String targetObjectName = getFileName(customerId, equipmentId, relativeTargetObjectName);
            File f = new File(dsRootDirName, targetObjectName);
            boolean result = f.delete();
            if(result) {
            	LOG.trace("deleted file {}", targetObjectName);
            } else {
            	LOG.warn("Could not delete file {}", targetObjectName);
            }
        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }

    }

    /**
     * @param customerId
     * @param fromTime
     * @param toTime
     * @return list of equipment Ids taken from the file names for a given customer in a given time range
     */
    //TODO this is not too efficient, may need to maintain a separate index
    public Set<Long> getEquipmentIds(int customerId) {
        LOG.trace("begin getEquipmentIds for customer {}", customerId);

        String counterMetricId = servoMetricPrefix + "getEquipmentIds-count";
        String timerMetricId = servoMetricPrefix + "getEquipmentIds-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTimeMs = System.currentTimeMillis();
        long endTimeMs = startTimeMs;

        Set<Long> ret = new HashSet<>();
        
        //cache customer-equipment mappings in hazelcast for ~2 hrs
        
        IMap<Integer, Set<Long>> dirCustomerEquipmentMap = null;
        
        if(hazelcastClient!=null){
            //try to find directory listing for customer equipment in hazelcast map dir-list<customerId,Set<equipmentId>>
            dirCustomerEquipmentMap = hazelcastClient.getMap(hazelcastObjectsConfiguration.getHdsDirectoryCustomerEquipmentMapName());
            
            Set<Long> equipmentIds = dirCustomerEquipmentMap.get(customerId);
            if(equipmentIds!=null){
                ret.addAll(equipmentIds);
            }
            
        }
        
        try{

            if(ret.isEmpty()){
                //did not find anything in cache, go to files to get customer-equipment mappings from the directory structure

                StringBuilder sb = new StringBuilder(1024);
                Formatter formatter = new Formatter(sb, null);
                //            formatter.format("%s/%d/%d/%4d/%02d/%02d/%02d/%s_%d_%d_%4d_%02d_%02d_%02d_%02d.zip",
                //                    dsPrefix, customerId, equipmentId, year, month, day, hour,
                //                    fileNamePrefix, customerId, equipmentId, year, month, day, hour, minute
                //                    );
                formatter.format("%s/%d/",dsPrefix, customerId);
                formatter.close();
    
                String computedPrefix = sb.toString();
    
                File parentFolder = new File(dsRootDirName + File.separator + computedPrefix);
                
                FilenameFilter fnameFilter = new  FilenameFilter() { public boolean accept(File file, String name){ return true;}};
                
                //This will process a list of sub-folder names, each representing an equipmentId.
                for(String fName: parentFolder.list(fnameFilter)) {
    
                        try{
                            long equipmentId = Long.parseLong(fName);
                            ret.add(equipmentId);
                        }catch(NumberFormatException e){
                            //skip this, does not seem to be equipment id
                        }
    
                }
    
                endTimeMs = System.currentTimeMillis();
                
                //store retrieved mappings in hazelcast for ~2 hrs
                if(hazelcastClient!=null){
                    dirCustomerEquipmentMap = hazelcastClient.getMap(hazelcastObjectsConfiguration.getHdsDirectoryCustomerEquipmentMapName());
                    dirCustomerEquipmentMap.put(customerId, ret);
                }
            }
            
            LOG.debug("Found equipment ids for customer {} in {} ms: {}", customerId, (endTimeMs - startTimeMs), ret);
        } finally{
            endTimeMs = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTimeMs - startTimeMs), TimeUnit.MILLISECONDS);
        }

        return ret;
    }
       

    public Set<Integer> getCustomerIds() {
        LOG.trace("begin getCustomerIds");

        String counterMetricId = servoMetricPrefix + "getCustomerIds-count";
        String timerMetricId = servoMetricPrefix + "getCustomerIds-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();
        long endTimeMs;

        Set<Integer> ret = new HashSet<>();
        try{
            StringBuilder sb = new StringBuilder(1024);
            Formatter formatter = new Formatter(sb, null);
            //            formatter.format("%s/%d/%d/%4d/%02d/%02d/%02d/%s_%d_%d_%4d_%02d_%02d_%02d_%02d.zip",
            //                    dsPrefix, customerId, equipmentId, year, month, day, hour,
            //                    fileNamePrefix, customerId, equipmentId, year, month, day, hour, minute
            //                    );
            formatter.format("%s/",dsPrefix);
            formatter.close();

            long startTimeMs = System.currentTimeMillis();
            String computedPrefix = sb.toString();
            
            File parentFolder = new File(dsRootDirName + File.separator + computedPrefix);
            
            FilenameFilter fnameFilter = new  FilenameFilter() { public boolean accept(File file, String name){ return true;}};
            
            //This will process a list of sub-folder names, each representing a customerId.
            for(String fName: parentFolder.list(fnameFilter)) {
                    try{
                        int customerId = Integer.parseInt(fName);
                        ret.add(customerId);
                    }catch(NumberFormatException e){
                        //skip this, does not seem to be equipment id
                    }
            }

            endTimeMs = System.currentTimeMillis();

            LOG.debug("Found customer {} ids ms: {}", ret.size(), (endTimeMs - startTimeMs));
        } finally{
            endTimeMs = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTimeMs - startTime), TimeUnit.MILLISECONDS);
        }

        return ret;
    }

    public Set<String> getFileNamesForOneHour(int customerId, long equipmentId, long time) {
        return getFileNamesForOneHour(customerId, equipmentId, time, fileNamePrefix);
    }

    private Set<String> getFileNamesForOneHour(int customerId, long equipmentId, long time, String fileNamePrefix) {
        LOG.trace("begin getFileNamesForOneHour");

        String counterMetricId = servoMetricPrefix + "getFileNamesForOneHour-count";
        String timerMetricId = servoMetricPrefix + "getFileNamesForOneHour-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();
        long endTimeMs;

        //adjust supplied time to the boundary of 1 hour
        time = time - time%TimeUnit.HOURS.toMillis(1);

        Calendar fromCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        fromCalendar.setTime(new Date(time));

        int year = fromCalendar.get(Calendar.YEAR);
        int month = fromCalendar.get(Calendar.MONTH) + 1;
        int day = fromCalendar.get(Calendar.DAY_OF_MONTH);
        int hour = fromCalendar.get(Calendar.HOUR_OF_DAY);            

        Set<String> ret = new HashSet<>();
        try{
            StringBuilder sb = new StringBuilder(1024);
            Formatter formatter = new Formatter(sb, null);
            //            formatter.format("%s/%d/%d/%4d/%02d/%02d/%02d/%s_%d_%d_%4d_%02d_%02d_%02d_%02d.zip",
            //                    dsPrefix, customerId, equipmentId, year, month, day, hour,
            //                    fileNamePrefix, customerId, equipmentId, year, month, day, hour, minute
            //                    );
            formatter.format("%s/%d/%d/%4d/%02d/%02d/%02d/",
                    dsPrefix, customerId, equipmentId, year, month, day, hour);                                
            formatter.close();

            long startTimeMs = System.currentTimeMillis();
            String computedPrefix = sb.toString();

            File parentFolder = new File(dsRootDirName + File.separator + computedPrefix);
            
            FilenameFilter fnameFilter = new  FilenameFilter() { public boolean accept(File file, String name){ return true;}};
            
            //This will process a list of files in a given folder
            for(String fName: parentFolder.list(fnameFilter)) {

                    if(fName.startsWith(fileNamePrefix)){
                        //we're only interested in files with supplied prefix 
                        ret.add(computedPrefix + fName);
                    }

            }

            endTimeMs = System.currentTimeMillis();

            LOG.debug("Found {} file names ms: {}", ret.size(), (endTimeMs - startTimeMs));
        } finally{
            endTimeMs = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTimeMs - startTime), TimeUnit.MILLISECONDS);
        }

        return ret;
    }

    public void uploadStreamToFileOverwriteOld(InputStream inputStream, long dataSize, String targetObjectName){
        uploadStream(inputStream, dataSize, targetObjectName, ObjectExistsBehaviour.overwriteOld);
    }

    public void upload(BaseJsonModel model, int customerId, String relativeTargetObjectName){
        String targetObjectName = getFileName(customerId, relativeTargetObjectName);

        byte[] modelBytes = model.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(modelBytes);
        uploadStream(bais, modelBytes.length, targetObjectName, ObjectExistsBehaviour.overwriteOld);
    }

    public void upload(BaseJsonModel model, int customerId, long equipmentId, String relativeTargetObjectName){
        String targetObjectName = getFileName(customerId, equipmentId, relativeTargetObjectName);

        byte[] modelBytes = model.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(modelBytes);
        uploadStream(bais, modelBytes.length, targetObjectName, ObjectExistsBehaviour.overwriteOld);
    }

    public BaseJsonModel getContentAsModelOrNull(int customerId, String relativeTargetObjectName){
        String targetObjectName = getFileName(customerId, relativeTargetObjectName);
        return getContentAsModelOrNull(targetObjectName);
    }

    public BaseJsonModel getContentAsModelOrNull(int customerId, long equipmentId, String relativeTargetObjectName){
        String targetObjectName = getFileName(customerId, equipmentId, relativeTargetObjectName);
        return getContentAsModelOrNull(targetObjectName);
    }

    private static enum ObjectExistsBehaviour{
        ignoreNew,
        overwriteOld,
        appendToOld,
        createNewFileWithAttemptNumber
    }

    public void uploadStream(InputStream inputStream, long dataSize, String fullTargetObjectName, ObjectExistsBehaviour whenObjectExists){

        String counterMetricId = servoMetricPrefix + "upload-count";
        String timerMetricId = servoMetricPrefix + "upload-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        try{

            int maxAttempts = 5;
            String effectiveTargetName = fullTargetObjectName;
            boolean targetObjectExists = false;

            LOG.debug("uploadStream: {}/{}", dsRootDirName, effectiveTargetName);

            if(whenObjectExists!=ObjectExistsBehaviour.overwriteOld) {

                //check if a file with the same name was already uploaded
                try{
                    File f = new File(dsRootDirName, effectiveTargetName);
                    targetObjectExists = f.exists();
                }catch(Exception e){
                    //expected it
                }

                if(targetObjectExists){
                    //if target object already exists, then apply specified behaviour 
                    switch(whenObjectExists){
                    case appendToOld:
                        //TODO: implement me
                        break;
                    case createNewFileWithAttemptNumber:
                        //try with another name, appended with "_attemptNumber"
                        for(int attemptNumber=1; attemptNumber<=maxAttempts && targetObjectExists; attemptNumber++){
                            targetObjectExists = false;
                            effectiveTargetName = fullTargetObjectName + "_" + attemptNumber;
                            try{
                                File f = new File(dsRootDirName, effectiveTargetName);
                                targetObjectExists = f.exists();
                            }catch(Exception e){
                                //expected it
                            }
                        }

                        if(targetObjectExists){
                            //object with that name still exists, ran out of attempts
                            LOG.error("File already exists {}/{} - could not upload after {} attempts",dsRootDirName,fullTargetObjectName, maxAttempts);
                            return;                            
                        }

                        break;
                    case ignoreNew:
                        LOG.info("File already exists {}/{} - will NOT upload this one",dsRootDirName,effectiveTargetName);
                        return;
                    case overwriteOld:
                        LOG.info("File already exists {}/{} - overwriting with this one",dsRootDirName,effectiveTargetName);
                        break;
                    default:
                        throw new IllegalArgumentException("Do not know how to deal with flag "+whenObjectExists);
                    }
                }

            }

            LOG.info("Upload started: {}/{}", dsRootDirName, effectiveTargetName);

            File parentDir = new File(dsRootDirName + File.separator + effectiveTargetName.substring(0, effectiveTargetName.lastIndexOf(File.separator)));
            if(!parentDir.exists()) {
            	LOG.info("Creating datastore dir {}", parentDir.getAbsoluteFile());
            	parentDir.mkdirs();
            }

            try(FileOutputStream fos = new FileOutputStream(new File(dsRootDirName, effectiveTargetName))) {
            	StreamUtils.copy(inputStream, fos);
            	fos.flush();
                LOG.info("Upload complete: {}/{}", dsRootDirName, effectiveTargetName);
            } catch (IOException e) {
                LOG.error("Unable to upload stream into {}/{}, upload was aborted. {}", dsRootDirName, effectiveTargetName, e);
            }

        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }

    }

    /**
     * Reads entries from the database and places them into supplied outputStream.
     * Primary purpose of this method is to retrieve large data sets and place them into local files without blowing through the memory limits.
     * @param outputStream - stream to write entries to. Stream will not be closed at the end!!!
     * @param customerId
     * @param equipmentId
     * @param fromTime
     * @param toTime
     * @param entryFilter - filter to apply to entries, only those that pass will appear in the outputStream
     * @param modelConverter - class that converts extracted model to a different one before writing the latter into the outputStream
     * @param indexName - index to use, can be null - will result in full scan of the data files
     * @param indexedValues - if index is used then indexedValues specifies what values should be matched by the index. Can be null or empty, in which case will result in full scan of the data files
     * @param dataClass - data class
     * 
     */
    public <T> void streamEntries(OutputStream outputStream, int customerId, long equipmentId, long fromTime, long toTime, 
            EntryFilter<T> entryFilter, 
            BaseModelConverter<T, ?extends BaseJsonModel> modelConverter, 
            String indexName, Set<String> indexedValues, Class<T> dataClass){

        LOG.trace("begin streamEntries for customer {} equipment {} from {} to {}", customerId, equipmentId, fromTime, toTime);

        DataFileOperation<T> operation = new DataFileOperation<T>() {
            @Override
            public void processFiles(EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues,
                    List<String> dataFileNames, DirectoryIndex hourlyIdx) {

                for(String fileName: dataFileNames){
                    //for each file, stream entries stored in it
                    synchronized (this) {
                        streamContent(outputStream, fileName, entryFilter, modelConverter, indexName, 
                                indexedValues, 
                                hourlyIdx==null?null:hourlyIdx.getDataFileNameToRecordIndexMap().get(fileName), dataClass);
                    }
                }

            }
        };

        processDataFiles(customerId, equipmentId, fromTime, toTime, entryFilter, indexName, indexedValues, operation);

        LOG.trace("end streamEntries for customer {} equipment {} from {} to {}", customerId, equipmentId, fromTime, toTime);
    }

    /**
     * Reads entries from the database and places them into supplied outputStream.
     * Primary purpose of this method is to retrieve large data sets and place them into local files without blowing through the memory limits.
     * @param outputStream - stream to write entries to. Stream will not be closed at the end!!!
     * @param customerId
     * @param equipmentId
     * @param fromTime
     * @param toTime
     * @param entryFilter - filter to apply to entries, only those that pass will appear in the outputStream
     * @param modelExtractor - class that extracts model from the entry for conversion, i.e. extract SystemEvent from SystemEventRecord.payload
     * @param modelConverter - class that converts extracted model to a different one before writing the latter into the outputStream
     * @param indexName - index to use, can be null - will result in full scan of the data files
     * @param indexedValues - if index is used then indexedValues specifies what values should be matched by the index. Can be null or empty, in which case will result in full scan of the data files
     * 
     */
    public <T, N extends BaseJsonModel> void streamEntries(OutputStream outputStream, int customerId, long equipmentId, long fromTime, long toTime, 
            EntryFilter<T> entryFilter, BaseModelConverter<T, N> modelExtractor, 
            BaseModelConverter<N, ?extends BaseJsonModel> modelConverter, 
            String indexName, Set<String> indexedValues, Class<T> dataClass){

        LOG.trace("begin streamEntries for customer {} equipment {} from {} to {}", customerId, equipmentId, fromTime, toTime);

        DataFileOperation<T> operation = new DataFileOperation<T>() {
            @Override
            public void processFiles(EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues,
                    List<String> dataFileNames, DirectoryIndex hourlyIdx) {

                for(String fileName: dataFileNames){
                    //for each file, stream entries stored in it
                    synchronized (this) {
                        streamContent(outputStream, fileName, entryFilter, modelExtractor, modelConverter, indexName,  
                                indexedValues, 
                                hourlyIdx==null?null:hourlyIdx.getDataFileNameToRecordIndexMap().get(fileName), dataClass);
                    }
                }

            }
        };

        processDataFiles(customerId, equipmentId, fromTime, toTime, entryFilter, indexName, indexedValues, operation);

        LOG.trace("end streamEntries for customer {} equipment {} from {} to {}", customerId, equipmentId, fromTime, toTime);
    }

    /**
     * Reads entries from the database and places them into supplied outputStream.
     * Primary purpose of this method is to retrieve large data sets and place them into local files without blowing through the memory limits.
     * 
     * Objects are parsed from lines inside zipped (JSON) file stored under specified key.
     * One object per line.
     * 
     * @param outputStream - stream to write entries to, will NOT be closed at the end.
     * @param fileName - name of the data file to read entries from (full fileName)
     * @param entryFilter - filter to apply to entries, only those that pass will appear in the outputStream
     * @param modelConverter - class that converts extracted model to a different one before writing the latter into the outputStream 
     * @param indexName - index to use, can be null - will result in full scan of the data files
     * @param indexedValues - if index is used then indexedValues specifies what values should be matched by the index. Can be null or empty, in which case will result in full scan of the data files
     * @param recordIndex - record index object to get matching positions from, can be null, in which case an attempt will be made to get record index from hazelcast
     * @param dataClass - data class
     */
    public <T> void streamContent(OutputStream outputStream, String fileName, EntryFilter<T> entryFilter, 
            BaseModelConverter<T, ?extends BaseJsonModel> modelConverter, 
            String indexName, Set<String> indexedValues, RecordIndex recordIndex, Class<T> dataClass){

        LOG.trace("begin streamContent for {}", fileName);

        String counterMetricId = servoMetricPrefix + "streamContent-count";
        String timerMetricId = servoMetricPrefix + "streamContent-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        // use record index in here to get the set of matching lines in the json datafile - positions corresponding to indexed values
        Set<Integer> matchingLines = getMatchingPositionsFromIndex(recordIndex, fileName, indexName, indexedValues);

        if(matchingLines!=null && matchingLines.isEmpty()){
            //no need to retrieve the data file - it does not have any of the indexed values
            return;
        }

        try(FileInputStream fis = new FileInputStream(dsRootDirName+ File.separator + fileName)){

            LOG.trace("found object {}", fileName);

            List<T> filteredEntries = getContent(fis, entryFilter, matchingLines, dataClass);

            for(T singleEntry: filteredEntries){
                BaseJsonModel  convertedModel = modelConverter.convert(singleEntry);

                if(convertedModel!=null){
                    outputStream.write(convertedModel.toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.write(13);
                    outputStream.write(10);
                }
            }

        } catch (FileNotFoundException e){
            LOG.trace("object not found {}", fileName);
            return;
        } catch (IOException e) {
            LOG.error("Cannot stream items : ", e);
            throw new GenericErrorException("Failed to stream items", e);
        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }

    }

    /**
     * Reads entries from the database and places them into supplied outputStream.
     * Primary purpose of this method is to retrieve large data sets and place them into local files without blowing through the memory limits.
     * 
     * Objects are parsed from lines inside zipped (JSON) file stored under specified key.
     * One object per line.
     * 
     * @param outputStream - stream to write entries to, will NOT be closed at the end.
     * @param fileName - name of the data file to read entries from
     * @param entryFilter - filter to apply to entries, only those that pass will appear in the outputStream
     * @param modelExtractor - class that extracts model from the entry for conversion, i.e. extract SystemEvent from SystemEventRecord.payload
     * @param modelConverter - class that converts extracted model to a different one before writing the latter into the outputStream 
     * @param indexName - index to use, can be null - will result in full scan of the data files
     * @param indexedValues - if index is used then indexedValues specifies what values should be matched by the index. Can be null or empty, in which case will result in full scan of the data files
     * @param recordIndex - record index object to get matching positions from, can be null, in which case an attempt will be made to get record index from hazelcast
     * @param dataClass - data class
     */
    public <T, N extends BaseJsonModel> void streamContent(OutputStream outputStream, String fileName,
            EntryFilter<T> entryFilter, BaseModelConverter<T, N> modelExtractor,
            BaseModelConverter<N, ? extends BaseJsonModel> modelConverter, String indexName, Set<String> indexedValues,
            RecordIndex recordIndex, Class<T> dataClass) {

        LOG.trace("begin streamContent for {}", fileName);

        String counterMetricId = servoMetricPrefix + "streamContent-count";
        String timerMetricId = servoMetricPrefix + "streamContent-timer";

        Counter cnt = CloudMetricsUtils.getCounter(counterMetricId);
        cnt.increment();
        long startTime = System.currentTimeMillis();

        // use record index in here to get the set of matching lines in the json datafile - positions corresponding to indexed values
        Set<Integer> matchingLines = getMatchingPositionsFromIndex(recordIndex, fileName, indexName, indexedValues);

        if(matchingLines!=null && matchingLines.isEmpty()){
            //no need to retrieve the data file - it does not have any of the indexed values
            return;
        }

        try(FileInputStream fis = new FileInputStream(dsRootDirName+ File.separator + fileName)){
            LOG.trace("found object {}", fileName);

            List<T> filteredEntries = getContent(fis, entryFilter, matchingLines, dataClass);

            for(T singleEntry: filteredEntries){

                BaseJsonModel convertedModel = null;
                //first step - extract model for conversion, i.e. extract SystemEvent from SystemEventRecord.payload
                N extractedModel = modelExtractor.convert(singleEntry);
                if(extractedModel!=null){
                    //second step - convert extracted model
                    convertedModel = modelConverter.convert(extractedModel);
                }

                if(convertedModel!=null){
                    outputStream.write(convertedModel.toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.write(13);
                    outputStream.write(10);
                }
            }

        } catch (FileNotFoundException e){
            LOG.trace("object not found {}", fileName);
            return;
        } catch (IOException e) {
            LOG.error("Cannot stream items : ", e);
            throw new GenericErrorException("Failed to stream items", e);
        } finally{
            long endTime = System.currentTimeMillis();
            Timer tmr = CloudMetricsUtils.getTimer(timerMetricId);
            tmr.record((endTime - startTime), TimeUnit.MILLISECONDS);
        }
    }


    public static Map<String, Long> getFileNamesAndLastMods(String rootDirName, String dirKey){

        String fullFileName;
        Long lastmodTs;
        Map<String,Long> fileNameToLastModMap = new HashMap<>();

        File parentFolder = new File(rootDirName + File.separator + dirKey);
        
        FilenameFilter fnameFilter = new  FilenameFilter() { public boolean accept(File file, String name){ return true;}};
        
        //This will process a list of files in a given folder
        for(String fName: parentFolder.list(fnameFilter)) {

                fullFileName = dirKey + fName;
                File tf = new File(dirKey, fName);
                lastmodTs = tf.lastModified();

                fileNameToLastModMap.put(fullFileName, lastmodTs);
        }            

        return fileNameToLastModMap;
    }


    public String getDsRootDirName() {
        return dsRootDirName;
    }

    public String getDsPrefix() {
        return dsPrefix;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public long getIdleTimeoutBeforeFlushingMs() {
        return idleTimeoutBeforeFlushingMs;
    }


    /**
     * Store record index counts and positions in hazelcast for the next 2 hours
     * 
     * @param idxName
     * @param idxCounts
     * @param idxPositions
     * @param fullFileName - name of the data file that this index represents
     */
    public void storeRecordIndex(String idxName, RecordIndexCounts idxCounts,
            RecordIndexPositions idxPositions, String fullFileName) {

        if(hazelcastClient==null){
            LOG.debug("Hazelcast client is not configured for this datastore {}. Working in limited capacity", fileNamePrefix);
            return;
        }

        String recordIdxMapName =  hazelcastObjectsConfiguration.getRecordIndexMapPrefix() + hazelcastMapPrefix + idxName;
        IMap<String, byte[]> recordIdxMap = hazelcastClient.getMap(recordIdxMapName);

        String idxKey = HierarchicalDatastore.getIndexFileName(fullFileName, idxName);

        RecordIndex countsAndPositions = new RecordIndex(idxCounts, idxPositions);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        try {
            baos.write(countsAndPositions.toZippedBytes());
            baos.flush();
        } catch (IOException e) {
            throw new GenericErrorException("Failed to store record index", e);
        }

        byte[] idxBytes = baos.toByteArray();

        recordIdxMap.put(idxKey, idxBytes);            

    }

    /**
     * Retrieve record index for a given data file from hazelcast.
     * @param idxName
     * @param fullFileName - name of the data file
     * @return record index for the specified data file, or null
     */
    public RecordIndex findRecordIndex(String idxName, String fullFileName){
        return findRecordIndex(hazelcastObjectsConfiguration.getRecordIndexMapPrefix(), hazelcastMapPrefix, hazelcastClient, idxName, fullFileName);
    }

    /**
     * Retrieve record index for a given data file from hazelcast.
     * @param recordIndexMapPrefix
     * @param hazelcastMapPrefix
     * @param hazelcastClient
     * @param idxName
     * @param fullFileName
     * @return record index for the specified data file, or null
     */
    public static RecordIndex findRecordIndex(String recordIndexMapPrefix, String hazelcastMapPrefix, HazelcastInstance hazelcastClient, String idxName, String fullFileName){

        if(hazelcastClient==null){
            LOG.debug("Hazelcast client is not configured for this datastore. Working in limited capacity. {}", fullFileName);
            return null;
        }

        String recordIdxMapName =  recordIndexMapPrefix + hazelcastMapPrefix + idxName;
        IMap<String, byte[]> recordIdxMap = hazelcastClient.getMap(recordIdxMapName);

        String idxKey = HierarchicalDatastore.getIndexFileName(fullFileName, idxName);

        byte[] idxBytes = recordIdxMap.get(idxKey);

        if(idxBytes == null){
            return null;
        }

        RecordIndex countsAndPositions = BaseJsonModel.fromZippedBytes(idxBytes, RecordIndex.class); 

        return countsAndPositions;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BaseJsonModel> T getModelFromFile(String dsRootDirName, String fileName) {
    	try(FileInputStream fis = new FileInputStream(dsRootDirName+ File.separator + fileName)){
            LOG.trace("found object ");
            return (T) BaseJsonModel.fromString(StreamUtils.copyToString(fis, StandardCharsets.UTF_8), BaseJsonModel.class);
        } catch (FileNotFoundException e){
            LOG.trace("object {} does not exist ", fileName);
            return null;
        } catch (IOException e) {
            throw new GenericErrorException("Failed to get object from file", e);
        }

    }

    public static <T extends BaseJsonModel> T getZippedModelFromFile(String dsRootDirName, String fileName, Class<T> dataClass) {
        List<T> list = getZippedModelsFromFile(dsRootDirName, fileName, dataClass);

        if(list == null || list.isEmpty()){
            return null;
        }

        return list.get(0);

    }

    public static <T extends BaseJsonModel> List<T> getZippedModelsFromFile(String dsRootDirName, String fileName, Class<T> dataClass) {

        try(FileInputStream fis = new FileInputStream(dsRootDirName+ File.separator + fileName)){
            LOG.trace("found object");
            EntryFilter<T> entryFilter = new EntryFilter<T>() {
                @Override
                public T getFilteredEntry(T entry) {
                    return entry;
                }
            };

            return HierarchicalDatastore.getContent(fis, entryFilter , null, dataClass);

        } catch (FileNotFoundException e){
            LOG.trace("object {} does not exist", fileName);
            return null;
        } catch (IOException e) {
            throw new GenericErrorException("Failed to load zipped models from file", e);
        }

    }

}