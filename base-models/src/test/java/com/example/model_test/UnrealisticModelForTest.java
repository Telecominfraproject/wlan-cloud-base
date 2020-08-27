package com.example.model_test;

import java.util.Objects;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class UnrealisticModelForTest extends BaseJsonModel {
    
    private static final long serialVersionUID = 7526523061687580360L;
    
    private String name;
    private int id;
    
    public UnrealisticModelForTest() {
        //for serialization
    }

    public UnrealisticModelForTest(String name, int id) {
        this.name = name;
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UnrealisticModelForTest)) {
            return false;
        }
        UnrealisticModelForTest other = (UnrealisticModelForTest) obj;
        return id == other.id && Objects.equals(name, other.name);
    }
    
}
