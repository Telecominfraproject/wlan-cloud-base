package com.telecominfraproject.wlan.hierarchical.datastore;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

import com.google.common.io.Files;
import com.hazelcast.core.HazelcastInstance;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.filter.EntryFilter;
import com.telecominfraproject.wlan.hazelcast.HazelcastForUnitTest;
import com.telecominfraproject.wlan.hazelcast.HazelcastForUnitTest.HazelcastUnitTestManager;
import com.telecominfraproject.wlan.hazelcast.common.HazelcastObjectsConfiguration;
import com.telecominfraproject.wlan.hierarchical.datastore.index.registry.RecordIndexRegistry;

/**
 * @author dtoptygin
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = HierarchicalDatastoreTests.class)
@Import(value = { 
        PropertySourcesPlaceholderConfigurer.class,
        HazelcastForUnitTest.class,
        RecordIndexRegistry.class,
        HazelcastObjectsConfiguration.class,
        })
@ActiveProfiles({"HazelcastForUnitTest"})
public class HierarchicalDatastoreTests {
    
    static{
        System.setProperty("tip.wlan.hdsExecutorQueueSize", "5000");
        System.setProperty("tip.wlan.hdsExecutorThreads", "10");
        System.setProperty("tip.wlan.hdsExecutorCoreThreadsFactor", "1");
        HazelcastUnitTestManager.initializeSystemProperty(HierarchicalDatastoreTests.class);
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
    private static final String dsPrefix = "testDs";
    String fileNamePrefix = "testF";
    String hazelcastMapPrefix = fileNamePrefix+"-";
    
    private HazelcastInstance hazelcastInstance;
    @Autowired RecordIndexRegistry recordIndexRegistry;
    @Autowired HazelcastObjectsConfiguration hazelcastObjectsConfiguration;
    
    //test with 1 minute per file in s3
    //TODO: test with 5, 15, 30, 60 (1hr), 240 (4 hrs), 1440 (24 hrs) - make sure tiered tables work with this
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

    @PostConstruct
    void initHds(){
    	//remove previous datastore content, if any
    	removeAllHdsFiles();
        
        hDs = new HierarchicalDatastore(dsRootDirName, dsPrefix, fileNamePrefix, 1, 20L, 
                hazelcastInstance, hazelcastMapPrefix, hazelcastObjectsConfiguration, recordIndexRegistry);
    }
    
    @AfterClass
    public static void removeAllHdsFiles(){
    	File rootDir = new File(dsRootDirName + File.separator + dsPrefix);
    	if(rootDir.getAbsolutePath().equals("/")) {
    		throw new IllegalArgumentException("attempting to delete / - please make sure your dsRootDirName and ds Prefix are not empty strings!");
    	}
    	
    	for(File f : Files.fileTreeTraverser().postOrderTraversal(rootDir)) {
    		f.delete();
    	}    	
    	
    	rootDir.delete();
    }   
    
    @Test
    public void testGetFileNames() throws IOException, InterruptedException {
        
        Calendar fromC = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar toC_8hrs = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar toC_15min = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar toC_1day = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar toC_1month = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        
        //init calendars
        fromC.set(      2015, 10,  9,  7,  4);
        toC_8hrs.set(   2015, 10,  9, 15,  4);
        toC_15min.set(  2015, 10,  9,  7, 19);
        toC_1day.set(   2015, 10, 10,  7,  4);
        toC_1month.set( 2015, 11,  9,  7,  4);
        
        int customerId = 42;
        long equipmentId = 314;

        //pre-create files in hDs - in the range (fromC, toC_1month), without this getFileNames has nothing to work with
        Calendar cal = (Calendar) fromC.clone();
        int numItems = 5; //number of items to create in each zipped file entry
        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicInteger numFilesCreated = new AtomicInteger();
        
        //we'll use the same content in all the files, as we're not interested in it - only care about file names here
        String fileNameFirstFile = hDs.getFileNameForNewFile(customerId, equipmentId, cal.getTimeInMillis());
        byte[] zippedBytes;
        try {
            zippedBytes = createZippedFileBytes(fileNameFirstFile.substring(fileNameFirstFile.lastIndexOf('/')+1, fileNameFirstFile.length()-4), numItems);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //use this for month-long range, which makes the test run for 129 sec
        //while(cal.before(toC_1month) || cal.equals(toC_1month)){
        
        //use 1 day range for the tests 
        while(cal.before(toC_1day) || cal.equals(toC_1day)){
            final long calTime = cal.getTimeInMillis() + 15; //distort nicely aligned timestamps, to see that they are normalized back
            String fileName = hDs.getFileNameForNewFile(customerId, equipmentId, calTime);
            
            futures.add(executor.submit(new Callable<Boolean>(){
                @Override
                    public Boolean call() {
                        InputStream inputStream = new ByteArrayInputStream(zippedBytes);
                        hDs.uploadStreamToFileOverwriteOld(inputStream, zippedBytes.length, fileName);
                        
                        numFilesCreated.incrementAndGet();
                        return true;
                    }
            }));
            cal.add(Calendar.MINUTE, hDs.getNumberOfMinutesPerFile());
        }

        //wait until all files are created in S3
        for(Future<Boolean> f: futures){
            try {
                f.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        
        Thread.sleep(100);
        
        //Now tests can begin
        
        //frequent case - 15 minutes worth of data
        List<String> fileNames = hDs.getFileNames(42, 314, fromC.getTimeInMillis(), toC_15min.getTimeInMillis()); //built 16 files, took 84 ms
        assertEquals(16, fileNames.size());
        Collections.sort(fileNames);
        assertEquals("testDs/42/314/2015/11/09/07/testF_42_314_2015_11_09_07_04", truncateCreatedTimestamp(fileNames.get(0)));
        assertEquals("testDs/42/314/2015/11/09/07/testF_42_314_2015_11_09_07_19", truncateCreatedTimestamp(fileNames.get(fileNames.size()-1)));

        //regular case - 8 hrs worth of data: from < to time 
        fileNames = hDs.getFileNames(42, 314, fromC.getTimeInMillis(), toC_8hrs.getTimeInMillis()); //built 481 files, took 131 ms
        assertEquals(481, fileNames.size());
        Collections.sort(fileNames);
        assertEquals("testDs/42/314/2015/11/09/07/testF_42_314_2015_11_09_07_04", truncateCreatedTimestamp(fileNames.get(0)));
        assertEquals("testDs/42/314/2015/11/09/15/testF_42_314_2015_11_09_15_04", truncateCreatedTimestamp(fileNames.get(fileNames.size()-1)));

        //ok case - a day worth of data
        fileNames = hDs.getFileNames(42, 314, fromC.getTimeInMillis(), toC_1day.getTimeInMillis()); //built 1441 files, took 271 ms
        assertEquals(1441, fileNames.size());
        Collections.sort(fileNames);
        assertEquals("testDs/42/314/2015/11/09/07/testF_42_314_2015_11_09_07_04", truncateCreatedTimestamp(fileNames.get(0)));
        assertEquals("testDs/42/314/2015/11/10/07/testF_42_314_2015_11_10_07_04", truncateCreatedTimestamp(fileNames.get(fileNames.size()-1)));

//        //tight case - a month worth of data <- should probably draw a line here
//        fileNames = hDs.getFileNames(42, 314, fromC.getTimeInMillis(), toC_1month.getTimeInMillis()); //built 43201 files, took 69443 ms
//        assertEquals(43201, fileNames.size());
//        assertEquals("testDs/42/314/2015/11/09/07/testF_42_314_2015_11_09_07_04", truncateCreatedTimestamp(fileNames.get(0)));
//        assertEquals("testDs/42/314/2015/12/09/07/testF_42_314_2015_12_09_07_04", truncateCreatedTimestamp(fileNames.get(fileNames.size()-1)));
//
//        //repeat the call, now it should completely come from hazelcast, because the data should have been cached
//        fileNames = hDs.getFileNames(42, 314, fromC.getTimeInMillis(), toC_1month.getTimeInMillis()); //built 43201 files, took 875 ms
//        assertEquals(43201, fileNames.size());

//        //extreme case - a year worth of data
//        toC.set(2016, 10, 9, 7, 4);
//        fileNames = hDs.getFileNames(42, 314, fromC.getTimeInMillis(), toC.getTimeInMillis());
//        assertEquals(527041, fileNames.size());
//        assertEquals("testDs/42/314/2015/11/09/07/testF_42_314_2015_11_09_07_04", truncateCreatedTimestamp(fileNames.get(0)));
//        assertEquals("testDs/42/314/2016/11/09/07/testF_42_314_2016_11_09_07_04", truncateCreatedTimestamp(fileNames.get(fileNames.size()-1)));
        
        //same from and to times should result in 1 file name
        fileNames = hDs.getFileNames(42, 314, fromC.getTimeInMillis(), fromC.getTimeInMillis()); //built 1 files, took 0 ms
        assertEquals(1, fileNames.size());
        assertEquals("testDs/42/314/2015/11/09/07/testF_42_314_2015_11_09_07_04", truncateCreatedTimestamp(fileNames.get(0)));

        //from > to time should result in empty file names list
        fileNames = hDs.getFileNames(42, 314, fromC.getTimeInMillis() + 1, fromC.getTimeInMillis()); //built 0 files
        assertEquals(0, fileNames.size());
        
    }
    
    private String truncateCreatedTimestamp(String fileName){
        return fileName.substring(0, fileName.lastIndexOf('_'));
    }
    
    @Test
    public void testReadFile() throws Exception {
        //<T> List<T> getContent(InputStream inputStream, EntryFilter<T> entryFilter);
        
        //create test stream of zipped MacAddress json objects, one per line
        int numItems = 10;

        //now test the getContent() method on that created stream
        List<MacAddress> result = HierarchicalDatastore.getContent(
                new ByteArrayInputStream(createZippedFileBytes("testF_42_314_2015_10_09_07_04", numItems)), 
                new EntryFilter<MacAddress>(){
                    @Override
                    public MacAddress getFilteredEntry(MacAddress r) {
                        return r;
                    }
        
                }, null /*do full scan*/, MacAddress.class);

        assertEquals(numItems, result.size());
        assertEquals(0L, (long) result.get(0).getAddressAsLong());
        assertEquals((long) (numItems - 1), (long) result.get(result.size() - 1).getAddressAsLong());
    }
    
    /**
     * @param partFileName - file name for the entry within zipped file, usually the same as the name of the zip file without the .gz extension
     * @param numItems - number of JSON models to create in the zipped file entry
     * @return bytes, representing the zipped file contents, to be used as { new ByteArrayInputStream(createZippedFileBytes("testF_42_314_2015_10_09_07_04", numItems)) }
     * @throws IOException
     */
    private byte[] createZippedFileBytes(String partFileName, int numItems) throws IOException{
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        ZipEntry entry = new ZipEntry(partFileName);
        zos.putNextEntry(entry);
        
        MacAddress macAddress; 

        for(int i =0; i<numItems; i++){
            macAddress = new MacAddress((long)i);
            
            byte[] itemBytes = macAddress.toString().getBytes(StandardCharsets.UTF_8); 
            zos.write(itemBytes);
            zos.write(13);
            zos.write(10);
        }

        zos.closeEntry();
        zos.flush();
        zos.close();

        outputStream.flush();
        outputStream.close();
        
        return outputStream.toByteArray();
    }
    
}
