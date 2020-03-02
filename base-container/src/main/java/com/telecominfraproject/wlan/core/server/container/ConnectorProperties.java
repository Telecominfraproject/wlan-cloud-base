package com.telecominfraproject.wlan.core.server.container;

import java.net.InetAddress;

/**
 * @author dtop
 * Interface that exposes properties of connectors inside spring container to other components
 */
public interface ConnectorProperties {

    int getExternalPort();
    int getInternalPort();
    InetAddress getExternalIpAddress();
    InetAddress getInternalIpAddress();
    String getExternalHostName();
    String getInternalHostName();
}
