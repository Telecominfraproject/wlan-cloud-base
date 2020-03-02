package com.telecominfraproject.wlan.core.model.pagination;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

/**
 * @author dtop
 *
 */
public class ColumnAndSort extends BaseJsonModel {
    private static final long serialVersionUID = 4880440052560255250L;
    
    private String columnName;
    private SortOrder sortOrder = SortOrder.asc;
    
    public ColumnAndSort(String columnName) {
        this.columnName = columnName;
    }

    public ColumnAndSort(String columnName, SortOrder sortOrder) {
        this.columnName = columnName;
        this.sortOrder = sortOrder;
    }

    public ColumnAndSort() {
        // for serialization
    }
    
    public String getColumnName() {
        return columnName;
    }
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    public SortOrder getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ColumnAndSort)) {
            return false;
        }
        ColumnAndSort other = (ColumnAndSort) obj;
        if (columnName == null) {
            if (other.columnName != null) {
                return false;
            }
        } else if (!columnName.equals(other.columnName)) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }
        
        if (SortOrder.isUnsupported(sortOrder)) {
            return true;
        }
        
        return false;
    }
}
