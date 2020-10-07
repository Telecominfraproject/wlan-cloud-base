package com.telecominfraproject.wlan.core.server.container;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

/**
 * @author dtop
 *
 */
public class ConnectorPropertiesImpl implements ConnectorProperties {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorProperties.class);

    //host and port on which this server listens for internal API requests
    private final int internalPort;
    private final String internalHostName;
    private final InetAddress internalIpAddress;

    //host and port on which this server listens for API requests from the outside world
    private final int externalPort;
    private final String externalHostName;
    private final InetAddress externalIpAddress;
    
    //host and port which this server advertises to clients so that they can send API requests from the outside world, could be a load-balancer host and port, or a kubernetes-remapped host/port
    private final int externallyVisiblePort;
    private final String externallyVisibleHostName;
    private final InetAddress externallyVisibleIpAddress;

    public ConnectorPropertiesImpl(Environment environment){
        
        int _externalPort = Integer.parseInt(environment.getProperty("server.port").trim());
        int _internalPort = Integer.parseInt(environment.getProperty("tip.wlan.secondaryPort", "7070").trim());
        InetAddress _externalIpAddress;
        InetAddress _internalIpAddress;
        String _externalHostName = environment.getProperty("tip.wlan.externalHostName");
        String _internalHostName = environment.getProperty("tip.wlan.internalHostName");
        
        String externalIpAddrStr = environment.getProperty("tip.wlan.externalIpAddress");
        if(externalIpAddrStr==null){
            try {
                _externalIpAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new ConfigurationException("Cannot get address of the system", e);
            }
        } else {
            try {
                _externalIpAddress = InetAddress.getByName(externalIpAddrStr.trim());
            } catch (UnknownHostException e) {
                throw new ConfigurationException("Cannot get address of the system", e);
            }
        }
        
        if(_externalHostName==null){
            _externalHostName = _externalIpAddress.getCanonicalHostName();
        }

        String internalIpAddrStr = environment.getProperty("tip.wlan.internalIpAddress");
        if(internalIpAddrStr==null){
            try {
                _internalIpAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new ConfigurationException("Cannot get address of the system", e);
            }
        } else {
            try {
                _internalIpAddress = InetAddress.getByName(internalIpAddrStr.trim());
            } catch (UnknownHostException e) {
                throw new ConfigurationException("Cannot get address of the system", e);
            }
        }

        if(_internalHostName==null){
            _internalHostName = _internalIpAddress.getCanonicalHostName();
        }


        //Populate externally-visible properties, if any
        int _externallyVisiblePort = Integer.parseInt(environment.getProperty("tip.wlan.externallyVisiblePort", "0").trim());
        if(_externallyVisiblePort == 0) {
            _externallyVisiblePort = _externalPort;
        }
        
        String _externallyVisibleHostName = environment.getProperty("tip.wlan.externallyVisibleHostName");
        if(_externallyVisibleHostName == null || _externallyVisibleHostName.trim().isEmpty()) {
            _externallyVisibleHostName = _externalHostName;
        }
        
        InetAddress _externallyVisibleIpAddress;
        String externallyVisibleIpAddrStr = environment.getProperty("tip.wlan.externallyVisibleIpAddress");
        if(externallyVisibleIpAddrStr == null) {
            _externallyVisibleIpAddress = _externalIpAddress;
        } else {
            try {
                _externallyVisibleIpAddress = InetAddress.getByName(externallyVisibleIpAddrStr.trim());
            } catch (UnknownHostException e) {
                throw new ConfigurationException("Cannot get externally visible address of the system", e);
            }
        }
        
        
        this.externalIpAddress = _externalIpAddress;
        this.externalHostName = _externalHostName;
        this.externalPort = _externalPort;

        this.internalIpAddress = _internalIpAddress;
        this.internalHostName = _internalHostName;
        this.internalPort = _internalPort;

        this.externallyVisibleIpAddress = _externallyVisibleIpAddress;
        this.externallyVisibleHostName = _externallyVisibleHostName;
        this.externallyVisiblePort = _externallyVisiblePort;

        LOG.info("connectorProperties {}", this);
    }
    

    public int getExternalPort() {
        return externalPort;
    }

    public int getInternalPort() {
        return internalPort;
    }

    public InetAddress getExternalIpAddress() {
        return externalIpAddress;
    }

    public InetAddress getInternalIpAddress() {
        return internalIpAddress;
    }

    public String getExternalHostName() {
        return externalHostName;
    }

    public String getInternalHostName() {
        return internalHostName;
    }

    public int getExternallyVisiblePort() {
        return externallyVisiblePort;
    }

    public String getExternallyVisibleHostName() {
        return externallyVisibleHostName;
    }

    public InetAddress getExternallyVisibleIpAddress() {
        return externallyVisibleIpAddress;
    }

    @Override
    public String toString() {
        return String.format(
                "ConnectorPropertiesImpl [internalPort=%s, internalHostName=%s, internalIpAddress=%s, externalPort=%s, externalHostName=%s, externalIpAddress=%s, externallyVisiblePort=%s, externallyVisibleHostName=%s, externallyVisibleIpAddress=%s]",
                internalPort, internalHostName, internalIpAddress, externalPort, externalHostName, externalIpAddress,
                externallyVisiblePort, externallyVisibleHostName, externallyVisibleIpAddress);
    }
    
}
