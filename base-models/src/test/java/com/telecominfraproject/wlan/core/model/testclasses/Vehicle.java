package com.telecominfraproject.wlan.core.model.testclasses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

@JsonSerialize()
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vehicle extends BaseJsonModel {
    /**
     * 
     */
    private static final long serialVersionUID = -6032969621688949872L;
    private String colour;
    private String name;
    private int numKm;
    private List<String> peopleOnTheLease;

    public Vehicle() {
        // nothing
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumKm() {
        return numKm;
    }

    public void setNumKm(int numKm) {
        this.numKm = numKm;
    }

    public List<String> getPeopleOnTheLease() {
        return peopleOnTheLease;
    }

    public void setPeopleOnTheLease(List<String> peopleOnTheLease) {
        this.peopleOnTheLease = peopleOnTheLease;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((colour == null) ? 0 : colour.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + numKm;
        result = prime * result + ((peopleOnTheLease == null) ? 0 : peopleOnTheLease.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vehicle other = (Vehicle) obj;
        if (colour == null) {
            if (other.colour != null)
                return false;
        } else if (!colour.equals(other.colour))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (numKm != other.numKm)
            return false;
        if (peopleOnTheLease == null) {
            if (other.peopleOnTheLease != null)
                return false;
        } else if (!peopleOnTheLease.equals(other.peopleOnTheLease))
            return false;
        return true;
    }

}
