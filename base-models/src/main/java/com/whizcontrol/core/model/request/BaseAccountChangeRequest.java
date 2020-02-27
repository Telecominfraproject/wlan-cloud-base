/**
 * 
 */
package com.whizcontrol.core.model.request;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.whizcontrol.core.model.json.BaseJsonModel;
import com.whizcontrol.core.model.json.JsonDeserializationUtils;
import com.whizcontrol.core.model.role.PortalUserRole;

/**
 * Data model to extend Account Change requests.
 * 
 * @author mpreston
 *
 */
public class BaseAccountChangeRequest extends BaseJsonModel {

    /**
     * Request Status
     * 
     * @author mpreston
     *
     */
    public static enum RequestStatus {
        APPROVED(3), CANCELLED(2), PENDING(1), REJECTED(4),

        UNSUPPORTED(-1);

        private static final Map<Integer, RequestStatus> ID_MAP = new HashMap<>();

        public static RequestStatus getById(int id) {
            if (ID_MAP.isEmpty()) {
                synchronized (ID_MAP) {
                    if (ID_MAP.isEmpty()) {
                        for (RequestStatus v : RequestStatus.values()) {
                            if (RequestStatus.isUnsupported(v)) {
                                continue;
                            }
                            ID_MAP.put(v.getId(), v);
                        }
                    }
                }
            }
            RequestStatus result = ID_MAP.get(id);
            return (null != result) ? result : UNSUPPORTED;
        }

        @JsonCreator
        public static RequestStatus getByName(String value) {
            return JsonDeserializationUtils.deserializEnum(value, RequestStatus.class, UNSUPPORTED);
        }

        public static boolean isUnsupported(RequestStatus value) {
            return UNSUPPORTED.equals(value);
        }

        private final int id;

        RequestStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    /**
     * Request type.
     * 
     * @author mpreston
     *
     */
    public static enum RequestType {
        CREATE_CUSTOMER(1), EMAIL_CHANGE(2),

        UNSUPPORTED(-1);

        private static final Map<Integer, RequestType> ID_MAP = new HashMap<>();

        public static RequestType getById(int id) {
            if (ID_MAP.isEmpty()) {
                synchronized (ID_MAP) {
                    if (ID_MAP.isEmpty()) {
                        for (RequestType v : RequestType.values()) {
                            if (RequestType.isUnsupported(v)) {
                                continue;
                            }
                            ID_MAP.put(v.getId(), v);
                        }
                    }
                }
            }
            RequestType result = ID_MAP.get(id);
            return (null != result) ? result : UNSUPPORTED;
        }

        @JsonCreator
        public static RequestType getByName(String value) {
            return JsonDeserializationUtils.deserializEnum(value, RequestType.class, UNSUPPORTED);
        }

        public static boolean isUnsupported(RequestType value) {
            return UNSUPPORTED.equals(value);
        }

        private final int id;

        RequestType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 4228748882337232164L;

    public static final long INVALID_ID = -1L;

    /**
     * Request id
     */
    protected long id = INVALID_ID;

    protected RequestStatus requestStatus = RequestStatus.PENDING;
    protected RequestType requestType = RequestType.CREATE_CUSTOMER;

    /**
     * Reason for last update, should only be set for REJECT and CANCELLED
     */
    protected String updateReason;

    protected long createdTimestamp;
    protected long lastModifiedTimestamp;
    private String requestingUserEmail;
    private PortalUserRole requestingUserRole;

    protected BaseAccountChangeRequest() {
    }

    @Override
    public BaseAccountChangeRequest clone() {
        BaseAccountChangeRequest result = (BaseAccountChangeRequest) super.clone();
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
        if (!(obj instanceof BaseAccountChangeRequest)) {
            return false;
        }
        BaseAccountChangeRequest other = (BaseAccountChangeRequest) obj;
        if (createdTimestamp != other.createdTimestamp) {
            return false;
        }
        if (id != other.id) {
            return false;
        }
        if (lastModifiedTimestamp != other.lastModifiedTimestamp) {
            return false;
        }
        if (requestStatus != other.requestStatus) {
            return false;
        }
        if (requestType != other.requestType) {
            return false;
        }
        if (requestingUserEmail == null) {
            if (other.requestingUserEmail != null) {
                return false;
            }
        } else if (!requestingUserEmail.equals(other.requestingUserEmail)) {
            return false;
        }
        if (requestingUserRole != other.requestingUserRole) {
            return false;
        }
        if (updateReason == null) {
            if (other.updateReason != null) {
                return false;
            }
        } else if (!updateReason.equals(other.updateReason)) {
            return false;
        }
        return true;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public long getId() {
        return id;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    /**
     * @return the requestingUserEmail
     */
    public String getRequestingUserEmail() {
        return requestingUserEmail;
    }

    /**
     * @return the requestingUserRole
     */
    public PortalUserRole getRequestingUserRole() {
        return requestingUserRole;
    }

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getUpdateReason() {
        return updateReason;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (createdTimestamp ^ (createdTimestamp >>> 32));
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + (int) (lastModifiedTimestamp ^ (lastModifiedTimestamp >>> 32));
        result = prime * result + ((requestStatus == null) ? 0 : requestStatus.hashCode());
        result = prime * result + ((requestType == null) ? 0 : requestType.hashCode());
        result = prime * result + ((requestingUserEmail == null) ? 0 : requestingUserEmail.hashCode());
        result = prime * result + ((requestingUserRole == null) ? 0 : requestingUserRole.hashCode());
        result = prime * result + ((updateReason == null) ? 0 : updateReason.hashCode());
        return result;
    }

    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }
        if (RequestStatus.isUnsupported(this.requestStatus)) {
            return true;
        }
        if (RequestType.isUnsupported(this.requestType)) {
            return true;
        }
        if (PortalUserRole.isUnsupported(this.requestingUserRole)) {
            return true;
        }
        return false;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    /**
     * @param requestingUserEmail
     *            the requestingUserEmail to set
     */
    public void setRequestingUserEmail(String requestingUserEmail) {
        this.requestingUserEmail = requestingUserEmail;
    }

    /**
     * @param requestingUserRole
     *            the requestingUserRole to set
     */
    public void setRequestingUserRole(PortalUserRole requestingUserRole) {
        this.requestingUserRole = requestingUserRole;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public void setUpdateReason(String updateReason) {
        this.updateReason = updateReason;
    }

}
