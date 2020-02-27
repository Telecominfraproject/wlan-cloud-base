package com.whizcontrol.core.model.json.flattener;

import java.util.HashMap;
import java.util.Map;

import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * @author dtop 
 * This class maintains mappings for string value replacements -
 * outer key is a full column name, inner key is a string value in that
 * column, translation value is an integer.
 */
public class ColumnValueReplacements extends BaseJsonModel {

    private static final long serialVersionUID = 6510799460571019223L;

    private Map<String, Map<String, Integer>> columnValueTranslationMap = new HashMap<>();

    public Map<String, Map<String, Integer>> getColumnValueTranslationMap() {
        return columnValueTranslationMap;
    }

    public void setColumnValueTranslationMap(Map<String, Map<String, Integer>> columnValueTranslationMap) {
        this.columnValueTranslationMap = columnValueTranslationMap;
    }
    
    public static void main(String[] args) {
        ColumnValueReplacements cvr = new ColumnValueReplacements();
        cvr.getColumnValueTranslationMap().put("c_1", new HashMap<>());
        cvr.getColumnValueTranslationMap().put("c_2", new HashMap<>());
        
        cvr.getColumnValueTranslationMap().get("c_1").put("v_1_1",11);
        cvr.getColumnValueTranslationMap().get("c_1").put("v_1_2",12);
        
        cvr.getColumnValueTranslationMap().get("c_2").put("v_2_1",21);
        cvr.getColumnValueTranslationMap().get("c_2").put("v_2_2",22);
        
        System.out.println("Original: " + cvr);
        
        ColumnValueReplacements cvrDeserialized = ColumnValueReplacements.fromString(cvr.toString(), ColumnValueReplacements.class);
        
        System.out.println("Deserialized: " + cvrDeserialized);
    }
}
