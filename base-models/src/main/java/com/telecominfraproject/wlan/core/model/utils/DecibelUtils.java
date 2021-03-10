package com.telecominfraproject.wlan.core.model.utils;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;


public class DecibelUtils 
{
    public static double getAverageDecibel(int[] decibels)
    {
        double aggregation = 0;
        int count=0;
        for(Integer value: decibels)
        {
            aggregation += Math.pow(10, value/10.0);
            count++;
        } 

        double avgDbVal10 = 10 * Math.log10(aggregation/count);
        return avgDbVal10;
    }

    public static double getAverageDecibel(Collection<Integer> decibels) 
    {
    	if (decibels == null || decibels.isEmpty()) { 
    		return 0.0; 
    	}
    	
    	double aggregation = 0; 
    	int count=0; 
    	Iterator<Integer> iter = decibels.iterator(); 
    	
    	while(iter.hasNext()) { 
    		Integer value = iter.next(); 
    		if(value!=null) { 
    			aggregation += Math.pow(10, value/10.0); 
    			count++; 
    		} 
    	} 
    	
    	if (count == 0) {
    		return 0.0; 
    	}else {
    		return 10 * Math.log10(aggregation/count);
    	}
    }
    
    public static double getDecibelStandardDeviation(int[] decibels)
    {
        double aggregation = 0;
        int count=0;
        for(Integer value: decibels)
        {
            aggregation += Math.pow(10, value/10.0);
            count++;
        } 

        
        double standardDeviationCummulative = 0.0;
        
        for(Integer value : decibels)
        {
            standardDeviationCummulative += Math.pow(Math.pow(10, value/10.0) - (aggregation / count), 2);
        }
        
        return 10 * Math.log10(Math.sqrt(standardDeviationCummulative / count));
    }
    
    
    public static double getDecibelPercentile(int[] decibels, int percentile)
    {
        if(decibels != null)
        {
            double transforedDecibels[] = new double[decibels.length];
            int index = 0;

            for(Integer decibel : decibels)
            {
                transforedDecibels[index++] = Math.pow(10, decibel/10.0);
            }
            
            Percentile percentileUtil = new Percentile(percentile);
            double transformedPercentileDecibel = percentileUtil.evaluate(transforedDecibels);
            
            return 10 * Math.log10(transformedPercentileDecibel);
        }
        
        return -1.0;
    }

//    public static double getPercentile(int[] values, double percentile)
//    {
//        if(values != null)
//        {
//            Percentile percentileUtil = new Percentile(percentile);
//            return percentileUtil.evaluate(toDoubleArray(values));
//        }
//        
//        return -1.0;
//    }
//
    
    
    public static double[] toDoubleArray(int[] values)
    {
        double[] returnValue = new double[values.length];
        int index = 0;
        
        for(Integer value : values)
        {
            returnValue[index++] = (double) value;
        }
        
        return returnValue;
    }
    
}
