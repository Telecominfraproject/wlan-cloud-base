package com.telecominfraproject.wlan.hierarchical.datastore;

import java.util.List;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.filter.EntryFilter;
import com.telecominfraproject.wlan.hierarchical.datastore.index.DirectoryIndex;

/**
 * This interface is used byHDS internally to apply a piece of logic to every data file traversed by method HierarchicalDatastore.processDataFiles().
 * @see com.telecominfraproject.wlan.hierarchical.datastore.HierarchicalDatastore.processDataFiles(int, long, long, long, EntryFilter<T>, String, Set<String>, DataFileOperation<T>)
 * @author dtop
 *
 * @param <T> - Class of the entry records stored in the data files
 */
public interface DataFileOperation<T>{
    /**
     * @param entryFilter
     * @param indexName
     * @param indexedValues
     * @param dataFileNames - list of data file names to be processed
     * @param hourlyIdx - this parameter can be NULL !!! - means no hourly index is available
     */
    void processFiles(EntryFilter<T> entryFilter, String indexName, Set<String> indexedValues, List<String> dataFileNames, DirectoryIndex hourlyIdx);
}