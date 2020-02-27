package com.whizcontrol.core.model.json.flattener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * @author dtop
 * This class maintains mappings between long column names and their short versions (c_xxxxxx)
 */
public class ColumnNameTranslator extends BaseJsonModel {

    private static final long serialVersionUID = -5285763238431015606L;
    
    private Map<String, String> columnNamesMap = new HashMap<>();
    private List<String> reverseColumnNamesList = new ArrayList<>();
    
    public Map<String, String> getColumnNamesMap() {
        return columnNamesMap;
    }
    public void setColumnNamesMap(Map<String, String> columnNamesMap) {
        this.columnNamesMap = columnNamesMap;
    }
    public List<String> getReverseColumnNamesList() {
        return reverseColumnNamesList;
    }
    public void setReverseColumnNamesList(List<String> reverseColumnNamesList) {
        this.reverseColumnNamesList = reverseColumnNamesList;
    }

}
