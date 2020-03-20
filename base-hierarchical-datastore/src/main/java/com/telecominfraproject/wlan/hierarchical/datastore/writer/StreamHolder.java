package com.telecominfraproject.wlan.hierarchical.datastore.writer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.hierarchical.datastore.HierarchicalDatastore;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexCounts;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexPositions;
import com.telecominfraproject.wlan.hierarchical.datastore.index.RecordIndexValueExtractor;
import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

/**
 * @author dtop
 * Class that holds a single zipStream and related properties. 
 * Responsible for creation of the stream, writing models into it, and uploading stream to s3.
 */
public class StreamHolder {

    private static final Logger LOG = LoggerFactory.getLogger(StreamHolder.class);

    private ByteArrayOutputStream outputStream;
    private ZipOutputStream zipOutputStream;
    private long zipStreamStartTimeMs;

    private final int customerId;
    private final long equipmentId;
    private final HierarchicalDatastore hierarchicalDatastore;
    
    private int bytesWrittenToPart;
    private int lineNumber;
    private String fullFileName;
    private String partFileName;
    private long timestampFromFileName;
    private long lastModelWrittenToStreamTimestampMs = System.currentTimeMillis(); 
    
    private Map<String, RecordIndexCounts> indexCountsMap = new HashMap<>();
    private Map<String, RecordIndexPositions> indexPositionsMap = new HashMap<>();
    
    public StreamHolder(long streamFirstModelStartTimeMs, int customerId, long equipmentId, HierarchicalDatastore hierarchicalDatastore){
        this.customerId = customerId;
        this.equipmentId = equipmentId;
        this.hierarchicalDatastore = hierarchicalDatastore;
        
        this.zipStreamStartTimeMs = streamFirstModelStartTimeMs;
        //normalize timestamp to n minutes - per HDatastore configuration
        this.zipStreamStartTimeMs = zipStreamStartTimeMs - zipStreamStartTimeMs%(1L*hierarchicalDatastore.getNumberOfMinutesPerFile()*60*1000);
        
        this.fullFileName = hierarchicalDatastore.getFileNameForNewFile(customerId, equipmentId, zipStreamStartTimeMs);
        //extract file name - from the last '/' until '.zip'
        this.partFileName = fullFileName.substring(fullFileName.lastIndexOf('/')+1, fullFileName.length()-4);
        //extract timestamp from the file name - number after the last '_'
        this.timestampFromFileName = Long.parseLong(partFileName.substring(partFileName.lastIndexOf('_')+1));
        
        this.outputStream = new ByteArrayOutputStream(5*1024);
        this.zipOutputStream = new ZipOutputStream(outputStream);
        ZipEntry entry = new ZipEntry(partFileName);
        
        try {
            this.zipOutputStream.putNextEntry(entry); 
        } catch (IOException e) {
            throw new GenericErrorException("Cannot write first zip entry into "+partFileName, e);
        }
        
        LOG.info("Created new file {}", partFileName);
        
    }
    
    public long getStreamKey(){
        return zipStreamStartTimeMs;
    }
    
    public long getTimestampFromFileName(){
        return timestampFromFileName;
    }
    
    public void commitOutputStreamToFile() throws IOException {
        
        if(outputStream == null){
            //nothing to do here
            return;
        }

        LOG.info("Closing existing stream from queue({}_{}_{})", hierarchicalDatastore.getFileNamePrefix(), customerId, equipmentId);

        zipOutputStream.closeEntry();
        zipOutputStream.flush();
        zipOutputStream.close();

        outputStream.flush();
        
        //write into file only if at least one record was put into output stream
        //otherwise - just close existing stream
        if(bytesWrittenToPart > 0){
            //write collected bytes into file
            byte[] collectedBytes = outputStream.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(collectedBytes);
            hierarchicalDatastore.uploadStreamToFileOverwriteOld(bais, collectedBytes.length, fullFileName);
            LOG.trace("Uploaded to s3 {}", fullFileName);

        }
        
        outputStream.close();
        outputStream = null;
        zipOutputStream = null;
        
        
        //now write accumulated values for all registered indexes - one file per index, not compressed
        Map<String, RecordIndexValueExtractor> allIndexes = hierarchicalDatastore.getRecordIndexes();
        for(Map.Entry<String, RecordIndexValueExtractor> idxEntry: allIndexes.entrySet()){
            String idxName = idxEntry.getKey();
            
            //for every index we'll store record counts and record line numbers in the data file
            RecordIndexCounts idxCounts = indexCountsMap.get(idxName);
            if(idxCounts==null){
                idxCounts = new RecordIndexCounts();
                idxCounts.setName(idxName);
            }

            RecordIndexPositions idxPositions = indexPositionsMap.get(idxName);
            if(idxPositions==null){
                idxPositions = new RecordIndexPositions();
                idxPositions.setName(idxName);
            }

            hierarchicalDatastore.storeRecordIndex(idxName, idxCounts, idxPositions, fullFileName);
            
            LOG.trace("Uploaded index {} for {}", idxName, fullFileName);
        }

        indexCountsMap.clear();
        indexPositionsMap.clear();

    }

    public void writeModelToStream(BaseJsonModel model) throws IOException {
        
        byte[] modelBytes = model.toString().getBytes(StandardCharsets.UTF_8); 
        zipOutputStream.write(modelBytes);
        zipOutputStream.write(13);
        zipOutputStream.write(10);
        
        bytesWrittenToPart += modelBytes.length + 2;
        
        lastModelWrittenToStreamTimestampMs = System.currentTimeMillis();
        
        //process all record indexes registered in appropriate HDS
        Map<String, RecordIndexValueExtractor> allIndexes = hierarchicalDatastore.getRecordIndexes();
        for(Map.Entry<String, RecordIndexValueExtractor> idxEntry: allIndexes.entrySet()){
            String idxName = idxEntry.getKey();
            RecordIndexValueExtractor valueExtractor = idxEntry.getValue();
            
            //for every index we'll update record counts and record line numbers in the data file
            RecordIndexCounts idxCounts = indexCountsMap.get(idxName);
            if(idxCounts==null){
                idxCounts = new RecordIndexCounts();
                idxCounts.setName(idxName);
                indexCountsMap.put(idxName, idxCounts);
            }

            RecordIndexPositions idxPositions = indexPositionsMap.get(idxName);
            if(idxPositions==null){
                idxPositions = new RecordIndexPositions();
                idxPositions.setName(idxName);
                indexPositionsMap.put(idxName, idxPositions);
            }

            for(String idxValue : valueExtractor.extractValues(model)) {
                idxCounts.incrementCountForValue(idxValue);
                idxPositions.addPositionForValue(idxValue, lineNumber);    
            }
        }
        
        //move to the next line in the data file
        lineNumber++;
    }

    public long getLastModelWrittenToStreamTimestampMs() {
        return lastModelWrittenToStreamTimestampMs;
    }

    public long getZipStreamStartTimeMs() {
        return zipStreamStartTimeMs;
    }

    public String getFullFileName() {
        return fullFileName;
    }
}
