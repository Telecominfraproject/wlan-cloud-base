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

    private final int externalPort;
    private final int internalPort;
    private final InetAddress externalIpAddress;
    private final InetAddress internalIpAddress;

    private final String externalHostName;
    private final String internalHostName;
    
    public ConnectorPropertiesImpl(Environment environment){
        
        int _externalPort = Integer.parseInt(environment.getProperty("server.port").trim());
        int _internalPort = Integer.parseInt(environment.getProperty("whizcontrol.secondaryPort", "7070").trim());
        InetAddress _externalIpAddress;
        InetAddress _internalIpAddress;
        String _externalHostName = environment.getProperty("whizcontrol.externalHostName");
        String _internalHostName = environment.getProperty("whizcontrol.internalHostName");
        
        String externalIpAddrStr = environment.getProperty("whizcontrol.externalIpAddress");
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

        String internalIpAddrStr = environment.getProperty("whizcontrol.internalIpAddress");
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

        
        this.externalIpAddress = _externalIpAddress;
        this.externalHostName = _externalHostName;
        this.externalPort = _externalPort;
        this.internalIpAddress = _internalIpAddress;
        this.internalHostName = _internalHostName;
        this.internalPort = _internalPort;

        LOG.info("connectorProperties {}", this);
    }
    
    public ConnectorPropertiesImpl(String externalHostName, InetAddress externalIpAddress, int externalPort, 
            String internalHostName, InetAddress internalIpAddress, int internalPort) {
        this.externalIpAddress = externalIpAddress;
        this.externalHostName = externalHostName;
        this.externalPort = externalPort;
        this.internalIpAddress = internalIpAddress;
        this.internalHostName = internalHostName;
        this.internalPort = internalPort;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConnectorProperties [externalHostName=");
        builder.append(externalHostName);
        builder.append(", externalIpAddress=");
        builder.append(externalIpAddress);
        builder.append(", externalPort=");
        builder.append(externalPort);
        builder.append(", internalHostName=");
        builder.append(internalHostName);
        builder.append(", internalIpAddress=");
        builder.append(internalIpAddress);
        builder.append(", internalPort=");
        builder.append(internalPort);
        builder.append("]");
        return builder.toString();
    }

    
}
