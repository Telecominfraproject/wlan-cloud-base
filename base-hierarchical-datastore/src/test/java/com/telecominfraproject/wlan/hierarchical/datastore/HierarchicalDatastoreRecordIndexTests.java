package com.telecominfraproject.wlan.hierarchical.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;

import org.junit.AfterClass;
import org.junit.Ignore;
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
import com.hazelcast.map.IMap;
import com.telecominfraproject.wlan.core.model.filter.EntryFilter;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.hazelcast.HazelcastForUnitTest;
import com.telecominfraproject.wlan.hazelcast.HazelcastForUnitTest.HazelcastUnitTestManager;
import com.telecominfraproject.wlan.hazelcast.common.HazelcastObjectsConfiguration;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexValueExtractor;
import com.telecominfraproject.wlan.hierarchical.datastore.index.registry.RecordIndexRegistry;
import com.telecominfraproject.wlan.hierarchical.datastore.writer.StreamHolder;

/**
 * @author dtoptygin
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = HierarchicalDatastoreRecordIndexTests.class)
@Import(value = { 
        HazelcastForUnitTest.class,
        RecordIndexRegistry.class,
        HazelcastObjectsConfiguration.class,
        PropertySourcesPlaceholderConfigurer.class, //must have this to resolve non-string @Value annotations, i.e. int properties, etc.
        })
@ActiveProfiles({"HazelcastForUnitTest"})
@Ignore("DT: these compoinents are not used for now, re-enable the tests if and when they are back in the system")
public class HierarchicalDatastoreRecordIndexTests {
    
    static{
        System.setProperty("tip.wlan.hdsExecutorQueueSize", "5000");
        System.setProperty("tip.wlan.hdsExecutorThreads", "10");
        System.setProperty("tip.wlan.hdsExecutorCoreThreadsFactor", "1");
        HazelcastUnitTestManager.initializeSystemProperty(HierarchicalDatastoreRecordIndexTests.class);
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
    private static final String dsPrefix = "testDsRIT";

    String fileNamePrefix = "testF";
    String hazelcastMapPrefix = fileNamePrefix+"-";
    String hdsCreationTimestampFileMapPrefix = "hdsCreationTs-";
       
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

        hDs = new HierarchicalDatastore(dsRootDirName, dsPrefix, fileNamePrefix, 1, 20L, 
                hazelcastInstance, hazelcastMapPrefix, hazelcastObjectsConfiguration, recordIndexRegistry);
    }
    
    
    private final static String recordTypeIdx = "recordType";
    private final static String clientIdx = "client";

    @Test
    @Ignore("TODO: dtop - rewrite this test so it does not depend on currentTimeMillis()")
    public void testNormalOneIndex() throws IOException{

        //create 2 data files in s3ds, 2 records each - one with record index, one without any indexes

        String type1 = "t1";
        String type2 = "t2";
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
        long streamFirstModelStartTimeMs = System.currentTimeMillis();

        //first write a datafile when no indexes are present
        StreamHolder streamHolder1 = new StreamHolder(streamFirstModelStartTimeMs , customerId, equipmentId, hDs);
        streamHolder1.writeModelToStream(mdl1);
        streamHolder1.writeModelToStream(mdl2);
        streamHolder1.commitOutputStreamToFile();
        
        //now register an index and write a second datafile, with an index
        hDs.registerRecordIndex(recordTypeIdx, new RecordIndexValueExtractor() {
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                return Collections.singleton(((TestModelForHds)model).getRecordType());
            }
        });
        
        StreamHolder streamHolder2 = new StreamHolder(streamFirstModelStartTimeMs + 100 , customerId, equipmentId, hDs);
        streamHolder2.writeModelToStream(mdl3);
        streamHolder2.writeModelToStream(mdl4);
        streamHolder2.commitOutputStreamToFile();
        
        long streamLastModelTimeMs = System.currentTimeMillis();
        if(streamLastModelTimeMs <= streamFirstModelStartTimeMs){
            streamLastModelTimeMs = streamFirstModelStartTimeMs + 200;
        }
        
        //verify that 3 files were written by this time: one data file without index, and one data file with an index
        List<String> dataFileNames = hDs.getFileNames(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs);
        assertEquals(2, dataFileNames.size());
        
        assertTrue(dataFileNames.contains(streamHolder1.getFullFileName()));
        assertTrue(dataFileNames.contains(streamHolder2.getFullFileName()));

        assertFalse(checkIfHazelcastObjectExists("recIdx-" + hazelcastMapPrefix + recordTypeIdx ,
                HierarchicalDatastore.getIndexFileName(streamHolder1.getFullFileName(), recordTypeIdx)));

        //second index should exist because the index was registered with s3ds at the time of writing the data file
        assertTrue(checkIfHazelcastObjectExists("recIdx-"+ hazelcastMapPrefix + recordTypeIdx ,
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

        verifyAllOperations(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, mdl1, mdl2, mdl3, mdl4, recordTypeIdx, type1, type2, type1EntryFilter);
        
        //Test normal index operations when more than one index is present
        //We will remove files with indexes, register two indexes in s3ds, create one more data file with 2 index files, and verify all operations again
        File f = new File(hDs.getDsRootDirName(), streamHolder2.getFullFileName());
        f.delete();
        deleteHazelcastObject("recIdx-"+ hazelcastMapPrefix + recordTypeIdx , HierarchicalDatastore.getIndexFileName(streamHolder2.getFullFileName(), recordTypeIdx));
        assertFalse(checkIfS3ObjectExists(streamHolder2.getFullFileName()));
        assertFalse(checkIfHazelcastObjectExists("recIdx-" + hazelcastMapPrefix + recordTypeIdx ,
                HierarchicalDatastore.getIndexFileName(streamHolder2.getFullFileName(), recordTypeIdx)));
        
        //remove cached filename list from hazelcast
        String tsMapName = hdsCreationTimestampFileMapPrefix + fileNamePrefix+"-";
        IMap<String, List<Long>> tsMap = hazelcastInstance.getMap(tsMapName);
        tsMap.clear();
        
        IMap<String, Set<String>> dirListMap = hazelcastInstance.getMap(hazelcastObjectsConfiguration.getHdsDirectoryListingsMapName());
        dirListMap.clear();


        //now register a second index and write a third datafile, with two indexes
        hDs.registerRecordIndex(clientIdx, new RecordIndexValueExtractor() {
            @Override
            public Set<String> extractValues(BaseJsonModel model) {
                return Collections.singleton(((TestModelForHds)model).getClient());
            }
        });

        StreamHolder streamHolder3 = new StreamHolder(streamFirstModelStartTimeMs + 100 , customerId, equipmentId, hDs);        
        streamHolder3.writeModelToStream(mdl3);
        streamHolder3.writeModelToStream(mdl4);
        streamHolder3.commitOutputStreamToFile();
        
        streamLastModelTimeMs = System.currentTimeMillis();
        if(streamLastModelTimeMs <= streamFirstModelStartTimeMs){
            streamLastModelTimeMs = streamFirstModelStartTimeMs + 200;
        }

        //verify that 4 files were written by this time: one data file without index, and one data file with two indexes
        dataFileNames = hDs.getFileNames(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs);
        assertEquals(2, dataFileNames.size());
        
        assertTrue(dataFileNames.contains(streamHolder1.getFullFileName()));
        assertTrue(dataFileNames.contains(streamHolder3.getFullFileName()));

        assertFalse(checkIfHazelcastObjectExists("recIdx-"+ hazelcastMapPrefix + recordTypeIdx ,
                HierarchicalDatastore.getIndexFileName(streamHolder1.getFullFileName(), recordTypeIdx)));

        //second index should exist because the index was registered with s3ds at the time of writing the data file
        assertTrue(checkIfHazelcastObjectExists("recIdx-" + hazelcastMapPrefix + recordTypeIdx ,
                HierarchicalDatastore.getIndexFileName(streamHolder3.getFullFileName(), recordTypeIdx)));
        assertTrue(checkIfHazelcastObjectExists("recIdx-"+ hazelcastMapPrefix + clientIdx,
                HierarchicalDatastore.getIndexFileName(streamHolder3.getFullFileName(), clientIdx)));

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
        File f = new File(hDs.getDsRootDirName(), s3Key);
        return f.exists();
    }

    
    private void verifyAllOperations(int customerId, long equipmentId, 
            long streamFirstModelStartTimeMs, long streamLastModelTimeMs, 
            TestModelForHds mdl1, TestModelForHds mdl2, TestModelForHds mdl3, TestModelForHds mdl4,
            String indexName,
            String idxValue1, String idxValue2, EntryFilter<TestModelForHds> matchType1EntryFilter) {
        
        EntryFilter<TestModelForHds> matchAllEntryFilter = new EntryFilter<TestModelForHds>() {
            @Override
            public TestModelForHds getFilteredEntry(TestModelForHds entry) {
                return entry;
            }
        };
        
        
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

        //test read/count with not-null index name and set of indexed values with one non-existent value - results in full scan for the data files that do not have indexes, and uses index when possible
        //in our case not-indexed file will return all its data - mdl1 and mdl2, and indexed file will be skipped.
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , 
                indexName, new HashSet<>(Arrays.asList("non-existing-type")), TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl2));

        //test read/count with not-null index name and set of indexed values with one value - results in full scan for the data files that do not have indexes, and uses index when possible
        //in our case not-indexed file will return all its data - mdl1 and mdl2, and indexed file will return only one matching value - mdl3.
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchAllEntryFilter , 
                indexName, new HashSet<>(Arrays.asList(idxValue1)), TestModelForHds.class);
        assertEquals(3, entryList.size());
        //System.err.println(entryList);
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl2));
        assertTrue(entryList.contains(mdl3));
        
        //test read/count with not-null index name and set of indexed values with two values - results in full scan for the data files that do not have indexes, and uses index when possible
        //in our case not-indexed file will return all its data - mdl1 and mdl2, and indexed file will return matching values - mdl3 and mdl4.
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

        //test read/count with not-null index name and set of indexed values with one non-existent value - results in full scan for the data files that do not have indexes, and uses index when possible
        //in our case not-indexed file will return all its matching data - mdl1, and indexed file will be skipped.
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchType1EntryFilter , 
                indexName, new HashSet<>(Arrays.asList("non-existing-type")), TestModelForHds.class);
        assertEquals(1, entryList.size());
        assertTrue(entryList.contains(mdl1));

        //test read/count with not-null index name and set of indexed values with one value - results in full scan for the data files that do not have indexes, and uses index when possible
        //in our case not-indexed file will return all its matching data - mdl1, and indexed file will return only one matching value - mdl3.
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchType1EntryFilter , 
                indexName, new HashSet<>(Arrays.asList(idxValue1)), TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl3));
        
        //test read/count with not-null index name and set of indexed values with two values - results in full scan for the data files that do not have indexes, and uses index when possible
        //in our case not-indexed file will return all its matching data - mdl1; and indexed file will return matching values - mdl3 and mdl4, entry filter will only pass mdl3.
        entryList = hDs.getEntries(customerId, equipmentId, streamFirstModelStartTimeMs, streamLastModelTimeMs, matchType1EntryFilter , 
                indexName, new HashSet<>(Arrays.asList(idxValue1, idxValue2)), TestModelForHds.class);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(mdl1));
        assertTrue(entryList.contains(mdl3));        
    }
    


    //TODO: Test missing/invalid index operations - they all should regress into full scan of data files
    //TODO: create N data files in s3ds, 2 records each -
    //  empty index file
    //  index file with no record counts
    //  index file with no record positions
    //TODO: test read/count with null index name
    //TODO: test read/count with not-null index name and null set of indexed values
    //TODO: test read/count with not-null index name and empty set of indexed values
    //TODO: test read/count with not-null index name and set of indexed values with one value
    //TODO: test read/count with not-null index name and set of indexed values with two values
    
       
    
}
