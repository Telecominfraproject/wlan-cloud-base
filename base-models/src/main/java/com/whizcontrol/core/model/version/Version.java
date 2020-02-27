package com.whizcontrol.core.model.version;

import com.whizcontrol.core.model.json.BaseJsonModel;

/**
 * A model for storing properties related to the release version of the cloud
 * software..
 * 
 * @author mpreston
 *
 */

public class Version extends BaseJsonModel implements Comparable<Version> {

    /**
     * 
     */
    private static final long serialVersionUID = -8407986279322601280L;

    public static final String DEFAULT_BRANCH = "branch";
    public static final int DEFAULT_MAJOR_VERSION = 1;
    public static final int DEFAULT_MINOR_VERSION = 0;
    public static final int DEFAULT_PATCH_VERSION = 0;

    private String branch;
    private int major;
    private int minor;
    private int patch;

    public Version() {
        this(DEFAULT_BRANCH, DEFAULT_MAJOR_VERSION, DEFAULT_MINOR_VERSION, DEFAULT_PATCH_VERSION);
    }

    public Version(String branch, int majorVersion, int minorVersion, int patchVersion) {
        setBranch(branch);
        setMajor(majorVersion);
        setMinor(minorVersion);
        setPatch(patchVersion);
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getPatch() {
        return patch;
    }

    public void setPatch(int patch) {
        this.patch = patch;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.branch == null) ? 0 : this.branch.hashCode());
        result = prime * result + this.major;
        result = prime * result + this.minor;
        result = prime * result + this.patch;
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
        if (!(obj instanceof Version)) {
            return false;
        }
        Version other = (Version) obj;
        if (this.branch == null) {
            if (other.branch != null) {
                return false;
            }
        } else if (!this.branch.equals(other.branch)) {
            return false;
        }
        if (this.major != other.major) {
            return false;
        }
        if (this.minor != other.minor) {
            return false;
        }
        if (this.patch != other.patch) {
            return false;
        }
        return true;
    }

    @Override
    public Version clone() {
        return (Version) super.clone();
    }

    @Override
    public int compareTo(Version o) {
        int result = Integer.compare(this.major, o.major);
        if (result != 0) {
            return result;
        }

        result = Integer.compare(this.minor, o.minor);
        if (result != 0) {
            return result;
        }

        return Integer.compare(this.patch, o.patch);
    }
}
