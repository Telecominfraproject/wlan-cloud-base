package com.telecominfraproject.wlan.hierarchical.datastore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an iterator that returns full names of the hourly index files for a given HDS, index, customer, equipment and time range.
 * We do not pre-build the complete list of files because it may be too long - depending on a time range.
 * 
 * @author dtop
 */
public class HourlyIndexFileNames implements Iterable<String> {

    private static final Logger LOG = LoggerFactory.getLogger(HourlyIndexFileNames.class);

    public final static String hourlyIndexFileNamePrefix = "hrIdx_";

    private final int customerId;
    private final long equipmentId;
    private final long fromTime;
    private final long toTime;
    private final String indexName;
    private final String dsPrefix;
    private final String fileNamePrefix;
    private final int numberOfMinutesPerFile;
    private final int size;
    private final int hourIncrement;
    
    
    private boolean hasNextValue = true;
    private String nextValue;
    private Calendar fromCalendar;
    private Calendar toCalendar;
    
    public HourlyIndexFileNames(int customerId, long equipmentId, long fromTime, long toTime, String indexName, String dsPrefix, String fileNamePrefix, int numberOfMinutesPerFile){

        this.numberOfMinutesPerFile = numberOfMinutesPerFile;
        
        if(numberOfMinutesPerFile<=60){
            hourIncrement = 1;
        } else {
            // if number of minutes per file is greater than one hour, not every hourly directory will contain the data files
            // we'll skip those hourly directories that do not have the data we're looking for 
            if(numberOfMinutesPerFile%60!=0){
                throw new IllegalArgumentException("If number of minutes per file is greater than 60, it must be a multiple of 60");
            }
            hourIncrement = numberOfMinutesPerFile/60;
        }
        
        if(fromTime>toTime){
            //invalid date range, return empty list
            LOG.debug("HourlyIndexFileNames for customer {} equipment {} from {} to {} built 0 files. invalid time range.", customerId, equipmentId, fromTime, toTime);
            this.hasNextValue = false;
            this.size = 0;
        } else {
        
            //if toTime is in the future - set it back to now
            long currentTime = System.currentTimeMillis();
            if(toTime>currentTime){
                toTime = currentTime;
            }

            //NOT needed, because each data file contains entries from beginning of a numberOfMinutesPerFile interval
            //keeping it here so we do not forget about it and not over-think this logic
            //            //adjust fromTime so it catches file that may be on a boundary of an hour
            //            fromTime = fromTime - 60000L*this.numberOfMinutesPerFile; 

            //adjust fromTime so it is a multiple of 1 hour
            fromTime = fromTime - fromTime%(TimeUnit.HOURS.toMillis(1) * getHourIncrement()); 

            this.size = (int)((toTime - fromTime)/(TimeUnit.HOURS.toMillis(1) * getHourIncrement())) + 1;
            
            fromCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            fromCalendar.setTime(new Date(fromTime));
            
            toCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            toCalendar.setTime(new Date(toTime));

        }
        
        this.customerId = customerId;
        this.equipmentId = equipmentId;
        this.fromTime = fromTime;
        this.toTime = toTime; 
        this.indexName = indexName;
        this.dsPrefix = dsPrefix;
        this.fileNamePrefix = fileNamePrefix;
    }
    
    /**
     * @return number of files that will be produced if caller iterates through all of them
     */
    public int getSize() {
        return size;
    }
    
    public int getHourIncrement() {
        return hourIncrement;
    }

    private String getFileName(int year, int month, int day, int hour){
        StringBuilder sb = new StringBuilder(1024);
        Formatter formatter = new Formatter(sb, null);
        formatter.format("%s/%d/%d/%4d/%02d/%02d/%02d/%s%s_%s_%d_%d_%4d_%02d_%02d_%02d.zip",
                dsPrefix, customerId, equipmentId, year, month, day, hour,
                hourlyIndexFileNamePrefix, indexName, fileNamePrefix, 
                customerId, equipmentId, year, month, day, hour
                );
        formatter.close();
        
        return sb.toString();
    }

    private void advanceToNextValue() {
        // generate list of files based on supplied criteria
        if (hasNextValue) {

            int year = fromCalendar.get(Calendar.YEAR);
            int month = fromCalendar.get(Calendar.MONTH) + 1;
            int day = fromCalendar.get(Calendar.DAY_OF_MONTH);
            int hour = fromCalendar.get(Calendar.HOUR_OF_DAY);

            nextValue = getFileName(year, month, day, hour);

            // advance time for the next iteration
            fromCalendar.add(Calendar.HOUR_OF_DAY, getHourIncrement());
            hasNextValue = fromCalendar.before(toCalendar) || fromCalendar.equals(toCalendar);
        } else {
            nextValue = null;
        }
    }
    
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {

            @Override
            public boolean hasNext() {
                return hasNextValue;
            }

            @Override
            public String next() {
                if (!hasNextValue) {
                    throw new NoSuchElementException("No more element");
                }
                advanceToNextValue();
                return nextValue;
            }

        };
    }

    public static void main(String[] args) {
        long fromTime = System.currentTimeMillis() 
                - TimeUnit.MINUTES.toMillis(30) 
                - TimeUnit.HOURS.toMillis(33);
        long toTime = System.currentTimeMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS XXX");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        System.out.println("From: "+ sdf.format(new Date(fromTime))+" To: "+sdf.format(new Date(toTime)));
        
        HourlyIndexFileNames hifn = new HourlyIndexFileNames(1, 2, fromTime, toTime, "metricDataType", "dev1", "sm_x24h", 24*60);
        
        for(String fn: hifn){
            System.out.println(fn);
        }
        
        System.out.println("size: "+ hifn.getSize());
    }
}
