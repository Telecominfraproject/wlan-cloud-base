package com.telecominfraproject.wlan.core.server.container;

import java.net.InetAddress;

/**
 * @author dtop
 * Interface that exposes properties of connectors inside spring container to other components
 */
public interface ConnectorProperties {

    /**
     * @return port on which this server listens for internal API requests
     */
    int getInternalPort();
    
    /**
     * @return host on which this server listens for internal API requests
     */
    String getInternalHostName();
    
    /**
     * @return ip address on which this server listens for internal API requests
     */
    InetAddress getInternalIpAddress();

    
    /**
     * @return port on which this server listens for API requests from the outside world
     */
    int getExternalPort();
    
    /**
     * @return host on which this server listens for API requests from the outside world
     */
    String getExternalHostName();
    
    /**
     * @return ip address on which this server listens for API requests from the outside world
     */
    InetAddress getExternalIpAddress();

    
    /**
     * @return port which this server advertises to clients so that they can send API requests from the outside world, could be a load-balancer port, or a kubernetes-remapped port
     */
    int getExternallyVisiblePort();
    
    /**
     * @return host which this server advertises to clients so that they can send API requests from the outside world, could be a load-balancer host, or a kubernetes-remapped host
     */
    String getExternallyVisibleHostName();
    
    /**
     * @return  ip address which this server advertises to clients so that they can send API requests from the outside world, could be a load-balancer ip address, or a kubernetes-remapped ip address
     */
    InetAddress getExternallyVisibleIpAddress();

}
