package com.telecominfraproject.wlan.hierarchical.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.telecominfraproject.wlan.core.model.filter.EntryFilter;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.hazelcast.HazelcastForUnitTest;
import com.telecominfraproject.wlan.hazelcast.HazelcastForUnitTest.HazelcastUnitTestManager;
import com.telecominfraproject.wlan.hazelcast.common.HazelcastObjectsConfiguration;
import com.telecominfraproject.wlan.hierarchical.datastore.index.DirectoryIndex;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexValueExtractor;
import com.telecominfraproject.wlan.hierarchical.datastore.index.registry.RecordIndexRegistry;
import com.telecominfraproject.wlan.hierarchical.datastore.writer.StreamHolder;

/**
 * @author dtoptygin
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = HierarchicalDatastoreHourlyIndexTests.class)
@Import(value = { 
        HazelcastForUnitTest.class,
        RecordIndexRegistry.class,
        HazelcastObjectsConfiguration.class,
        PropertySourcesPlaceholderConfigurer.class, //must have this to resolve non-string @Value annotations, i.e. int properties, etc.
        })
@ActiveProfiles({"HazelcastForUnitTest"})
public class HierarchicalDatastoreHourlyIndexTests {
    
    static{
        System.setProperty("tip.wlan.hdsExecutorQueueSize", "5000");
        System.setProperty("tip.wlan.hdsExecutorThreads", "10");
        System.setProperty("tip.wlan.hdsExecutorCoreThreadsFactor", "1");
        HazelcastUnitTestManager.initializeSystemProperty(HierarchicalDatastoreHourlyIndexTests.class);
    }

    static final HazelcastUnitTestManager testManager = new HazelcastUnitTestManager();

    @Autowired
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        testManager.registerInstance(hazelcastInstance);
    }

    @AfterClass
    public static void shutdown() {
        testManager.shutdownAllInstances();
    }
    
    private static final String dsRootDirName = "hds-test";
    private static final String dsPrefix = "testDsHIT";

    String fileNamePrefix = "testF";
    String hazelcastMapPrefix = fileNamePrefix+"-";
    int numberOfMinutesPerFile = 1;
    
    private HazelcastInstance hazelcastInstance;
    @Autowired HazelcastObjectsConfiguration hazelcastObjectsConfiguration;
    
    @Autowired RecordIndexRegistry recordIndexRegistry;
    
    HierarchicalDatastore hDs;
    
    ExecutorService executor = Executors.newFixedThreadPool(8, new ThreadFactory(){
        int cnt;
        @Override
        public Thread newThread(Runnable r) {
            Thread thr = new Thread(r, "UnitTest-HierarchicalDatastoreTests-"+(cnt++));
            thr.setDaemon(true);
            return thr;
        }
    });

    @AfterClass
    public static void removeAllHdsFiles(){
    	File dsRootDir = new File(dsRootDirName + File.separator + dsPrefix);
    	HdsCommonTests.removeAllHdsFiles(dsRootDir);
    }   
    
    @PostConstruct
    void initHds(){
    	//remove previous datastore content, if any
    	removeAllHdsFiles();

        hDs = new HierarchicalDatastore(dsRootDirName, dsPrefix, fileNamePrefix, numberOfMinutesPerFile, 20L, 
                hazelcastInstance, hazelcastMapPrefix, hazelcastObjectsConfiguration, recordIndexRegistry);
    }
    
    
    private final static String recordTypeIdx = "recordType";
    private final static String clientIdx = "client";
    private final static String manyClientIdx = "manyClient";
    
    @Test
    public void testNormalOneIndex() throws IOException{

        //create 2 data files in s3ds, 2 records each - one with record index, one without any indexes

        String type1 = "t1";
        String type2 = "t2";
        String type3 = "t3";
        String type4 = "t4";
        String type5 = "t5";
        String client1 = "c1";
        String client2 = "c2";
        String value1 = "v1";
        String value2 = "v2";
        String value3 = "v3";
        String value4 = "v4";
        
        TestModelForHds mdl1 = new TestModelForHds(type1, client1, value1);
        TestModelForHds mdl2 = new TestModelForHds(type2, client2, value2);
        TestModelForHds mdl3 = new TestModelForHds(type1, client1, value3);
        TestModelForHds mdl4 = new TestModelForHds(type2, client2, value4);
        
        int customerId = (int)System.currentTimeMillis();
        long equipmentId = System.currentTimeMillis();       
        //if time is less than 70 minutes from now, hourly indexes will not be used, so we'll adjust time to be in the past
        long streamFirstModelStartTimeMs = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2);
        
        //align our data files at the beginning of the hour, so that if test runs at the end of the hour we do not end up with 2 hourly directories
        streamFirstModelStartTimeMs = streamFirstModelStartTimeMs 
                - streamFirstModelStartTimeMs%TimeUnit.HOURS.toMillis(1) 
                + ((long)(100*Math.random()));

        //first write a datafile when no indexes are present
        StreamHolder streamHolder1 = new StreamHolder(streamFirstModelStartTimeMs , customerId, equipmentId, hDs);        
        streamHolder1.writeModelToStream(mdl1);
        streamHolder1.writeModelToStream(mdl2);
        streamHolder1.commitOutputStreamToFile();
        
        String dataFileName = streamHolder1.getFullFileName();
        String hourlyDirectoryName = dataFileName.substring(0, dataFileName.lastIndexOf('/')+1);
        HourlyIndexFileNames recordTypeIdxHourlyIndexFileNames = new HourlyIndexFileNames(
                customerId, equipmentId, streamFirstModelStartTimeMs, streamFirstModelStartTimeMs+ 1000, 
                recordTypeIdx, dsPrefix, fileNamePrefix, numberOfMinutesPerFile);
        String recordTypeIdxHourlyIdxFileName = recordTypeIdxHourlyIndexFileNames.iterator().next();
        
        HourlyIndexFileNames clientIdxHourlyIndexFileNames = new HourlyIndexFileNames(
                customerId, equipmentId, streamFirstModelStartTimeMs, streamFirstModelStartTimeMs+ 1000, 
                clientIdx, dsPrefix, fileNamePrefix, numberOfMinutesPerFile);
        String clientIdxHourlyIdxFileName = clientIdxHourlyIndexFileNames.iterator().next();
        
        
        //verify that hourly index is not present
        Map<String,Long> fileNameToLastModMap = HierarchicalDatastore.getFileNamesAndLastMods(dsRootDirName, hourlyDirectoryName);
        assertTrue(checkIfS3ObjectExists(streamHolder1.getFullFileName()));
        assertEquals(1, fileNameToLastModMap.size());        
        assertTrue(fileNameToLastModMap.keySet().contains(dataFileName));

        //Build hourly index - should build none, since no indexes were registered with this datastore.
        HierarchicalDatastore.rebuildHourlyIndex(hourlyDirectoryName, dsRootDirName, dsPrefix, recordIndexRegistry, hazelcastInstance, hazelcastObjectsConfiguration);

        //verify that hourly index is not present
        fileNameToLastModMap = HierarchicalDatastore.getFileNamesAndLastMods(dsRootDirName, hourlyDirectoryName);
        assertEquals(1, fileNameToLastModMap.size());        
        assertTrue(fileNameToLastModMap.keySet().contains(dataFileName));
        
        //now register an index and write a second datafile, with an index
        RecordIndexValueExtractor recordTypeIdxValueExtractor = new RecordIndexValueExtractor() {
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                return Collections.singleton(((TestModelForHds)model).getRecordType());
            }
        };
        
        hDs.registerRecordIndex(recordTypeIdx, recordTypeIdxValueExtractor );        
        recordIndexRegistry.getIndexMap(fileNamePrefix).put(recordTypeIdx, recordTypeIdxValueExtractor);
        recordIndexRegistry.getAllFileNamePrefixes().add(fileNamePrefix);
        
        StreamHolder streamHolder2 = new StreamHolder(streamFirstModelStartTimeMs + 100 , customerId, equipmentId, hDs);        
        streamHolder2.writeModelToStream(mdl3);
        streamHolder2.writeModelToStream(mdl4);
        streamHolder2.commitOutputStreamToFile();
        
        long streamLastModelTimeMs = streamFirstModelStartTimeMs + 200;
        
        //verify that 2 files were written to S3 by this time: 
        //  one data file before index was registered, 
        //  and one data file after index was registered, 
        //index itself is stored in hazelcast
        List<String> dataFileNames = hDs.getFileNames(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs);
        assertEquals(2, dataFileNames.size());
        
        assertTrue(dataFileNames.contains(streamHolder1.getFullFileName()));
        assertTrue(dataFileNames.contains(streamHolder2.getFullFileName()));

        assertFalse(checkIfHazelcastObjectExists("recIdx-" + hazelcastMapPrefix + recordTypeIdx,
                HierarchicalDatastore.getIndexFileName(streamHolder1.getFullFileName(), recordTypeIdx)));

        //second index should exist because the index was registered with s3ds at the time of writing the data file
        assertTrue(checkIfHazelcastObjectExists("recIdx-" + hazelcastMapPrefix + recordTypeIdx,
                HierarchicalDatastore.getIndexFileName(streamHolder2.getFullFileName(), recordTypeIdx)));

        EntryFilter<TestModelForHds> type1EntryFilter = new EntryFilter<TestModelForHds>() {
            @Override
            public TestModelForHds getFilteredEntry(TestModelForHds entry) {
                if(entry.getClient().equals(client1)){
                    return entry;
                }
                
                return null;
            }
        };

        //Build hourly index - should build one hourly index file, since now one index is registered with this datastore.
        HierarchicalDatastore.rebuildHourlyIndex(hourlyDirectoryName, dsRootDirName, dsPrefix, recordIndexRegistry, hazelcastInstance, hazelcastObjectsConfiguration);

        //verify that hourly index is present 
        fileNameToLastModMap = HierarchicalDatastore.getFileNamesAndLastMods(dsRootDirName, hourlyDirectoryName);
        assertEquals(3, fileNameToLastModMap.size());        
        assertTrue(fileNameToLastModMap.keySet().contains(streamHolder1.getFullFileName()));
        assertTrue(fileNameToLastModMap.keySet().contains(streamHolder2.getFullFileName()));
        assertTrue(fileNameToLastModMap.keySet().contains(recordTypeIdxHourlyIdxFileName));
        
        //check content of the hourly index file
        DirectoryIndex recordTypeIdxHourlyIdx = HierarchicalDatastore.getZippedModelFromFile(dsRootDirName,
                recordTypeIdxHourlyIdxFileName,  DirectoryIndex.class);
        assertNotNull(recordTypeIdxHourlyIdx);
        assertEquals(recordTypeIdx, recordTypeIdxHourlyIdx.getName());
        //there should be indexes created for 2 files
        assertEquals(2, recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().size());
        //first data file has 2 models, first one with type1, second one with type2
        assertEquals(2, recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getCounts().getTotalCount());
        assertEquals(0, (int) recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getPositions().getPositionsForValue(type1).get(0));
        assertEquals(1, (int) recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getPositions().getPositionsForValue(type2).get(0));
        //second data file has 2 models, first one with type1, second one with type2
        assertEquals(2, recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getCounts().getTotalCount());
        assertEquals(0, (int) recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getPositions().getPositionsForValue(type1).get(0));
        assertEquals(1, (int) recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getPositions().getPositionsForValue(type2).get(0));
        
        verifyAllOperations(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, mdl1, mdl2, mdl3, mdl4, recordTypeIdx, type1, type2, type1EntryFilter);


        //Test normal index operations when more than one index is present
        //register a second index and re-create hourly index file
        RecordIndexValueExtractor clientIdxValueExtractor = new RecordIndexValueExtractor() {
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                return Collections.singleton(((TestModelForHds)model).getClient());
            }
        };
        
        hDs.registerRecordIndex(clientIdx, clientIdxValueExtractor);
        recordIndexRegistry.getIndexMap(fileNamePrefix).put(clientIdx, clientIdxValueExtractor);


        //Build hourly index - should build two hourly index files, since now two indexes are registered with this datastore.
        HierarchicalDatastore.rebuildHourlyIndex(hourlyDirectoryName, dsRootDirName, dsPrefix, recordIndexRegistry, hazelcastInstance, hazelcastObjectsConfiguration);

        //verify first index file
        //verify that two hourly indexes are present 
        fileNameToLastModMap = HierarchicalDatastore.getFileNamesAndLastMods(dsRootDirName, hourlyDirectoryName);
        assertEquals(4, fileNameToLastModMap.size());        
        assertTrue(fileNameToLastModMap.keySet().contains(streamHolder1.getFullFileName()));
        assertTrue(fileNameToLastModMap.keySet().contains(streamHolder2.getFullFileName()));
        assertTrue(fileNameToLastModMap.keySet().contains(recordTypeIdxHourlyIdxFileName));
        assertTrue(fileNameToLastModMap.keySet().contains(clientIdxHourlyIdxFileName));
        
        //check content of the hourly index file for recordTypeIdx
        recordTypeIdxHourlyIdx = HierarchicalDatastore.getZippedModelFromFile(dsRootDirName,
                recordTypeIdxHourlyIdxFileName, DirectoryIndex.class);
        assertNotNull(recordTypeIdxHourlyIdx);
        assertEquals(recordTypeIdx, recordTypeIdxHourlyIdx.getName());
        //there should be indexes created for 2 files
        assertEquals(2, recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().size());
        //first data file has 2 models, first one with type1, second one with type2
        assertEquals(2, recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getCounts().getTotalCount());
        assertEquals(0, (int) recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getPositions().getPositionsForValue(type1).get(0));
        assertEquals(1, (int) recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getPositions().getPositionsForValue(type2).get(0));
        //second data file has 2 models, first one with type1, second one with type2
        assertEquals(2, recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getCounts().getTotalCount());
        assertEquals(0, (int) recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getPositions().getPositionsForValue(type1).get(0));
        assertEquals(1, (int) recordTypeIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getPositions().getPositionsForValue(type2).get(0));

        //check content of the hourly index file for clientIdx
        DirectoryIndex clientIdxHourlyIdx = HierarchicalDatastore.getZippedModelFromFile(dsRootDirName,
                clientIdxHourlyIdxFileName, DirectoryIndex.class);
        assertNotNull(clientIdxHourlyIdx);
        assertEquals(clientIdx, clientIdxHourlyIdx.getName());
        //there should be indexes created for 2 files
        assertEquals(2, clientIdxHourlyIdx.getDataFileNameToRecordIndexMap().size());
        //first data file has 2 models, first one with client1, second one with client2
        assertEquals(2, clientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getCounts().getTotalCount());
        assertEquals(0, (int) clientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getPositions().getPositionsForValue(client1).get(0));
        assertEquals(1, (int) clientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getPositions().getPositionsForValue(client2).get(0));
        //second data file has 2 models, first one with client1, second one with client2
        assertEquals(2, clientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getCounts().getTotalCount());
        assertEquals(0, (int) clientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getPositions().getPositionsForValue(client1).get(0));
        assertEquals(1, (int) clientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getPositions().getPositionsForValue(client2).get(0));
        
        
        //verify that two hourly indexes are present 
        fileNameToLastModMap = HierarchicalDatastore.getFileNamesAndLastMods(dsRootDirName, hourlyDirectoryName);
        assertEquals(4, fileNameToLastModMap.size());        
        assertTrue(fileNameToLastModMap.keySet().contains(streamHolder1.getFullFileName()));
        assertTrue(fileNameToLastModMap.keySet().contains(streamHolder2.getFullFileName()));
        assertTrue(fileNameToLastModMap.keySet().contains(recordTypeIdxHourlyIdxFileName));
        assertTrue(fileNameToLastModMap.keySet().contains(clientIdxHourlyIdxFileName));

        EntryFilter<TestModelForHds> client1EntryFilter = new EntryFilter<TestModelForHds>() {
                @Override
                public TestModelForHds getFilteredEntry(TestModelForHds entry) {
                    if(entry.getClient().equals(client1)){
                        return entry;
                    }
                    
                    return null;
                }
            };
            
        //verify operations with first index
        verifyAllOperations(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, mdl1, mdl2, mdl3, mdl4, recordTypeIdx, type1, type2, type1EntryFilter);
        
        //verify operations with second index
        //we can re-use verification logic because models with type1 values have client1 and models with type2 values have client2 
        verifyAllOperations(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, mdl1, mdl2, mdl3, mdl4, clientIdx, client1, client2, client1EntryFilter);

        //////
        // now check multi-valued indexes
        //////
        
        //register multi-valued index and write a datafile, with an index
        RecordIndexValueExtractor manyClientIdxValueExtractor = new RecordIndexValueExtractor() {
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                if(((TestModelForHds)model).getManyClients()==null || ((TestModelForHds)model).getManyClients().isEmpty()){
                    return Collections.singleton("");
                } 
                
                return new HashSet<>(((TestModelForHds)model).getManyClients());
            }
        };
        
        hDs.registerRecordIndex(manyClientIdx, manyClientIdxValueExtractor );        
        recordIndexRegistry.getIndexMap(fileNamePrefix).put(manyClientIdx, manyClientIdxValueExtractor);
        recordIndexRegistry.getAllFileNamePrefixes().add(fileNamePrefix);
        
        TestModelForHds mdl5 = new TestModelForHds(type3, client1, value3);
        mdl5.setManyClients(new HashSet<>( Arrays.asList(new String[]{"mc1","mc2","mc3"})));
        TestModelForHds mdl6 = new TestModelForHds(type4, client1, value3);
        mdl6.setManyClients(new HashSet<>( Arrays.asList(new String[]{"mc1","mc4","mc5"})));
        TestModelForHds mdl7 = new TestModelForHds(type5, client1, value3);
        mdl7.setManyClients(new HashSet<>( Arrays.asList(new String[]{})));

        
        streamFirstModelStartTimeMs += 120000 + 3000;
        StreamHolder streamHolder3 = new StreamHolder(streamFirstModelStartTimeMs, customerId, equipmentId, hDs);        
        streamHolder3.writeModelToStream(mdl5);
        streamHolder3.writeModelToStream(mdl6);
        streamHolder3.writeModelToStream(mdl7);
        streamHolder3.commitOutputStreamToFile();
        
        //update fileCreatedTimestampsForInterval in hazelcast - append new timestamp for just-uploaded-stream to it
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTime(new Date(streamHolder3.getZipStreamStartTimeMs()));
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        hDs.appendFileNameToDirectoryListing(customerId, equipmentId, year, month, day, hour, streamHolder3.getFullFileName());

        streamLastModelTimeMs = streamFirstModelStartTimeMs + 100;
        
        HierarchicalDatastore.rebuildHourlyIndex(hourlyDirectoryName, dsRootDirName, dsPrefix, recordIndexRegistry, hazelcastInstance, hazelcastObjectsConfiguration);

        //verify that no-index search returns 3 models
        List<TestModelForHds> entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , null, null /*no index*/, TestModelForHds.class);
        assertEquals(3 /*new models*/ , entryList.size());
        assertTrue(entryList.contains(mdl5));
        assertTrue(entryList.contains(mdl6));
        assertTrue(entryList.contains(mdl7));
        
        //verify that manyClientIdx search for "mc2" and "mc4" returns one model
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , 
                manyClientIdx, new HashSet<>(Arrays.asList("mc2")), TestModelForHds.class);
        assertEquals(1, entryList.size());
        assertTrue(entryList.contains(mdl5));

        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , 
                manyClientIdx, new HashSet<>(Arrays.asList("mc4")), TestModelForHds.class);
        assertEquals(1, entryList.size());
        assertTrue(entryList.contains(mdl6));

        //verify that manyClientIdx search for "mc1" returns two models
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , 
                manyClientIdx, new HashSet<>(Arrays.asList("mc1")), TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl5));
        assertTrue(entryList.contains(mdl6));
        
        //check content of the hourly index file for manyClientIdx
        HourlyIndexFileNames manyClientIdxHourlyIndexFileNames = new HourlyIndexFileNames(
                customerId, equipmentId, streamFirstModelStartTimeMs, streamFirstModelStartTimeMs+ 1000, 
                manyClientIdx, dsPrefix, fileNamePrefix, numberOfMinutesPerFile);
        String manyClientIdxHourlyIdxFileName = manyClientIdxHourlyIndexFileNames.iterator().next();

        DirectoryIndex manyClientIdxHourlyIdx = HierarchicalDatastore.getZippedModelFromFile(dsRootDirName,
                manyClientIdxHourlyIdxFileName, DirectoryIndex.class);
        assertNotNull(manyClientIdxHourlyIdx);
        assertEquals(manyClientIdx, manyClientIdxHourlyIdx.getName());
        
//      {"_type":"DirectoryIndex",
//      "name":"manyClient",
//      "dataFileNameToRecordIndexMap":{
//          "testDs/-1926954131/1492721664877/2017/04/20/18/testF_-1926954131_1492721664877_2017_04_20_18_00_1492721664880.zip":
//              {"_type":"RecordIndex",
//                  "counts":{"_type":"RecordIndexCounts","name":"manyClient","totalCount":2,"perValueCounts":{"":2}},
//                  "positions":{"_type":"RecordIndexPositions","name":"manyClient",
//                      "perValuePositions":{"":[0,1]}}},
//          "testDs/-1926954131/1492721664877/2017/04/20/18/testF_-1926954131_1492721664877_2017_04_20_18_02_1492721665391.zip":
//              {"_type":"RecordIndex",
//                  "counts":{"_type":"RecordIndexCounts","name":"manyClient","totalCount":7,"perValueCounts":{"":1,"mc1":2,"mc3":1,"mc2":1,"mc5":1,"mc4":1}},
//                  "positions":{"_type":"RecordIndexPositions","name":"manyClient","perValuePositions":{"":[2],"mc1":[0,1],"mc3":[0],"mc2":[0],"mc5":[1],"mc4":[1]}}},
//          "testDs/-1926954131/1492721664877/2017/04/20/18/testF_-1926954131_1492721664877_2017_04_20_18_00_1492721665101.zip":
//              {"_type":"RecordIndex",
//                  "counts":{"_type":"RecordIndexCounts","name":"manyClient","totalCount":2,"perValueCounts":{"":2}},
//                  "positions":{"_type":"RecordIndexPositions","name":"manyClient","perValuePositions":{"":[0,1]}}}}}
  
        
        //there should be indexes created for 3 files (streamHolder1, 2 and 3)
        assertEquals(3, manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().size());
        //data file for streamholder1 has 2 models
        assertEquals(2, manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getCounts().getTotalCount());
        assertEquals(0, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getPositions().getPositionsForValue("").get(0));
        assertEquals(1, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder1.getFullFileName()).getPositions().getPositionsForValue("").get(1));

        //data file for streamholder2 has 2 models
        assertEquals(2, manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getCounts().getTotalCount());
        assertEquals(0, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getPositions().getPositionsForValue("").get(0));
        assertEquals(1, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder2.getFullFileName()).getPositions().getPositionsForValue("").get(1));

        //data file for streamholder3 has 7 positions (mc1 is pointing to two positions) 
        assertEquals(7, manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder3.getFullFileName()).getCounts().getTotalCount());
        assertEquals(0, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder3.getFullFileName()).getPositions().getPositionsForValue("mc1").get(0));
        assertEquals(1, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder3.getFullFileName()).getPositions().getPositionsForValue("mc1").get(1));
        assertEquals(2, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder3.getFullFileName()).getPositions().getPositionsForValue("").get(0));
        assertEquals(0, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder3.getFullFileName()).getPositions().getPositionsForValue("mc2").get(0));
        assertEquals(0, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder3.getFullFileName()).getPositions().getPositionsForValue("mc3").get(0));
        assertEquals(1, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder3.getFullFileName()).getPositions().getPositionsForValue("mc4").get(0));
        assertEquals(1, (int) manyClientIdxHourlyIdx.getDataFileNameToRecordIndexMap().get(streamHolder3.getFullFileName()).getPositions().getPositionsForValue("mc5").get(0));
        
    }


    private boolean checkIfHazelcastObjectExists(String mapName, String mapKey) {
        IMap<String, byte[]> hcMap = hazelcastInstance.getMap(mapName);
        return hcMap==null?false:hcMap.containsKey(mapKey);
    }

    private void deleteHazelcastObject(String mapName, String mapKey) {
        IMap<String, byte[]> hcMap = hazelcastInstance.getMap(mapName);
        if(hcMap!=null){
            hcMap.delete(mapKey);
        }
    }

    private boolean checkIfS3ObjectExists(String s3Key) {
        File f = new File(dsRootDirName, s3Key);
        return f.exists();
    }
    
    private static final EntryFilter<TestModelForHds> matchAllEntryFilter = new EntryFilter<TestModelForHds>() {
        @Override
        public TestModelForHds getFilteredEntry(TestModelForHds entry) {
            return entry;
        }
    };

    private void verifyAllOperations(int customerId, long equipmentId, 
            long streamFirstModelStartTimeMs, long streamLastModelTimeMs, 
            TestModelForHds mdl1, TestModelForHds mdl2, TestModelForHds mdl3, TestModelForHds mdl4,
            String indexName,
            String idxValue1, String idxValue2, EntryFilter<TestModelForHds> matchType1EntryFilter) {
                
        
        //test read/count with null index name - results in full scan of the data files
        List<TestModelForHds> entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , null, null /*no index*/, TestModelForHds.class);
        assertEquals(4, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl2));
        assertTrue(entryList.contains(mdl3));
        assertTrue(entryList.contains(mdl4));
        
        //test read/count with not-null index name and null set of indexed values - results in full scan of the data files
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , indexName, null, TestModelForHds.class);
        assertEquals(4, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl2));
        assertTrue(entryList.contains(mdl3));
        assertTrue(entryList.contains(mdl4));
        
        //test read/count with not-null index name and empty set of indexed values - results in full scan of the data files
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , indexName, Collections.emptySet(), TestModelForHds.class);
        assertEquals(4, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl2));
        assertTrue(entryList.contains(mdl3));
        assertTrue(entryList.contains(mdl4));

        //test read/count with not-null index name and set of indexed values with one non-existent value 
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , 
                indexName, new HashSet<>(Arrays.asList("non-existing-type")), TestModelForHds.class);
        assertEquals(0, entryList.size());

        //test read/count with not-null index name and set of indexed values with one value - uses index 
        //indexed files will return only matching values - mdl1 and mdl3.
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , 
                indexName, new HashSet<>(Arrays.asList(idxValue1)), TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl3));
        
        //test read/count with not-null index name and set of indexed values with two values 
        //indexed files will return matching values - mdl1, mdl2, mdl3 and mdl4
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , 
                indexName, new HashSet<>(Arrays.asList(idxValue1, idxValue2)), TestModelForHds.class);
        assertEquals(4, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl2));
        assertTrue(entryList.contains(mdl3));
        assertTrue(entryList.contains(mdl4));

        
        //
        // Now repeat above tests with an entry filter that matches only specific records (of type type1/client1)
        //
        
        //test read/count with null index name - results in full scan of the data files
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchType1EntryFilter , null, null /*no index*/, TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl3));
        
        //test read/count with not-null index name and null set of indexed values - results in full scan of the data files
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchType1EntryFilter , indexName, null, TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl3));
        
        //test read/count with not-null index name and empty set of indexed values - results in full scan of the data files
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchType1EntryFilter , indexName, Collections.emptySet(), TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl3));

        //test read/count with not-null index name and set of indexed values with one non-existent value 
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchType1EntryFilter , 
                indexName, new HashSet<>(Arrays.asList("non-existing-type")), TestModelForHds.class);
        assertEquals(0, entryList.size());

        //test read/count with not-null index name and set of indexed values with one value
        //indexed file will return only matching values - mdl1 and mdl3.
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchType1EntryFilter , 
                indexName, new HashSet<>(Arrays.asList(idxValue1)), TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl3));
        
        //test read/count with not-null index name and set of indexed values with two values
        //indexed files will return matching values - mdl1, mdl2, mdl3 and mdl4, entry filter will only pass mdl1 and mdl3.
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchType1EntryFilter , 
                indexName, new HashSet<>(Arrays.asList(idxValue1, idxValue2)), TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl3));        
    }
    


    //TODO: Test invalid index operations - they all should regress into full scan of data files
    //TODO: create N data files in s3ds, 2 records each -
    //  empty index file
    //  index file with no record counts
    //  index file with no record positions
    //TODO: test read/count with null index name
    //TODO: test read/count with not-null index name and null set of indexed values
    //TODO: test read/count with not-null index name and empty set of indexed values
    //TODO: test read/count with not-null index name and set of indexed values with one value
    //TODO: test read/count with not-null index name and set of indexed values with two values
    //TODO: verify situation when index contains data, but data file is missing
    
       
    
}
