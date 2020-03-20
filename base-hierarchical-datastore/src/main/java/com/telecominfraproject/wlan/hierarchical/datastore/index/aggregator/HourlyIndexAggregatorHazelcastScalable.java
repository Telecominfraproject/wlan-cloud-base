package com.telecominfraproject.wlan.hierarchical.datastore.index.aggregator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import com.hazelcast.core.HazelcastInstance;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.hazelcast.HazelcastForUnitTest;
import com.telecominfraproject.wlan.hazelcast.common.HazelcastObjectsConfiguration;
import com.telecominfraproject.wlan.hierarchical.datastore.HourlyIndexFileNames;
import com.telecominfraproject.wlan.hierarchical.datastore.HierarchicalDatastore;
import com.telecominfraproject.wlan.hierarchical.datastore.index.DirectoryIndex;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndex;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexCounts;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexPositions;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexValueExtractor;
import com.telecominfraproject.wlan.hierarchical.datastore.index.registry.RecordIndexRegistry;
import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

/**
 * This class produces a single hourly directory index file for a given 
 * <pre>
 *  &lt;dsRootDirName, customer, equipment, RecordIndexRegistry, year, month, day, hour>
 * </pre>
 * It performs the following steps:
 * <ol>
 * <li>compute directory name from the given parameters
 * <li>get list of all files in that directory
 * <li>get content of all hourly index files in that directory
 * <li>check if oldest creation timestamp of the data files is older than the creation timestamp of the hourly index file
 * <li> if not ( a data file was created after the hourly index file, or if hourly index file is missing) - then re-build and overwrite hourly index file 
 * <ol>
 * <li>for every data file that does not have all the record indexes - build missing record index files, store them in hazelcast. 
 *      Make sure to NOT accumulate content of the data files in memory. 
 * <li>build hourly index file by combining the content of all record index files in the directory 
 * <li>store hourly index file in the same directory in zipped format 
 * </ol>
 * </ol> 
 * 
 * @author dtop
 *
 */
public class HourlyIndexAggregatorHazelcastScalable {
    
    private static final Logger LOG = LoggerFactory.getLogger(HourlyIndexAggregatorHazelcastScalable.class);
        
    private final String dsRootDirName;
    private final String dsPrefix;
    private final RecordIndexRegistry recordIndexRegistry;
    private final HazelcastInstance hazelcastClient;
    private final HazelcastObjectsConfiguration hazelcastObjectsConfiguration;
    
    public HourlyIndexAggregatorHazelcastScalable(String dsRootDirName, String dsPrefix, RecordIndexRegistry recordIndexRegistry, 
            HazelcastInstance hazelcastClient, HazelcastObjectsConfiguration hazelcastObjectsConfiguration) {
        this.dsRootDirName = dsRootDirName;
        this.dsPrefix = dsPrefix;
        this.recordIndexRegistry = recordIndexRegistry;
        this.hazelcastClient = hazelcastClient;
        this.hazelcastObjectsConfiguration = hazelcastObjectsConfiguration;
    }
    
    /**
     * build hourly index for customer/equipment for all hours that fall within supplied time range [timeFromMs, timeToMs]
     * @param customerId
     * @param equipmentId
     * @param fromTimeMs
     * @param toTimeMs
     */
    public void buildHourlyIndex(int customerId, long equipmentId, long fromTimeMs, long toTimeMs){

        LOG.debug("started buildHourlyIndex({}, {}, {}, {})", customerId, equipmentId, fromTimeMs, toTimeMs);

        //if toTime is in the future - set it back to 2 hours ago
        long currentTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2);
                
        if(toTimeMs>currentTime){
            toTimeMs = currentTime;
        }
        
        //adjust fromTime/toTime so they are on a boundary of GMT hour
        fromTimeMs = fromTimeMs - fromTimeMs%(TimeUnit.HOURS.toMillis(1)); 
        toTimeMs = toTimeMs - toTimeMs%(TimeUnit.HOURS.toMillis(1)); 
        
        Calendar fromCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        fromCalendar.setTime(new Date(fromTimeMs));
        
        Calendar toCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        toCalendar.setTime(new Date(toTimeMs));

        //generate list of directories based on supplied criteria
        while(fromCalendar.before(toCalendar) || fromCalendar.equals(toCalendar)){

            final int year = fromCalendar.get(Calendar.YEAR);
            final int month = fromCalendar.get(Calendar.MONTH) + 1;
            final int day = fromCalendar.get(Calendar.DAY_OF_MONTH);
            final int hour = fromCalendar.get(Calendar.HOUR_OF_DAY);
            
            buildHourlyIndexForSingleHour(customerId, equipmentId, year, month, day, hour);
            
            //advance time to get directory for the next hour
            fromCalendar.add(Calendar.HOUR_OF_DAY, 1);
        }
        
        LOG.debug("completed buildHourlyIndex({}, {}, {}, {})", customerId, equipmentId, fromTimeMs, toTimeMs);
    }
    
    public void buildHourlyIndexForSingleHour(int customerId, long equipmentId, int year, int month, int day, int hour){
        LOG.info("started buildHourlyIndexForSingleHour({}, {}, {}, {}, {}, {})",  customerId, equipmentId, year, month, day, hour);

        //123wlan-datastore-us-east-1/dev1/13/834/2016/12/03/04/
        StringBuilder sb = new StringBuilder(1024);
        Formatter formatter = new Formatter(sb, null);
        formatter.format("%s/%d/%d/%4d/%02d/%02d/%02d/",
                dsPrefix, customerId, equipmentId, year, month, day, hour
                );
        formatter.close();
        
        String dirKey = sb.toString();
        
        StringBuilder sb1 = new StringBuilder(256);
        Formatter formatter1 = new Formatter(sb1, null);
        formatter1.format("_%d_%d_%4d_%02d_%02d_%02d",
                customerId, equipmentId, year, month, day, hour
                );
        formatter1.close();
        
        String hourlyIndexSuffix = sb1.toString();
        
        Map<String,Long> fileNameToLastModMap = HierarchicalDatastore.getFileNamesAndLastMods(dsRootDirName, dirKey);
        Set<String> dataFilesPrefixes = recordIndexRegistry.getAllFileNamePrefixes();
        Set<String> dataFilesPrefixesInUse = new HashSet<>();
        
        Map<String, Set<String>> dataFilePrefixToNamesOfDataFilesMap = new HashMap<>();
        Set<String> tmpNamesOfDataFiles;
        String shortFileName;        
        
        for(String fName: fileNameToLastModMap.keySet()){
            //System.out.println(fName);
            shortFileName = fName.substring(fName.lastIndexOf('/')+1);
            for(String prefixName : dataFilesPrefixes){
                if(shortFileName.startsWith(prefixName)){
                    tmpNamesOfDataFiles = dataFilePrefixToNamesOfDataFilesMap.get(prefixName);
                    if(tmpNamesOfDataFiles==null){
                        tmpNamesOfDataFiles = new HashSet<>();
                        dataFilePrefixToNamesOfDataFilesMap.put(prefixName, tmpNamesOfDataFiles);
                    }
                    tmpNamesOfDataFiles.add(fName);
                    dataFilesPrefixesInUse.add(prefixName);
                    break;
                }
            }
            
        }
        
        //now process each dataFilePrefix separately - to make sure hourly index built for them is up to date
        for(String fNamePrefix: dataFilesPrefixesInUse){
            buildHourlyIndexesForSingleFileNamePrefix(dirKey, fNamePrefix, hourlyIndexSuffix,  
                        dataFilePrefixToNamesOfDataFilesMap.get(fNamePrefix), fileNameToLastModMap);
        }
                
        LOG.info("completed buildHourlyIndexForSingleHour({}, {}, {}, {}, {}, {})",  customerId, equipmentId, year, month, day, hour);

    }
    
    /**
     * Convenience class that keeps track of DirectoryIndex, its indexName, hourlyIdxFileName, hrIndexPrefix, and other metadata 
     * @author dtop
     *
     */
    private static class InternalDirectoryIndex{
        String indexName;
        String hourlyIdxFileName;
        String hrIndexPrefix;
        DirectoryIndex hourlyIdx;
        long hourlyIdxFileLastmod = 0;
        boolean needsToBeStored = false;
    }
    

    /**
     * @param directory - what directory to process for the hourly index file
     * @param fNamePrefix - file name prefix for the data files to process
     * @param hourlyIndexSuffix - in the form "_%d_%d_%4d_%02d_%02d_%02d"
     * @param namesOfDataFiles - subset of all file names as read from directory, all having fNamePrefix
     * @param fileNameToLastModMap - file names and their lastmod timestamps read from the directory 
     */
    public void buildHourlyIndexesForSingleFileNamePrefix(String directory, String fNamePrefix, String hourlyIndexSuffix,
            Set<String> namesOfDataFiles,
            Map<String,Long> fileNameToLastModMap){

        LOG.debug("started buildHourlyIndexForSingleFileNamePrefix({})", fNamePrefix);

        Set<String> indexNamesToProcess = recordIndexRegistry.getAllIndexesForFileNamePrefix(fNamePrefix);
        List<InternalDirectoryIndex> internalDirectoryIndexes = new ArrayList<>(indexNamesToProcess.size());

        //populate InternalDirectoryIndex-es for current fileNamePrefix
        
        for(String indexName: indexNamesToProcess){
            InternalDirectoryIndex internalDirIdx = new InternalDirectoryIndex();
            internalDirectoryIndexes.add(internalDirIdx);
            internalDirIdx.indexName = indexName;
            
            //find the name of the hourly index file, if it is present
            internalDirIdx.hrIndexPrefix = HourlyIndexFileNames.hourlyIndexFileNamePrefix+indexName+"_"+fNamePrefix;
            for(String fName: fileNameToLastModMap.keySet()){
                if(fName.substring(fName.lastIndexOf('/')+1).startsWith(internalDirIdx.hrIndexPrefix)){
                    internalDirIdx.hourlyIdxFileName = fName;
                    break;
                }            
            }
            
            //If hourly idx file exists then we need to check if there are any existing index files 
            //  or data files that have lastMod after the hourly index was created
            //  if they are present, then hourly index needs to be rebuilt.
            //We are covering the following cases in here:
            //  1. hourly index was initially created, but later new data file 
            //      appeared in the same directory (with or without its own index file)
            //  2. hourly index was initially created, later all record index files were 
            //      deleted (because we only need hourly index after it is built), and 
            //      later a new data file appeared in the same directory (with or 
            //      without its own index file)
            // In all of these cases
            //      we need to get (or build) the record index file for the new data file, 
            //      and add it into the hourly index. And then save the hourly index with all the changes.
    
            if(internalDirIdx.hourlyIdxFileName!=null){
                //retrieve existing hourly index file
                LOG.debug("hourly index found : {}", internalDirIdx.hourlyIdxFileName);
                internalDirIdx.hourlyIdx = HierarchicalDatastore.getZippedModelFromFile(dsRootDirName, internalDirIdx.hourlyIdxFileName, DirectoryIndex.class);
                internalDirIdx.hourlyIdxFileLastmod = fileNameToLastModMap.get(internalDirIdx.hourlyIdxFileName);
            } else {
                internalDirIdx.hourlyIdxFileName = directory + internalDirIdx.hrIndexPrefix + hourlyIndexSuffix+".zip";
            } 
            
            if(internalDirIdx.hourlyIdx == null){
                LOG.debug("hourly index NOT found : {}", internalDirIdx.hourlyIdxFileName);
                internalDirIdx.hourlyIdx = new DirectoryIndex();
                internalDirIdx.hourlyIdx.setName(indexName);
                internalDirIdx.needsToBeStored = true;
            }
            
        }
        
        //Summary of the logic below
        // Go through each datafile with fNamePrefix:
        // + if a record index object for that datafile is present in hourlyIdx, 
        //      and lastmod of that data file is older (less than) than hourlyIdxFileLastmod
        //      then no action is required
        // + else if a record index object for that datafile is NOT present in hourlyIdx,
        //      then build and merge that record index object into hourlyIdx, mark hourlyIdx as needsToBeStored
        // + else if a record index object for that datafile is present in hourlyIdx, 
        //      and lastmod of that data file is newer (greater than) than hourlyIdxFileLastmod
        //      then build and replace that record index object into hourlyIdx, mark hourlyIdx as needsToBeStored
                
        RecordIndex recordIndex;
        Long dataFileLastMod;
        boolean hourlyIdxContainsIndex;
        for(String dataFileName: namesOfDataFiles){

            //determine what indexes need to be re-built for a given datafile
            List<InternalDirectoryIndex> indexesToRebuild = new ArrayList<>();
            for(InternalDirectoryIndex internalIdx: internalDirectoryIndexes){
                
                dataFileLastMod = fileNameToLastModMap.get(dataFileName);
                hourlyIdxContainsIndex = internalIdx.hourlyIdx.getDataFileNameToRecordIndexMap().get(dataFileName) != null;
                if(hourlyIdxContainsIndex){
                    //data file already present in the hourly index
                    if(dataFileLastMod < internalIdx.hourlyIdxFileLastmod) {
                        //nothing to do here, all up-to-date
                    } else {
                        // merge/replace record index for that new data file into hourlyIdx
                        internalIdx.needsToBeStored = true;
                        recordIndex = HierarchicalDatastore.findRecordIndex(hazelcastObjectsConfiguration.getRecordIndexMapPrefix(), 
                                fNamePrefix+"-", hazelcastClient, internalIdx.indexName, dataFileName);
                        
                        if(recordIndex==null){
                            //record index NOT found in hazelcast - possibly expired
                            //we'll build on the fly
                            LOG.debug("Could not get content of record index {} for data file {} - building record index from scratch", internalIdx.indexName, dataFileName);
                            indexesToRebuild.add(internalIdx);
                        } else {
                            //record index found in hazelcast, will use it in directory index
                            if(recordIndex!=null && recordIndex.getCounts()!=null && recordIndex.getPositions()!=null){
                                internalIdx.hourlyIdx.getDataFileNameToRecordIndexMap().put(dataFileName, recordIndex);
                            } else {
                                LOG.error("Could not merge record index {} for data file {}", internalIdx.indexName, dataFileName);
                            }
                        }
    
                    }                
                } else {
                    //data file is NOT present in the hourly index
                    // build that record index file and merge it into hourlyIdx
                    internalIdx.needsToBeStored = true;
                    indexesToRebuild.add(internalIdx);    
                }
            }

            if(!indexesToRebuild.isEmpty()){
                //rebuild indexes for a given datafile, as determined above
                buildCountsAndPositions(dataFileName, fNamePrefix, indexesToRebuild);
            }
            
        }
        
        //now, after processing all datafiles, store those directory indexes that were marked as needsToBeStored 
        for(InternalDirectoryIndex internalDirIdx: internalDirectoryIndexes){
            if(internalDirIdx.needsToBeStored){
                //store zipped hourlyIdx under name hourlyIdxFileName
                storeZippedModelInFile(internalDirIdx.hourlyIdxFileName, internalDirIdx.hourlyIdx);
            }
        }

        LOG.debug("completed buildHourlyIndexesForSingleFileNamePrefix({})", fNamePrefix);

    }
    
    
    private void storeZippedModelInFile(String fileName, BaseJsonModel model) {

        LOG.info("storing {} in {}/{}", model.getClass().getSimpleName(), dsRootDirName, fileName);


        byte[] collectedBytes;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(10 * 1024);
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            // entry name is without ".zip"
            ZipEntry entry = new ZipEntry(fileName.substring(fileName.lastIndexOf('/'), fileName.length() - 4));
            zipOutputStream.putNextEntry(entry);

            byte[] modelBytes = model.toString().getBytes(StandardCharsets.UTF_8);
            zipOutputStream.write(modelBytes);
            zipOutputStream.write(13);
            zipOutputStream.write(10);

            zipOutputStream.closeEntry();
            zipOutputStream.flush();
            zipOutputStream.finish();

            outputStream.flush();

            collectedBytes = outputStream.toByteArray();
        } catch (IOException e) {
            throw new GenericErrorException("Cannot write zip entry into " + fileName, e);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(collectedBytes);
        
        LOG.info("Upload started (size {}): {}/{}", collectedBytes.length, dsRootDirName, fileName);
        try(FileOutputStream fos = new FileOutputStream(new File(dsRootDirName, fileName))) {
        	StreamUtils.copy(bais, fos);
        	fos.flush();

            LOG.info("Upload complete: {}/{}", dsRootDirName, fileName);
        } catch (IOException e) {
            LOG.error("Unable to upload stream into {}/{}, upload was aborted. {}", dsRootDirName, fileName, e);
        }
    }

    
    /**
     * Convenience class to group internal data structures for the record index
     * @author dtop
     *
     */
    private static class InternalRecordIndex{
        String indexName;
        RecordIndexCounts recordIndexCounts = new RecordIndexCounts(); 
        RecordIndexPositions recordIndexPositions = new RecordIndexPositions();
        RecordIndexValueExtractor valueExtractor;
        
        InternalRecordIndex(String fileNamePrefix, String indexName, RecordIndexRegistry recordIndexRegistry){
            this.indexName = indexName;
            recordIndexCounts.setName(indexName);
            recordIndexPositions.setName(indexName);
            valueExtractor = recordIndexRegistry.getIndexValueExtractor(fileNamePrefix, indexName);
        }
        
    }
    
    private void buildCountsAndPositions(String dataFileName, String fileNamePrefix, List<InternalDirectoryIndex> indexesToRebuild) {

        Map<String, InternalRecordIndex> recordIndexes = new HashMap<>();
        
        //initialize new record indexes
        for(InternalDirectoryIndex internalIdx: indexesToRebuild){
            recordIndexes.put(internalIdx.indexName, new InternalRecordIndex(fileNamePrefix, internalIdx.indexName, recordIndexRegistry));            
        }

        LOG.debug("Building record indexes for {}", dataFileName);

        try(FileInputStream fis = new FileInputStream(new File(dsRootDirName, dataFileName))) {
            
            ZipEntry ze;
            String zipEntryName;
            
            try(ZipInputStream zis = new ZipInputStream(fis)){
                
                while ((ze=zis.getNextEntry())!=null){
                    zipEntryName = ze.getName();
                    LOG.trace("Processing zip entry {}", zipEntryName);
                    InputStreamReader isr = new InputStreamReader(zis, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr); 
                    
                    int lineNum = 0;
                    Set<String> idxValues;

                    for(String line; (line = br.readLine()) != null; ) {
                       
                       BaseJsonModel entity = null;
                       try{
                           entity = BaseJsonModel.fromString(line, BaseJsonModel.class);
                       }catch(Exception e){
                            LOG.debug("Could not deserialize entry {}", line);
                       }

                       if(entity!=null){
                           for(InternalRecordIndex iRecordIdx: recordIndexes.values()){
                               idxValues = iRecordIdx.valueExtractor.extractValues(entity);
                               for(String idxValue: idxValues){
                                   iRecordIdx.recordIndexCounts.incrementCountForValue(idxValue);
                                   iRecordIdx.recordIndexPositions.addPositionForValue(idxValue, lineNum);
                               }
                           }

                       }
                       lineNum++; 

                    }
                    
                    LOG.trace("Read {} entries", lineNum);            

                }
                
            }
            
        } catch (FileNotFoundException e){
            LOG.trace("file {} does not exist", dataFileName);
            return;
        } catch (IOException e) {
            throw new GenericErrorException(e);
        }
        

        //merge freshly built record indexes into supplied indexesToRebuild
        for(InternalDirectoryIndex internalIdx: indexesToRebuild){
            InternalRecordIndex iRecordIdx = recordIndexes.get(internalIdx.indexName);
            RecordIndex recordIndex =  new RecordIndex(iRecordIdx.recordIndexCounts, iRecordIdx.recordIndexPositions);           
            internalIdx.hourlyIdx.getDataFileNameToRecordIndexMap().put(dataFileName, recordIndex);
        }

        LOG.debug("Completed building record indexes for {}", dataFileName);

    }

    
    public static void main(String[] args) {
                
        RecordIndexRegistry rir = new RecordIndexRegistry();
        rir.postConstruct();
        HazelcastObjectsConfiguration hazelcastObjectsConfiguration = new HazelcastObjectsConfiguration();
        HazelcastInstance hazelcastClient = new HazelcastForUnitTest().hazelcastInstanceTest();
        HourlyIndexAggregatorHazelcastScalable hia = new HourlyIndexAggregatorHazelcastScalable(
                "/Users/dtop/hierarchical_ds", "dev1", rir, hazelcastClient, hazelcastObjectsConfiguration);

        long startTime = System.currentTimeMillis()
                - TimeUnit.DAYS.toMillis(30)
                - TimeUnit.HOURS.toMillis(8)
                ;
        
        long endTime = startTime + 1;
        
        long withIndexesTsStart = System.currentTimeMillis();
        //try with 1 hour and indexes existing
        hia.buildHourlyIndex(13, 834, startTime, endTime);
        long withIndexesTsEnd = System.currentTimeMillis();
        
        System.out.println("Took "+ (withIndexesTsEnd - withIndexesTsStart) + " ms to re-build hourly index");
        
        hazelcastClient.shutdown();
    }
}
