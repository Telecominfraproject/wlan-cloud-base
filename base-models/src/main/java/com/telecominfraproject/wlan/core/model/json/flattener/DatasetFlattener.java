package com.telecominfraproject.wlan.core.model.json.flattener;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.utils.NumberUtils;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;
import com.telecominfraproject.wlan.server.exceptions.GenericErrorException;

/**
 * @author dtop 
 *         This class produces matrices of values from data files of
 *         metrics, events, and system events. Each row in the resulting matrix
 *         is a bucket of time, with value in each cell as a last known value
 *         within that bucket of time. Columns in the matrix represent
 *         individual fields in metric reports and system/equipment events.
 * 
 *         Column names are uniquely identified by their json path, and
 *         equipment ids are included into the json path. Array indexes are
 *         translated using equipmentIds and macAdresses so that each metric
 *         value from a particular equipment or from a particular client is
 *         always mapped to a specific column.
 * 
 *         Another capability provided by this class is consistent mapping of
 *         columns between training dataset and prediction dataset. When
 *         training dataset is prepared - column name mappings, column value
 *         translations, and min/max column values can be stored in json files.
 *         Later, when prediction dataset is prepared these stored maps are used
 *         to create data that is mapped to the same column positions as during
 *         training, and containing values in the same ranges as the training
 *         dataset.
 *         
 *         This class also includes methods for visualizing a matrix of values as a PNG image.  
 * 
 */
public class DatasetFlattener {
    
    private static final Logger LOG = LoggerFactory.getLogger(DatasetFlattener.class);
    
    private ColumnNameTranslator columnNameTranslator = new ColumnNameTranslator();
    /**
     * TODO: this map may become a global one so that things learned in one environment may be directly comparable and transferable to another environment
     * Map used to translate string values into numbers for a specified column name (original column name, not translated one)
     */
    private ColumnValueReplacements columnValueReplacements = new ColumnValueReplacements();
    
    private boolean populateColumnNameTranslatorAndValueReplacements = true;

    private Object[][] matrixOfValues;
    
    private Map<Long,ValueElementMap> accumulatedValues = new HashMap<>();
    
    /**
     * Arrays used to keep min and max values for a specified column index (c_xxx) - after translating strings into numbers
     */
    private ColumnMinMaxValues columnMinMaxValues = new ColumnMinMaxValues();
    
    private final Pattern macColumnPattern = Pattern.compile("^.+\\.mac\\[\\d+\\]$");
    
    private long timeBucketMs = TimeUnit.MINUTES.toMillis(2);
    
    //anonymous constructor
    {
        ArrayList<String> keys = new ArrayList<>();
        
        keys.add(getTimestampColumnName());
        //year, month, day, hour, minute, second, day_of_week, week_of_year
        keys.add(getTimestampColumnName()+"_1_year");
        keys.add(getTimestampColumnName()+"_2_month");
        keys.add(getTimestampColumnName()+"_3_day");
        keys.add(getTimestampColumnName()+"_4_hour");
        keys.add(getTimestampColumnName()+"_5_minute");
        keys.add(getTimestampColumnName()+"_6_second");
        keys.add(getTimestampColumnName()+"_7_day_of_week");
        keys.add(getTimestampColumnName()+"_8_week_of_year");
        
        int i=0;
        String cName;
        //give shorter names to columns
        for(String key: keys){
            cName = getColumnName(i);
            columnNameTranslator.getColumnNamesMap().put(key, cName);
            columnNameTranslator.getReverseColumnNamesList().add(key);
            i++;
        }
        
        if(!columnNameTranslator.getReverseColumnNamesList().get(0).equals(getTimestampColumnName())){
            throw new ConfigurationException("Column c_0 must be mapped to " + getTimestampColumnName() 
                    + ", not to " + columnNameTranslator.getReverseColumnNamesList().get(0));
        }
    }
    
    public DatasetFlattener() {
        //default constructor, used for training data sets
    }
    
    
    public DatasetFlattener(ColumnNameTranslator trainingColumnNamesTranslator,
            ColumnValueReplacements trainingValueReplacements, ColumnMinMaxValues trainingColumnMinMaxValues) {
        // This constructor is used for prediction data sets. It assumes that
        // column name translation, value replacement maps, and min/max values
        // are pre-built during the training data set preparation.
        this.columnNameTranslator = trainingColumnNamesTranslator;
        this.columnValueReplacements = trainingValueReplacements;
        this.columnMinMaxValues = trainingColumnMinMaxValues;
        this.populateColumnNameTranslatorAndValueReplacements = false;
    }


    public void addValue(long timestampMs, Map<String, Object> values){
        
        long bucketTimestampMs = timestampMs;
        bucketTimestampMs = bucketTimestampMs - bucketTimestampMs%timeBucketMs;
        
        ValueElementMap accumulatedValue = accumulatedValues.get(bucketTimestampMs); 
        if(accumulatedValue==null){
            //bucket does not exist yet, add it
            accumulatedValue = new ValueElementMap(bucketTimestampMs, new HashMap<>());
            accumulatedValues.put(bucketTimestampMs, accumulatedValue);
        }
        
        ValueElementMap newValueElementMap = new ValueElementMap(timestampMs, values);
        if(populateColumnNameTranslatorAndValueReplacements){
            //we're processing training data set nd need to build column name translation map
            appendColumnNamesToTranslationMaps(newValueElementMap);
        }
        
        newValueElementMap = replaceColumnNamesAndValues(newValueElementMap);

        //merge new value into accumulated one
        synchronized (accumulatedValue) {
            accumulatedValue.values.putAll(newValueElementMap.values);
        }
        
    }
    
    private ValueElementMap replaceColumnNamesAndValues(ValueElementMap valueElementMap) {
        ValueElementMap ret = new ValueElementMap(valueElementMap.timestampMs, new HashMap<>());
        
        for(Map.Entry<String, Object> entry: valueElementMap.values.entrySet()){
            String translatedColumnName = columnNameTranslator.getColumnNamesMap().get(entry.getKey());
            
            if(translatedColumnName == null){
                //This can happen when training data set did not have a column, but prediction dataset has it.
                //In this case we'll skip this column, as it was unknown when training data set was produced.
                continue;
            }
            
            Object value = entry.getValue();
            if(value instanceof String){                
                // For a string value - replace it with a unique number.
                // If the replacement value is null then the entry will be skipped.
                // This can happen when training data set does not know of a new value that is present in the prediction data set.
                value = replaceStringValueInColumn(entry.getKey(), (String) value); 
            }
            
            if(value==null){
                //no need to keep track of null values at this point, they will be represented as nulls in the matrix anyway
                continue;
            }
                
            ret.values.put(translatedColumnName, value);
        }
        
        return ret;
    }

    public static String getTimestampColumnName(){
        //use a name that would always be on top when the list is sorted
        return "000___timestamp";
    }
    
    private void appendColumnNamesToTranslationMaps(ValueElementMap valueElementMap){
        Set<String> keysSet = new HashSet<>();
        
        //gather all possible key names, skip the ones we already know about
        for(String key: valueElementMap.values.keySet()){
            if(!columnNameTranslator.getColumnNamesMap().containsKey(key)){
                keysSet.add(key);
            }
        }
        
        ArrayList<String> keys = new ArrayList<>(keysSet);

        //sort original keys so that properties of each object are clumped in neighbouring columns
        Collections.sort(keys);
        int i=columnNameTranslator.getReverseColumnNamesList().size();
        String cName;
        //give shorter names to columns
        for(String key: keys){
            cName = getColumnName(i);
            columnNameTranslator.getColumnNamesMap().put(key, cName);
            columnNameTranslator.getReverseColumnNamesList().add(key);
            i++;
        }

    }
    
    /**
     * colIdx is 0-based. First column name is c_0
     * @param colIdx
     * @return "c_"+ colIdx
     */
    public static String getColumnName(int colIdx){
        return "c_"+ colIdx;
    }
    
    public Map<String, String> getColumnNamesTranslationMap(){
        return columnNameTranslator.getColumnNamesMap();
    }

    public List<String> getReverseColumnNamesTranslationList(){
        return columnNameTranslator.getReverseColumnNamesList();
    }
    
    /**
     * Populate matrix of values with all the columns.
     * First column (0) contains timestamp of the row. 
     * All other columns are populated from accumulated json attributes for that timestamp.
     * Timestamps in the matrix are not unique, but sorted - earlier timestamps have lower row numbers (are closer to the top). 
     * Matrix is sparse - contains null values.
     * After conversion is completed, all accumulated values are cleared.
     * 
     * @return
     */
    public void produceMatrixOfValues(TargetFormulaInterface targetFormulaImpl){
        getMatrixOfValues(targetFormulaImpl);
        //release accumulated values - they hold to a lot of memory
        accumulatedValues.clear();
    }
    
    public String getTargetFormulaColumnName(){
        return "targetValue";
    }

    public Object[][] getMatrixOfValues(){
        return getMatrixOfValues(null);
    }

    public Object[][] getMatrixOfValues(TargetFormulaInterface targetFormulaImpl){


        if(matrixOfValues==null){

            if(targetFormulaImpl!=null){
                //add formula column to hold computed value for each row
                String targetFormulaCName = getColumnName(columnNameTranslator.getReverseColumnNamesList().size());
                columnNameTranslator.getColumnNamesMap().put(getTargetFormulaColumnName(), targetFormulaCName);
                columnNameTranslator.getReverseColumnNamesList().add(getTargetFormulaColumnName());
            }

            
            LOG.debug("Allocating array of {} x {} ", accumulatedValues.size(), columnNameTranslator.getReverseColumnNamesList().size());

            matrixOfValues = new Object[accumulatedValues.size()][columnNameTranslator.getReverseColumnNamesList().size()]; 

            int numColumns = columnNameTranslator.getReverseColumnNamesList().size();
            int row = 0;
            int colIdx = 0;
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            
            List<ValueElementMap> valueList = new ArrayList<>(accumulatedValues.values());
            Collections.sort(valueList);
            
            for(ValueElementMap v: valueList){
                colIdx = 0;
                //store value timestamp in the first column
                matrixOfValues[row][colIdx++] = v.timestampMs;
                
                
                //store elements of a parsed timestamp in separate columns: year, month, day, hour, minute, second, day_of_week, week_of_year
                calendar.setTimeInMillis(v.timestampMs);
                matrixOfValues[row][colIdx++] = calendar.get(Calendar.YEAR);
                matrixOfValues[row][colIdx++] = calendar.get(Calendar.MONTH);
                matrixOfValues[row][colIdx++] = calendar.get(Calendar.DAY_OF_MONTH);
                matrixOfValues[row][colIdx++] = calendar.get(Calendar.HOUR_OF_DAY);
                matrixOfValues[row][colIdx++] = calendar.get(Calendar.MINUTE);
                matrixOfValues[row][colIdx++] = calendar.get(Calendar.SECOND);
                matrixOfValues[row][colIdx++] = calendar.get(Calendar.DAY_OF_WEEK);
                matrixOfValues[row][colIdx++] = calendar.get(Calendar.WEEK_OF_YEAR);
                
                //populate all other values
                for(int column=colIdx; column<numColumns; column++ ){
                    
                    if(column<numColumns-1){
                        //regular column, nothing special here
                        matrixOfValues[row][column] = v.values.get(getColumnName(column));
                    } else {
                        //compute target formula value and store it in the last column
                        if(targetFormulaImpl!=null){
                            matrixOfValues[row][column] = targetFormulaImpl.produceValue(v);
                        }                        
                    }
                    
                }
                                
                row++;
            }
        }

        return matrixOfValues;
    }

    /**
     * Translate unique string into a number specific for a given column
     * 
     * @param column
     * @param incoming string
     * @return number that represents incoming string in a given column. In the
     *         training mode this method will populate value replacement map. In
     *         prediction mode this method will use pre-built value replacement
     *         map (if value replacement is not found, it will return null).
     */
    private Number replaceStringValueInColumn(String fullColumnName, String strValue) {
        Map<String, Integer> valuesForColumn = columnValueReplacements.getColumnValueTranslationMap().get(fullColumnName);
        if( valuesForColumn == null){
            if(!populateColumnNameTranslatorAndValueReplacements){
                //we do not populate new values in this mode, we assume that all known values have already been processed (during training)
                //if we cannot find translation at this point, that means this value was not present during training, and we will return null for it
                return null;
            } else {
                //training mode, auto-build value replacement map
                valuesForColumn = new HashMap<>();
                columnValueReplacements.getColumnValueTranslationMap().put(fullColumnName, valuesForColumn);
            }
        }
        
        
        Integer translatedNumber = valuesForColumn.get(strValue);
        if(translatedNumber==null){
            
            if(!populateColumnNameTranslatorAndValueReplacements){
                //we do not populate new values in this mode, we assume that all known values have already been processed (during training)
                //if we cannot find translation at this point, that means this value was not present during training, and we will return null for it
                return null;
            } else {
                //training mode, auto-build value replacement map

                //generate new number and use it for this object value in this column
                translatedNumber = valuesForColumn.size() + 1;
                //put translated number in a map for future use
                valuesForColumn.put(strValue, translatedNumber);
            }
        }

        return translatedNumber;
    }
    
    private void collectMinMaxValuesPerColumn() {

        int numColumns = matrixOfValues[0].length;
        
        double[] minColumnValues = new double[numColumns];
        double[] maxColumnValues = new double[numColumns];
        
        columnMinMaxValues.setMinValues(minColumnValues);
        columnMinMaxValues.setMaxValues(maxColumnValues);
        
        double dVal;
        String origColumnName;
        for(int col=0; col<numColumns; col++){
            origColumnName = columnNameTranslator.getReverseColumnNamesList().get(col);
            
            //for columns that represent mac addresses - use min=0, max=255 (.mac[2])
            if(macColumnPattern.matcher(origColumnName).matches()){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 255;
                continue;
            }
            
            //for columns that represent TimeOfDay/DayOfWeek values - use consistent min/max values
            if(origColumnName.equals(getTimestampColumnName()+"_1_year")){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 5000;
                continue;                
            }
            if(origColumnName.equals(getTimestampColumnName()+"_2_month")){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 12;
                continue;                
            }
            if(origColumnName.equals(getTimestampColumnName()+"_3_day")){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 31;
                continue;                
            }
            if(origColumnName.equals(getTimestampColumnName()+"_4_hour")){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 24;
                continue;                
            }
            if(origColumnName.equals(getTimestampColumnName()+"_5_minute")){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 60;
                continue;                
            }
            if(origColumnName.equals(getTimestampColumnName()+"_6_second")){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 60;
                continue;                
            }
            if(origColumnName.equals(getTimestampColumnName()+"_7_day_of_week")){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 7;
                continue;                
            }
            if(origColumnName.equals(getTimestampColumnName()+"_8_week_of_year")){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 53;
                continue;                
            }
            if(origColumnName.endsWith("Percentage")||origColumnName.endsWith("Percent") || origColumnName.endsWith("Pct")){
                minColumnValues[col] = 0;
                maxColumnValues[col] = 100;
                continue;                
            }
            
            

            //for all other columns - calculate min/max from actual data in the column 
            minColumnValues[col] = Double.MAX_VALUE;
            maxColumnValues[col] = Double.MIN_VALUE;

            for(int row=0; row<matrixOfValues.length; row++){
                if(matrixOfValues[row][col]==null){
                    continue;
                }
                
                dVal = ((Number)matrixOfValues[row][col]).doubleValue();
                if( Double.compare(dVal, minColumnValues[col]) < 0 ){
                    minColumnValues[col] = dVal;
                }
                
                if( Double.compare(dVal, maxColumnValues[col]) > 0 ){
                    maxColumnValues[col] = dVal;
                }

            }
        }
    }

    /**
     * Translate every value m[i,j] into ((m[i,j] - min[j]) / (max[j] - min[j]) - 1/2) * 2 so that resulting value falls in range [-1, 1]
     * 
     * See http://www.faqs.org/faqs/ai-faq/neural-nets/part2/
     * Normalize values in each column. 
     * For linear NNs, scaling the inputs to [-1,1] will work better than [0,1]
     * 
     * @param nullValue - value to use to replace nulls
     */
    private void normalizeValuesPerColumn(int nullValue) {
        // translate every value m[i,j] into ((m[i,j] - min[j]) / (max[j] - min[j]) - 1/2) * 2 so that resulting value falls in range [-1, 1]
        int numColumns = matrixOfValues[0].length;
        double[] maxColumnValues = columnMinMaxValues.getMaxValues();
        double[] minColumnValues = columnMinMaxValues.getMinValues();
        
        double dVal;
        for(int col=0; col<numColumns; col++){
            double deltaCol = maxColumnValues[col] - minColumnValues[col];
            
            for(int row=0; row<matrixOfValues.length; row++){
                if (matrixOfValues[row][col] == null) {
                    dVal = nullValue;
                } else {
                    dVal = ((Number) matrixOfValues[row][col]).doubleValue();
                    if (!NumberUtils.isDoubleZero(dVal)) {
                        dVal = ((dVal - minColumnValues[col]) / (deltaCol) - 0.5) * (double) 2;
                    } else {
                        dVal = 1;
                    }
                }

                matrixOfValues[row][col] = dVal;
            }
        }
        
    }
    
    private void populateNullsFromOlderRows(List<Pattern> fullColumnNamePatterns){
        
        List<Integer> columnIndexesToProcess = new ArrayList<>();
        
        //find out what columns need to be filled in according to the supplied list of regexp patterns
        Matcher matcher;
        for(Pattern pattern: fullColumnNamePatterns){
            for(int i=0; i< columnNameTranslator.getReverseColumnNamesList().size(); i++){
                matcher = pattern.matcher(columnNameTranslator.getReverseColumnNamesList().get(i));
                if(matcher.matches()){
                    columnIndexesToProcess.add(i);
                }
            }
        }

        if(columnIndexesToProcess.isEmpty()){
            //nothing to process
            return;
        }
        
        //in the first pass replace each null value with the first non-null value above it
        for(int col: columnIndexesToProcess){
            for(int row=0; row<matrixOfValues.length; row++){
                if(matrixOfValues[row][col]==null){
                    matrixOfValues[row][col] = findClosestNonNullValueAbove(row, col);
                }
            }
        }
        
        //after the first pass, fill in nulls for the specified columns from below - to populate the series of nulls at the top of the column
        for(int col: columnIndexesToProcess){
            for(int row=0; row<matrixOfValues.length; row++){
                if(matrixOfValues[row][col]==null){
                    matrixOfValues[row][col] = findClosestNonNullValueBelow(row, col);
                }
            }
        }
        
    }

    private Object findClosestNonNullValueAbove(int row, int column) {
        for(int i=row; i>=0; i--){
            if(matrixOfValues[i][column]!=null){
                return matrixOfValues[i][column];
            }
        }
        return null;
    }

    private Object findClosestNonNullValueBelow(int row, int column) {
        for(int i=row; i<matrixOfValues.length; i++){
            if(matrixOfValues[i][column]!=null){
                return matrixOfValues[i][column];
            }
        }
        return null;
    }

    public static void printAccumulatedValuesWithBuckets(Object[][] matrix, int nullValue, long timeBucketMs){

        Object v;
        Object bucketedRow[] = new Object[matrix[0].length];
        long bucketTimestamp = 0;
        long nextRowTimestamp;
        
        for(int row=0; row<matrix.length; row++){
            
            if(bucketTimestamp==0){
                //start accumulating values for the new bucket
                bucketTimestamp = ((Number)matrix[row][0]).longValue();
                bucketTimestamp = bucketTimestamp - bucketTimestamp%timeBucketMs;
                bucketedRow = new Object[matrix[0].length];
                //fill in timestamp-related values
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                
                int colIdx = 0;
                //store value timestamp in the first column
                bucketedRow[colIdx++] = bucketTimestamp;
                
                //store elements of a parsed timestamp in separate columns: year, month, day, hour, minute, second, day_of_week, week_of_year
                calendar.setTimeInMillis(bucketTimestamp);
                bucketedRow[colIdx++] = calendar.get(Calendar.YEAR);
                bucketedRow[colIdx++] = calendar.get(Calendar.MONTH);
                bucketedRow[colIdx++] = calendar.get(Calendar.DAY_OF_MONTH);
                bucketedRow[colIdx++] = calendar.get(Calendar.HOUR_OF_DAY);
                bucketedRow[colIdx++] = calendar.get(Calendar.MINUTE);
                bucketedRow[colIdx++] = calendar.get(Calendar.SECOND);
                bucketedRow[colIdx++] = calendar.get(Calendar.DAY_OF_WEEK);
                bucketedRow[colIdx++] = calendar.get(Calendar.WEEK_OF_YEAR);
                
            }
            
            //transfer the rest of the columns into the bucketedRow (only non-null values can overwrite null ones)
            for(int column=9; column<matrix[row].length; column++){
                v = matrix[row][column];
                if(v!=null){
                    bucketedRow[column] = matrix[row][column];
                }
            }
            
            //detect if the bucket has ended
            boolean endOfBucket = false;
            if(row+1 < matrix.length){
                //next row exists
                nextRowTimestamp = ((Number)matrix[row+1][0]).longValue();
                nextRowTimestamp = nextRowTimestamp - nextRowTimestamp%timeBucketMs;
                
                if(nextRowTimestamp != bucketTimestamp){
                    //next row belongs to a different bucket
                    endOfBucket = true;
                }
            } else {
                //this is the last row, close the bucket
                endOfBucket = true;
            }
            
            if(endOfBucket) {
                //print bucketed row and prepare for the next bucket
                bucketTimestamp = 0;
                for(int column=0; column<bucketedRow.length; column++){
                    v = bucketedRow[column];
                    
                    if(v==null){
                        v = nullValue;
                    }
                    System.out.print(v);
                    if(column< matrix[row].length-1){
                        System.out.print(",");
                    }
                }
                System.out.println();
            }
        }

    }
    
    private Integer[][] normalizeIntMatrix(int minValue, int maxValue) {
        Integer[][] ret = new Integer[matrixOfValues.length][];
        for(int row=0; row<matrixOfValues.length; row++){
            ret[row] = new Integer[matrixOfValues[row].length];
            
            for(int column=0; column<matrixOfValues[row].length; column++){
                ret[row][column] = ((int)((((Number)matrixOfValues[row][column]).doubleValue() + 1) * (maxValue - minValue) / 2)) + minValue;
            }
        }
        
        return ret;
    }

    public static <T> void writeMatrixOfValues(T[][] m, Writer writer) throws IOException {
        if(m==null || writer==null){
            return;
        }
        
        LOG.info("writing matrix [{}][{}]", m.length, m[0].length);

        //write column header
        for(int column=0; column<m[0].length; column++){
            writer.write(getColumnName(column) + ((column<m[0].length-1)?",":"\n"));
        }

        //write data
        for(int row=0; row<m.length; row++){
            for(int column=0; column<m[row].length; column++){
                writer.write("" + m[row][column] + ((column<m[row].length-1)?",":"\n"));
            }
        }
        
        writer.flush();
        
    }

    public static <T> void writeMatrixOfValues(T[][] m) {
        try(Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {
            writeMatrixOfValues(m, writer );
        } catch (Exception e) {
            throw new GenericErrorException("Cannot output matrix of values", e);
        }
    }

    public static <T> void writeMatrixOfValues(T[][] m, String fileName) {
        try(Writer writer = new BufferedWriter(new FileWriter(new File(fileName)))) {
            writeMatrixOfValues(m, writer );
            LOG.info("wrote matrix [{}][{}] into file {}", m.length, m[0].length, fileName);
        } catch (Exception e) {
            LOG.error("Cannot save matrix of values into {} ", fileName, e);
            throw new GenericErrorException("Cannot save matrix of values", e);
        }
        
    }

    public static void writeColumnNameTranslations(ColumnNameTranslator nameTranslator, Writer writer) throws IOException {
        if(nameTranslator==null || writer==null){
            return;
        }
        
        LOG.info("writing column name translator");

        writer.write(nameTranslator.toString());        
        writer.flush();
        
    }

    public static void writeColumnNameTranslations(ColumnNameTranslator nameTranslator) {
        try(Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {
            writeColumnNameTranslations(nameTranslator, writer );
        } catch (Exception e) {
            throw new GenericErrorException("Cannot output ColumnNameTranslator", e);
        }
    }

    public static void writeColumnNameTranslations(ColumnNameTranslator nameTranslator, String fileName) {
        try(Writer writer = new BufferedWriter(new FileWriter(new File(fileName)))) {
            writeColumnNameTranslations(nameTranslator, writer );
            LOG.info("wrote ColumnNameTranslator into file {}", fileName);
        } catch (Exception e) {
            LOG.error("Cannot save ColumnNameTranslator into {} ", fileName, e);
            throw new GenericErrorException("Cannot save ColumnNameTranslator", e);
        }
        
    }

    public static ColumnNameTranslator readColumnNameTranslations(String fileName) {
        
        try {
            ColumnNameTranslator nameTranslator = ColumnNameTranslator.fromFile(fileName, ColumnNameTranslator.class);
            LOG.info("read ColumnNameTranslator from file {}", fileName);
            return nameTranslator;
        } catch (Exception e) {
            LOG.error("Cannot read ColumnNameTranslator from {} ", fileName, e);
            throw new GenericErrorException("Cannot read ColumnNameTranslator", e);
        }
        
    }

    
    public static void writeColumnValueReplacements(ColumnValueReplacements valueReplacements, Writer writer) throws IOException {
        if(valueReplacements==null || writer==null){
            return;
        }
        
        LOG.info("writing column value replacements");

        writer.write(valueReplacements.toString());
        writer.flush();
        
    }

    public static void writeColumnValueReplacements(ColumnValueReplacements valueReplacements) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {
            writeColumnValueReplacements(valueReplacements, writer );
        } catch (Exception e) {
            throw new GenericErrorException("Cannot output ColumnValueReplacements", e);
        }
    }

    public static void writeColumnValueReplacements(ColumnValueReplacements valueReplacements, String fileName) {
        try(Writer writer = new BufferedWriter(new FileWriter(new File(fileName)))) {            
            writeColumnValueReplacements(valueReplacements, writer );
            LOG.info("wrote ColumnValueReplacements into file {}", fileName);
        } catch (Exception e) {
            LOG.error("Cannot save ColumnValueReplacements into {} ", fileName, e);
            throw new GenericErrorException("Cannot save ColumnValueReplacements", e);
        }
        
    }

    public static ColumnValueReplacements readColumnValueReplacements(String fileName) {
        
        try {
            ColumnValueReplacements valueReplacements = ColumnValueReplacements.fromFile(fileName, ColumnValueReplacements.class);
            LOG.info("read ColumnValueReplacements from file {}", fileName);
            return valueReplacements;
        } catch (Exception e) {
            LOG.error("Cannot read ColumnValueReplacements from {} ", fileName, e);
            throw new GenericErrorException("Cannot read ColumnValueReplacements", e);
        }
        
    }


    public static void writeColumnMinMaxValues(ColumnMinMaxValues minMaxValues, Writer writer) throws IOException {
        if(minMaxValues==null || writer==null){
            return;
        }
        
        LOG.info("writing min/max column values");

        writer.write(minMaxValues.toString());
        writer.flush();
        
    }

    public static void writeColumnMinMaxValues(ColumnMinMaxValues minMaxValues) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {
            writeColumnMinMaxValues(minMaxValues, writer );
        } catch (Exception e) {
            throw new GenericErrorException("Cannot output ColumnMinMaxValues", e);
        }
    }

    public static void writeColumnMinMaxValues(ColumnMinMaxValues minMaxValues, String fileName) {
        try (Writer writer = new BufferedWriter(new FileWriter(new File(fileName)))) {
            writeColumnMinMaxValues(minMaxValues, writer );
            LOG.info("wrote ColumnMinMaxValues into file {}", fileName);
        } catch (Exception e) {
            LOG.error("Cannot save ColumnMinMaxValues into {} ", fileName, e);
            throw new GenericErrorException("Cannot save ColumnMinMaxValues", e);
        }
    }

    public static ColumnMinMaxValues readColumnMinMaxValues(String fileName) {
        
        try {
            ColumnMinMaxValues minMaxValues = ColumnMinMaxValues.fromFile(fileName, ColumnMinMaxValues.class);
            LOG.info("read ColumnMinMaxValues from file {}", fileName);
            return minMaxValues;
        } catch (Exception e) {
            LOG.error("Cannot read ColumnMinMaxValues from {} ", fileName, e);
            throw new GenericErrorException("Cannot read ColumnMinMaxValues", e);
        }
        
    }    
        
    public void appendData(String dataFileName, int maxRowsToProcessPerDataFile, List<Pattern> pathPatternsToInclude, List<Pattern> pathPatternsToExclude, boolean splitMacAddressesIntoBytes) throws IOException {
        
        //2000 per file for combined dataset produced matrix of 54 x 14K and took 28 seconds to generate PNG image
        //15000 per file for combined dataset we need ~1.5G of RAM to process, produced matrix of 300 x 28K and took 3 minutes to generate PNG image
        //Integer.MAX_VALUE: consumed 4G of RAM, produced csv file of 261Mb, PNG file of 14Mb, column names json of 34Mb, value replacements 12 Mb, min/max json 1.6Mb
        //    18:46:38.851 Read 107983 lines from /Users/dtop/123/dev_kodacloud_24hrs/metrics.data
        //    18:59:11.157 Read 86001 lines from /Users/dtop/123/dev_kodacloud_24hrs/system_events.data
        //    18:59:15.839 Read 56604 lines from /Users/dtop/123/dev_kodacloud_24hrs/equipment_events.data
        //    18:59:43.149 wrote matrix [289][146177] into file /Users/dtop/ml_matrix_training.csv
        //    18:59:43.502 wrote ColumnNameTranslator into file /Users/dtop/ml_column_names_training.json
        //    18:59:43.679 wrote ColumnValueReplacements into file /Users/dtop/ml_column_value_replacements_training.json
        //    18:59:43.737 wrote ColumnMinMaxValues into file /Users/dtop/ml_column_min_max_values_training.json
        //    19:00:36.119 wrote bitmap to /Users/dtop/ml_raster_image_training.png
        //    19:00:36.119 Completed preparation for training data in 955163 ms                
        
        InputStreamReader isr = new InputStreamReader(new FileInputStream(dataFileName), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr); 

        int lineNum = -1;
        Long timestampValue;
        Map<String, Object> map; 
        for(String line; (line = br.readLine()) != null && lineNum<=maxRowsToProcessPerDataFile; ) {
            lineNum++;
            map = ModelFlattener.flattenJson(line, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);
            timestampValue = (Long) map.get(getTimestampColumnName());
            if(timestampValue==null){
                timestampValue = 0L;
            }
            addValue(timestampValue, map);
        }
        
        br.close();
        
        LOG.info("Read {} lines from {}", lineNum, dataFileName);
    }
    
    
    /**
     * Visualize supplied matrix of integers as an image in a PNG format. 
     * @param m - matrix to visualize
     * @param fileName - where to save the generated PNG image
     */
    public static void writePngFile(Integer[][] m, String fileName) {
        int width = m[0].length;
        int height = m.length;
        int[] rgbs = new int[width* height];
        
        int i=0;
        for(Integer[] row: m){
            for(Integer cell: row){
                rgbs[i] = cell;
                i++;
            }
        }

        DataBuffer rgbData = new DataBufferInt(rgbs, rgbs.length);

        WritableRaster raster = Raster.createPackedRaster(rgbData, width, height, width,
            new int[]{0xff0000, 0xff00, 0xff}, //blue is in the least significant byte
            null);

        ColorModel colorModel = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);

        BufferedImage img = new BufferedImage(colorModel, raster, false, null);

        try {
            ImageIO.write(img, "png", new File(fileName));
        } catch (IOException e) {
            LOG.error("Failed to write {}", fileName, e);
        }
        
        LOG.info("wrote bitmap to {}", fileName);
    }

    public static void main_test_flattener(String[] args) throws IOException {
        
        long startMain = System.currentTimeMillis();
        
        List<Pattern> pathPatternsToInclude = new ArrayList<>();
        List<Pattern> pathPatternsToExclude = new ArrayList<>();
        boolean splitMacAddressesIntoBytes = false;

        pathPatternsToInclude.add(Pattern.compile("^_type$"));
        pathPatternsToInclude.add(Pattern.compile("^equipmentId$"));
        pathPatternsToInclude.add(Pattern.compile("^eventTimestamp$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\..+STA_Client_Failure$"));

        pathPatternsToInclude.add(Pattern.compile("^createdTimestamp$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\._type$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.apPerformance.+cpuUtilized$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioStats[25]G.+numRxBeacon$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioStats[25]G.+numRxData$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioStats[25]G.+numTxData$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.minCellSize[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.noiseFloor[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.cellSize[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.channelUtilization[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.apPerformance\\.cpuTemperature$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioUtilization[25]G.+$"));
        
        pathPatternsToInclude.add(Pattern.compile("^data\\.ssidStats[25]g.+addressAsString$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.ssidStats[25]g.+numClient$"));

        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+addressAsString$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+txRetries$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxData$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxSucc$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxByteSucc$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxDataRetries$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+lastTxPhyRateKb$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numRxBytes$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+rxBytes$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+rxDataBytes$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+rxLastRssi$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+snr$"));

        pathPatternsToInclude.clear();
        
        pathPatternsToExclude.add(Pattern.compile("^queueName$"));
        pathPatternsToExclude.add(Pattern.compile("^.*sessionId$"));
        pathPatternsToExclude.add(Pattern.compile("^.*[mM]essage$"));
        pathPatternsToExclude.add(Pattern.compile("^.+\\.sessionDetails\\..+$"));
        pathPatternsToExclude.add(Pattern.compile("^.+\\.historicalIssues\\..+$"));
        pathPatternsToExclude.add(Pattern.compile("^.+\\.historicalNonWifiBurstChannels\\..+$"));
        pathPatternsToExclude.add(Pattern.compile("^record\\.details\\.mitigationsEpocs\\[\\d+\\]$"));
        
        
        DatasetFlattener dfTraining = new DatasetFlattener();

        int maxRowsToProcess = 5000;
        //df.appendData("/Users/dtop/123/dev_ml_minLoadFactor/metrics.data");
        dfTraining.appendData("/Users/dtop/123/dev_kodacloud_24hrs/metrics.data", maxRowsToProcess, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);
        dfTraining.appendData("/Users/dtop/123/dev_kodacloud_24hrs/system_events.data", maxRowsToProcess, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);
        dfTraining.appendData("/Users/dtop/123/dev_kodacloud_24hrs/equipment_events.data", maxRowsToProcess, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);

        System.out.println("********************");
        
        
        System.out.println("Collected " + dfTraining.columnNameTranslator.getReverseColumnNamesList().size() +" columns");

        //convert accumulated metrics into one matrix of values        
        dfTraining.produceMatrixOfValues(null); 
        
        //print column name translation map
        System.out.println("Column translations:");
        for(Map.Entry<String, String> entry: dfTraining.columnNameTranslator.getColumnNamesMap().entrySet()){
            System.out.format("%s(%s) %n", entry.getKey(), entry.getValue());
        }
        System.out.println();
        
        int nullValue = -2;
        //df.printAccumulatedValues(nullValue, df.timeBucketMs);

        //collect min/max values per column
        dfTraining.collectMinMaxValuesPerColumn();
        
        //http://www.faqs.org/faqs/ai-faq/neural-nets/part2/
        //normalize values in each column 
        //for linear NNs, scaling the inputs to [-1,1] will work better than [0,1]
        dfTraining.normalizeValuesPerColumn(nullValue);
        
        DatasetFlattener.writeMatrixOfValues(dfTraining.getMatrixOfValues(), "/Users/dtop/ml_matrix_training.csv");
        String trainingColumnNamesFilename = "/Users/dtop/ml_column_names_training.json";
        DatasetFlattener.writeColumnNameTranslations(dfTraining.columnNameTranslator, trainingColumnNamesFilename);
        String trainingColumnValueReplacementsFilename = "/Users/dtop/ml_column_value_replacements_training.json";
        DatasetFlattener.writeColumnValueReplacements(dfTraining.columnValueReplacements, trainingColumnValueReplacementsFilename);
        String trainingColumnMinMaxValuesFilename = "/Users/dtop/ml_column_min_max_values_training.json";
        DatasetFlattener.writeColumnMinMaxValues(dfTraining.columnMinMaxValues, trainingColumnMinMaxValuesFilename);
        
        ColumnNameTranslator trainingColumnNamesTranslator = readColumnNameTranslations(trainingColumnNamesFilename);
        ColumnValueReplacements trainingValueReplacements = readColumnValueReplacements(trainingColumnValueReplacementsFilename);
        ColumnMinMaxValues trainingColumnMinMaxValues = readColumnMinMaxValues(trainingColumnMinMaxValuesFilename);
        
        //TODO: supply into NN constant inputs for columnName.strValue -> translatedStringIntoNumber
        //TODO: supply into NN constant inputs for columnName.minValue -> minValue
        //TODO: supply into NN constant inputs for columnName.maxValue -> maxValue
        
        String trainingBitmapFileName = "/Users/dtop/ml_raster_image_training.png";
        //Multiply normalized values by 0xffffff to get array that represents RGB values for the image,
        //then build image from normalized values, save as PNG
        Integer mTraining[][] = dfTraining.normalizeIntMatrix(0, 0xffffff);
        DatasetFlattener.writePngFile(mTraining, trainingBitmapFileName);

        //df.printMatrixOfValues(df.matrixOfValues);
        //df.printMatrixOfValues(m);
        
        long endMain = System.currentTimeMillis();
        LOG.info("Completed preparation for training data in {} ms", endMain - startMain );
        
        //now prepare data for prediction
        startMain = endMain;

        DatasetFlattener dfPrediction = new DatasetFlattener(trainingColumnNamesTranslator, trainingValueReplacements, trainingColumnMinMaxValues);

        //dfPrediction.appendData("/Users/dtop/123/dev_ml_minLoadFactor/metrics.data");
        dfPrediction.appendData("/Users/dtop/123/dev_kodacloud_24hrs/metrics.data", maxRowsToProcess, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);

        //convert accumulated metrics into one matrix of values        
        dfPrediction.produceMatrixOfValues(null); 

        //column value translations from training data are applied inside appendData
        //normalize column values using min/max from training data
        dfPrediction.normalizeValuesPerColumn(nullValue);
        
        DatasetFlattener.writeMatrixOfValues(dfPrediction.getMatrixOfValues(), "/Users/dtop/ml_matrix_prediction.csv");

        String predictionBitmapFileName = "/Users/dtop/ml_raster_image_prediction.png";
        Integer mPrediction[][] = dfPrediction.normalizeIntMatrix(0, 0xffffff);
        DatasetFlattener.writePngFile(mPrediction, predictionBitmapFileName);

        endMain = System.currentTimeMillis();
        LOG.info("Completed preparation for prediction data in {} ms", endMain - startMain );
    }

    public static void main_produce_datasets_for_Q_Learning(String[] args) throws IOException {
        
        long startMain = System.currentTimeMillis();
        
        List<Pattern> pathPatternsToInclude = new ArrayList<>();
        List<Pattern> pathPatternsToExclude = new ArrayList<>();
        boolean splitMacAddressesIntoBytes = false;

        pathPatternsToInclude.add(Pattern.compile("^_type$"));
        pathPatternsToInclude.add(Pattern.compile("^equipmentId$"));
        pathPatternsToInclude.add(Pattern.compile("^eventTimestamp$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\..+STA_Client_Failure$"));

        pathPatternsToInclude.add(Pattern.compile("^createdTimestamp$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\._type$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.apPerformance.+cpuUtilized$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioStats[25]G.+numRxBeacon$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioStats[25]G.+numRxData$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioStats[25]G.+numTxData$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.minCellSize[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.noiseFloor[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.cellSize[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.channelUtilization[25]G$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.apPerformance\\.cpuTemperature$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.radioUtilization[25]G.+$"));
        
        pathPatternsToInclude.add(Pattern.compile("^data\\.ssidStats[25]g.+addressAsString$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.ssidStats[25]g.+numClient$"));

        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+addressAsString$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+txRetries$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxData$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxSucc$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxByteSucc$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numTxDataRetries$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+lastTxPhyRateKb$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+numRxBytes$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+rxBytes$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+rxDataBytes$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+rxLastRssi$"));
        pathPatternsToInclude.add(Pattern.compile("^data\\.clientMetrics[25]g.+snr$"));
        
        //include system and equipment event counts 
        pathPatternsToInclude.add(Pattern.compile("^data\\.countsPerType\\..+Event$"));
        
        //include events that report changes in SNR thresholds
        //TODO: these values need to be propagated down the column until the next non-null value is found
        pathPatternsToInclude.add(Pattern.compile("^settings\\[\\d+\\]\\.radioType$"));
        pathPatternsToInclude.add(Pattern.compile("^settings\\[\\d+\\]\\.settings\\.dropInSnrPercentage$"));

        //pathPatternsToInclude.clear();
        
        pathPatternsToExclude.add(Pattern.compile("^queueName$"));
        pathPatternsToExclude.add(Pattern.compile("^.*sessionId$"));
        pathPatternsToExclude.add(Pattern.compile("^.*[mM]essage$"));
        pathPatternsToExclude.add(Pattern.compile("^.+\\.sessionDetails\\..+$"));
        pathPatternsToExclude.add(Pattern.compile("^.+\\.historicalIssues\\..+$"));
        pathPatternsToExclude.add(Pattern.compile("^.+\\.historicalNonWifiBurstChannels\\..+$"));
        pathPatternsToExclude.add(Pattern.compile("^record\\.details\\.mitigationsEpocs\\[\\d+\\]$"));

        //
        //exclude event counts for AlarmRaisedEvent and IssueRaisedEvent - they are part of the formula to train NN, do not want them in the inputs
        //TODO: cannot really exclude them here - need to calculate target value formula from them. Will make R script skip these columns. 
        //pathPatternsToExclude.add(Pattern.compile("^data.countsPerType.AlarmRaisedEvent$"));
        //pathPatternsToExclude.add(Pattern.compile("^data.countsPerType.IssueRaisedEvent$"));

        
        DatasetFlattener dfTraining = new DatasetFlattener();
        int maxRowsToProcess = Integer.MAX_VALUE;
        //int maxRowsToProcess = 1;
        
        dfTraining.appendData("/Users/dtop/123/dev_kodacloud_24hrs/metrics.data", maxRowsToProcess, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);
        dfTraining.appendData("/Users/dtop/123/dev_kodacloud_24hrs/system_events.data", maxRowsToProcess, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);
        //dfTraining.appendData("/Users/dtop/123/dev_kodacloud_24hrs/equipment_events.data", maxRowsToProcess, pathPatternsToInclude, pathPatternsToExclude, splitMacAddressesIntoBytes);

        System.out.println("********************");
        
        
        System.out.println("Collected " + dfTraining.columnNameTranslator.getReverseColumnNamesList().size() +" columns");

        //        
        //Define a formula for the target values - it will be computed and stored in the last column of the matrix of values
        //
        TargetFormulaInterface targetFormula = new TargetFormulaInterface() {
            
            private List<Integer> columnIndexesAlarmRaisedEvent = new ArrayList<>();
            private List<Integer> columnIndexesIssueRaisedEvent = new ArrayList<>();
            private List<Integer> columnIndexesNumClients = new ArrayList<>();
            private int numberOfAPs;
            
            {
                //find out what columns need to be summed to count # of alarms raised                
                List<Pattern> alarmRaisedColumnNamePatterns = new ArrayList<>();
                alarmRaisedColumnNamePatterns.add(Pattern.compile("^.+data\\.countsPerType\\.AlarmRaisedEvent$"));
                Matcher matcher;
                for(Pattern pattern: alarmRaisedColumnNamePatterns){
                    for(int i=0; i< dfTraining.columnNameTranslator.getReverseColumnNamesList().size(); i++){
                        matcher = pattern.matcher(dfTraining.columnNameTranslator.getReverseColumnNamesList().get(i));
                        if(matcher.matches()){
                            columnIndexesAlarmRaisedEvent.add(i);
                        }
                    }
                }

                //find out what columns need to be summed to count # of issues raised                
                List<Pattern> issueRaisedColumnNamePatterns = new ArrayList<>();
                issueRaisedColumnNamePatterns.add(Pattern.compile("^.+data\\.countsPerType\\.IssueRaisedEvent$"));
                for(Pattern pattern: issueRaisedColumnNamePatterns){
                    for(int i=0; i< dfTraining.columnNameTranslator.getReverseColumnNamesList().size(); i++){
                        matcher = pattern.matcher(dfTraining.columnNameTranslator.getReverseColumnNamesList().get(i));
                        if(matcher.matches()){
                            columnIndexesIssueRaisedEvent.add(i);
                        }
                    }
                }

                //find out total number of APs
                Set<Long> apIds = new HashSet<>();
                long apId;
                int startPos;
                int endPos;
                for(String colName: dfTraining.columnNameTranslator.getReverseColumnNamesList()){
                    apId = 0;
                    try{
                        startPos = colName.indexOf('.') + 1;
                        endPos = colName.indexOf('.', startPos);
                        apId = Long.parseLong(colName.substring(startPos, endPos));
                    }catch(Exception e){
                        //cannot extract equipmentId, skip it
                        LOG.debug("Skipping entry, failed to extract equipmentId from {}", colName, e);
                        continue;
                    }
                    
                    if(apId>0){
                        apIds.add(apId);
                    }
                }
                numberOfAPs = apIds.size();
                
                
                //find out what columns need to be summed to count total # of clients connected to all APs
                List<Pattern> numClientsColumnNamePatterns = new ArrayList<>();
                numClientsColumnNamePatterns.add(Pattern.compile("^.+\\.data\\.ssidStats.+\\.numClient$"));
                for(Pattern pattern: numClientsColumnNamePatterns){
                    for(int i=0; i< dfTraining.columnNameTranslator.getReverseColumnNamesList().size(); i++){
                        matcher = pattern.matcher(dfTraining.columnNameTranslator.getReverseColumnNamesList().get(i));
                        if(matcher.matches()){
                            columnIndexesNumClients.add(i);
                        }
                    }
                }
                
            }   
            
            
            @Override
            public double produceValue(ValueElementMap valueElementMap) {
                double ret = 0;
                int countOfAlarmsCreated = 0;
                int countOfIssuesCreated = 0;
                int countOfAPs = numberOfAPs;
                int countOfDevices = 0;
                
                Object v;
                for(int colIdx:columnIndexesAlarmRaisedEvent){
                    v = valueElementMap.values.get(getColumnName(colIdx));
                    if(v instanceof Number){
                        countOfAlarmsCreated += ((Number) v).intValue();
                    }
                }
                
                for(int colIdx:columnIndexesIssueRaisedEvent){
                    v = valueElementMap.values.get(getColumnName(colIdx));
                    if(v instanceof Number){
                        countOfIssuesCreated += ((Number) v).intValue();
                    }
                }

                for(int colIdx:columnIndexesNumClients){
                    v = valueElementMap.values.get(getColumnName(colIdx));
                    if(v instanceof Number){
                        countOfDevices += ((Number) v).intValue();
                    }
                }

                double maxValue = 1000;
                if(countOfAPs==0 || countOfDevices==0){
                    return maxValue;
                }
                
                ret += (((double) countOfAlarmsCreated) / countOfAPs);
                ret += (((double) countOfIssuesCreated) / countOfDevices);
                
                if (NumberUtils.isDoubleZero(ret)) {
                    return maxValue;
                }
                
                ret = (((double)1) / ret);
                
                //
                //resulting value will be in the range (0, 1000]
                //
                return ret;
            }
        }; 
        
        //convert accumulated metrics into one matrix of values        
        dfTraining.produceMatrixOfValues(targetFormula); 
                
        int nullValue = -2;
        //df.printAccumulatedValues(nullValue, df.timeBucketMs);

        List<Pattern> fullColumnNamePatternsToPopulateNulls = new ArrayList<>();
        fullColumnNamePatternsToPopulateNulls.add(Pattern.compile("^.+\\.settings\\.dropInSnrPercentage$"));
        dfTraining.populateNullsFromOlderRows(fullColumnNamePatternsToPopulateNulls);
        
        dfTraining.collectMinMaxValuesPerColumn();        
        dfTraining.normalizeValuesPerColumn(nullValue);
        
        DatasetFlattener.writeMatrixOfValues(dfTraining.getMatrixOfValues(), "/Users/dtop/123/ml_prep/q_learning/ml_matrix_training.csv");
        String trainingColumnNamesFilename = "/Users/dtop/123/ml_prep/q_learning/ml_column_names_training.json";
        DatasetFlattener.writeColumnNameTranslations(dfTraining.columnNameTranslator, trainingColumnNamesFilename);
        String trainingColumnValueReplacementsFilename = "/Users/dtop/123/ml_prep/q_learning/ml_column_value_replacements_training.json";
        DatasetFlattener.writeColumnValueReplacements(dfTraining.columnValueReplacements, trainingColumnValueReplacementsFilename);
        String trainingColumnMinMaxValuesFilename = "/Users/dtop/123/ml_prep/q_learning/ml_column_min_max_values_training.json";
        DatasetFlattener.writeColumnMinMaxValues(dfTraining.columnMinMaxValues, trainingColumnMinMaxValuesFilename);
                
        String trainingBitmapFileName = "/Users/dtop/123/ml_prep/q_learning/ml_raster_image_training.png";
        //Multiply normalized values by 0xffffff to get array that represents RGB values for the image,
        //then build image from normalized values, save as PNG
        Integer mTraining[][] = dfTraining.normalizeIntMatrix(0, 0xffffff);
        DatasetFlattener.writePngFile(mTraining, trainingBitmapFileName);

        long endMain = System.currentTimeMillis();
        LOG.info("Completed preparation for training data in {} ms", endMain - startMain );
        
    }

    public static void main(String[] args) throws Exception {
        main_produce_datasets_for_Q_Learning(args);
    }
    
}
