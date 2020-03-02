package com.telecominfraproject.wlan.core.server.security;

public interface AuthProviderInfo {
    /**
     * Return the access type based on the security provider
     * 
     * @return
     */
    AccessType getAccessType();
}
